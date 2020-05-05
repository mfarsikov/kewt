package com.github.mfarsikov.kewt.processor

import com.github.mfarsikov.kewt.annotations.Mapper
import com.github.mfarsikov.kewt.processor.mapper.AnnotationConfig
import com.github.mfarsikov.kewt.processor.mapper.ReadyForMappingFunction
import com.github.mfarsikov.kewt.processor.mapper.calculateMappings
import com.github.mfarsikov.kewt.processor.parser.parse
import com.github.mfarsikov.kewt.processor.render.RenderConverterClass
import com.github.mfarsikov.kewt.processor.render.RenderConverterFunction
import com.github.mfarsikov.kewt.processor.render.RenderPropertyMappings
import com.github.mfarsikov.kewt.processor.render.render
import com.github.mfarsikov.kewt.processor.resolver.PropertiesResolverImpl
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.StandardLocation
import kotlin.properties.Delegates

class KewtMapperProcessor : AbstractProcessor() {
    override fun getSupportedSourceVersion() = SourceVersion.latestSupported()
    override fun getSupportedOptions() = setOf("kewt.log.level", "kewt.generate.spring", "kewt.whitelist", "kewt.blacklist")
    override fun getSupportedAnnotationTypes() = setOf(Mapper::class.qualifiedName)

    var springComponent: Boolean by Delegates.notNull()
    var whitelist: List<String> = emptyList()
    var blacklist: List<String> = emptyList()

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        processingEnv.options["kewt.log.level"]
                ?.let { Logger.LogLevel.valueOf(it.toUpperCase()) }
                ?.also { Logger.logLevel = it }
        Logger.messager = processingEnv.messager

        springComponent = processingEnv.options["kewt.generate.spring"]?.toBoolean() ?: false
        whitelist = processingEnv.options["kewt.whitelist"]?.split(" ") ?: emptyList()
        blacklist = processingEnv.options["kewt.blacklist"]?.split(" ") ?: emptyList()

    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (roundEnv.processingOver()) return false
        val now = LocalDateTime.now()

        val version = this::class.java.classLoader
                .getResource("META-INF/build-info.properties")
                ?.readText() ?: "unknown version"

        Logger.info("Start processing @Mapper annotations")

        try {
            val propertiesResolver = PropertiesResolverImpl(roundEnv, processingEnv)


            roundEnv.getElementsAnnotatedWith(Mapper::class.java).forEach { element ->
                try {
                    val parsedInterface = parse(element, processingEnv)


                    if (
                            whitelist.isNotEmpty() && whitelist.none { parsedInterface.type.qualifiedName().startsWith(it) } ||
                            blacklist.isNotEmpty() && blacklist.any { parsedInterface.type.qualifiedName().startsWith(it) }
                    ) {
                        Logger.info("Skip ${parsedInterface.type.qualifiedName()}")
                        return@forEach
                    }

                    Logger.debug("Parsed class: ${parsedInterface.type}")

                    val conversionFunctions = parsedInterface.functions.filter { it.parameters.size == 1 }
                            .map {
                                ConversionFunction(
                                        name = it.name,
                                        parameter = it.parameters.single(),
                                        returnType = it.returnType
                                )
                            }

                    Logger.debug("Conversion functions: $conversionFunctions")
                    parsedInterface.functions
                            .filter { !it.abstract && it.parameters.size > 1 }
                            .map { it.name }
                            .takeIf { it.isNotEmpty() }
                            ?.let { Logger.warn("Not abstract function with more than one parameter cannot be used: $it, class: ${parsedInterface.type}") }

                    val mappingsResults = parsedInterface.functions
                            .filter { it.abstract }
                            .map { parsedFunction ->

                                Logger.debug("Abstract function: $parsedFunction")
                                val resolvedFunction = normalizeNamesAndResolveTypes(parsedFunction, propertiesResolver)
                                Logger.debug("Explicit mappings: ${resolvedFunction.nameMappings}")

                                calculateMappings(function = resolvedFunction, conversionFunctions = conversionFunctions)
                            }

                    val text = render(
                            converter = RenderConverterClass(
                                    type = parsedInterface.type,
                                    converterFunctions = mappingsResults.map {
                                        RenderConverterFunction(
                                                name = it.name,
                                                returnTypeLanguage = it.returnType.language,
                                                parameters = it.parameters.map {
                                                    Parameter(
                                                            name = it.name,
                                                            type = it.resolvedType.type
                                                    )
                                                },
                                                returnType = it.returnType.type,
                                                mappings = it.mappings.map {
                                                    RenderPropertyMappings(
                                                            parameterName = it.parameterName,
                                                            sourcePropertyName = it.sourceProperty.name,
                                                            targetPropertyName = it.targetProperty.name,
                                                            conversionContext = it.conversionContext!!
                                                    )
                                                }
                                        )
                                    },
                                    springComponent = springComponent
                            ),
                            version = version,
                            date = now.atOffset(ZoneOffset.UTC)
                    )

                    val file = processingEnv.filer.createResource(StandardLocation.SOURCE_OUTPUT, parsedInterface.type.packageName, "${parsedInterface.type.name}Impl.kt", element)

                    file.openWriter().use { it.write(text) }
                } catch (ex: KewtException) {
                    Logger.error("Cannot process ${element.simpleName}: ${ex.message}")
                }
            }
            Logger.info("Finished processing @Mapper annotations")
        } catch (ex: Throwable) {
            Logger.error(ex, "Kewt annotiation processor finished exceptionally")
            throw ex
        }

        return false
    }

    private fun normalizeNamesAndResolveTypes(parsedFunction: Function, propertiesResolver: PropertiesResolverImpl): ReadyForMappingFunction {
        val nameMappings = normalizeNames(parsedFunction.parameters, parsedFunction.annotationConfigs)

        val (resolvedReturnType, resolvedParameters) = propertiesResolver.resolveTypes(parsedFunction.returnType, parsedFunction.parameters, nameMappings)

        return ReadyForMappingFunction(
                name = parsedFunction.name,
                parameters = resolvedParameters,
                returnType = resolvedReturnType,
                nameMappings = nameMappings,
                explicitConverters = parsedFunction.annotationConfigs
                        .filter { it.converter != null }
                        .map { ExplicitConverter(targetName = it.target, converterName = it.converter!!) }
        )
    }

    private fun normalizeNames(
            sources: List<Parameter>,
            annotationConfigs: List<AnnotationConfig>
    ): List<NameMapping> =
            if (sources.size == 1 && annotationConfigs.none { it.source.startsWith("${sources.single().name}.") }) {
                annotationConfigs.map { it.copy(source = "${sources.single().name}.${it.source}") }
            } else {
                annotationConfigs
            }.map {
                NameMapping(
                        parameterName = it.source.split(".").first(),
                        sourcePath = it.source.substringAfter("."),
                        targetParameterName = it.target
                )
            }


}

data class ExplicitConverter(
        val targetName: String,
        val converterName: String
)

data class NameMapping(
        val parameterName: String,
        val sourcePath: String,
        val targetParameterName: String
) {
    override fun toString() = "$targetParameterName <= $parameterName.$sourcePath"
}

data class ConversionFunction(
        val name: String,
        val parameter: Parameter,
        val returnType: Type
) {
    override fun toString() = "$name($parameter): $returnType"
}
