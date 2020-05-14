package com.github.mfarsikov.kewt.processor

import com.github.mfarsikov.kewt.annotations.Mapper
import com.github.mfarsikov.kewt.processor.mapper.AnnotationConfig
import com.github.mfarsikov.kewt.processor.mapper.PropertyMapping
import com.github.mfarsikov.kewt.processor.mapper.ResolvedParameter
import com.github.mfarsikov.kewt.processor.mapper.ResolvedType
import com.github.mfarsikov.kewt.processor.mapper.Source
import com.github.mfarsikov.kewt.processor.mapper.calculateMappings
import com.github.mfarsikov.kewt.processor.parser.parse
import com.github.mfarsikov.kewt.processor.render.RenderConverterClass
import com.github.mfarsikov.kewt.processor.render.RenderConverterFunction
import com.github.mfarsikov.kewt.processor.render.RenderPropertyMappings
import com.github.mfarsikov.kewt.processor.render.render
import com.github.mfarsikov.kewt.processor.render.renderExt
import com.github.mfarsikov.kewt.processor.resolver.PropertiesResolver
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
            val propertiesResolver = PropertiesResolver(roundEnv, processingEnv)


            roundEnv.getElementsAnnotatedWith(Mapper::class.java).forEach { element ->

                val pkg = processingEnv.elementUtils.getPackageOf(element).qualifiedName.toString()
                if (
                        whitelist.isNotEmpty() && whitelist.none { pkg.startsWith(it) } || //TODO class level?
                        blacklist.isNotEmpty() && blacklist.any { pkg.startsWith(it) }
                ) {
                    Logger.info("Skip ${pkg}")
                    return@forEach
                }

                try {
                    val parsedInterface = parse(element, processingEnv)




                    Logger.debug("Parsed class: ${parsedInterface.type}")

                    val conversionFunctions = parsedInterface.functions.filter { it.parameters.size == 1 }
                            .map {
                                ConversionFunction(
                                        name = it.name,
                                        parameter = it.parameters.single().toParameter(),
                                        returnType = it.returnType,
                                        isExtension = it.isExtension
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
                                val nameMappings = normalizeNames(parsedFunction.parameters.map { it.toParameter() }, parsedFunction.annotationConfigs)


                                val targetParameter = parsedFunction.parameters.singleOrNull { it.isTarget }
                                val invalidTargetParameterType = targetParameter?.type?.toSimpleType()
                                        ?.takeIf { it != parsedFunction.returnType.toSimpleType() }


                                if (invalidTargetParameterType != null) throw KewtException("target paratmeter type does not match return type. target parameter: $invalidTargetParameterType, return type:${parsedFunction.returnType.toSimpleType()}")

                                val resolvedSources = parsedFunction.parameters
                                        .filter { !it.isTarget }
                                        .map { parameter ->

                                            val ms = nameMappings.filter { it.parameterName == parameter.name }

                                            val nestedSources = propertiesResolver.nestedParameterProperties(ms, parameter.toParameter())

                                            val resolveType = propertiesResolver.resolveType(parameter.type)

                                            val rt = resolveType
                                                    .mapParameter { p -> Source(parameterName = parameter.name, path = listOf(p.name), type = p.type) }
                                                    .let {
                                                        it.copy(properties = it.properties + nestedSources)
                                                    }

                                            ResolvedParameter(parameter.name, rt)
                                        }

                                val resolvedReturnType = propertiesResolver.resolveType(parsedFunction.returnType)

                                val returnPropertiesWithDefaultValues = resolvedReturnType.properties.filter { it.hasDefaultValue }.map { it.name }.toSet()


                                val sources = resolvedSources.flatMap { it.resolvedType.properties } +
                                        parsedFunction.parameters.map { Source(it.name, path = emptyList(), type = it.type) }


                                val mappings = calculateMappings(
                                        sources = sources,
                                        targets = resolvedReturnType.properties.map { Parameter(it.name, it.type) }.toSet(),
                                        nameMappings = nameMappings,
                                        explicitConverters = parsedFunction.annotationConfigs
                                                .filter { it.converter != null }
                                                .map { ExplicitConverter(targetName = it.target, converterName = it.converter!!) },
                                        conversionFunctions = conversionFunctions,
                                        returnPropertiesWithDefaultValues = returnPropertiesWithDefaultValues
                                )

                                MappedFunction(
                                        name = parsedFunction.name,
                                        parameters = parsedFunction.parameters.map { it.toParameter() },
                                        returnType = resolvedReturnType.mapParameter { Parameter(it.name, it.type) },
                                        mappings = mappings,
                                        targetParameterName = targetParameter?.name

                                )
                            }

                    val text =
                            if (parsedInterface.isInterface) {
                                render(
                                        converter = RenderConverterClass(
                                                type = parsedInterface.type,
                                                converterFunctions = mappingsResults.map {
                                                    RenderConverterFunction(
                                                            name = it.name,
                                                            returnTypeLanguage = it.returnType.language,
                                                            parameters = it.parameters,
                                                            returnType = it.returnType.type,
                                                            mappings = it.mappings.map {
                                                                RenderPropertyMappings(
                                                                        parameterName = it.source.parameterName,
                                                                        sourcePropertyName = it.source.path.joinToString("."),//TODO should parser see path as array? for null-safe calls?
                                                                        targetPropertyName = it.target.name,
                                                                        conversionContext = it.conversionContext!!
                                                                )
                                                            },
                                                            targetParameterName = it.targetParameterName
                                                    )
                                                },
                                                springComponent = springComponent
                                        ),
                                        version = version,
                                        date = now.atOffset(ZoneOffset.UTC)
                                )
                            } else {
                                renderExt(
                                        converter = RenderConverterClass(
                                                type = parsedInterface.type,
                                                converterFunctions = mappingsResults.map {
                                                    RenderConverterFunction(
                                                            name = it.name,
                                                            returnTypeLanguage = it.returnType.language,
                                                            parameters = it.parameters,
                                                            returnType = it.returnType.type,
                                                            mappings = it.mappings.map {
                                                                RenderPropertyMappings(
                                                                        parameterName = it.source.parameterName,
                                                                        sourcePropertyName = it.source.path.joinToString("."),//TODO should parser see path as array? for null-safe calls?
                                                                        targetPropertyName = it.target.name,
                                                                        conversionContext = it.conversionContext!!
                                                                )
                                                            },
                                                            targetParameterName = it.targetParameterName
                                                    )
                                                },
                                                springComponent = springComponent
                                        ),
                                        version = version,
                                        date = now.atOffset(ZoneOffset.UTC)
                                )
                            }

                    val file = processingEnv.filer.createResource(StandardLocation.SOURCE_OUTPUT, parsedInterface.type.packageName, "${parsedInterface.type.name}Impl.kt", element)

                    file.openWriter().use { it.write(text) }
                } catch (ex: KewtException) {
                    Logger.error(ex, "Cannot process ${element.simpleName}: ${ex.message}")
                }
            }
            Logger.info("Finished processing @Mapper annotations")
        } catch (ex: Throwable) {
            Logger.error(ex, "Kewt annotiation processor finished exceptionally")
            throw ex
        }

        return false
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
                        sourcePath = it.source.substringAfter(".").split("."),
                        targetParameterName = it.target
                )
            }
}

data class MappedFunction(
        val name: String,
        val parameters: List<Parameter>,
        val returnType: ResolvedType<Parameter>,
        val mappings: List<PropertyMapping>,
        val targetParameterName: String?
)

data class ExplicitConverter(
        val targetName: String,
        val converterName: String
)

data class NameMapping(
        val parameterName: String,
        val sourcePath: List<String>,
        val targetParameterName: String
) {
    override fun toString() = "$targetParameterName <= ${(listOf(parameterName) + sourcePath).joinToString(".")}"
}

data class ConversionFunction(
        val name: String,
        val parameter: Parameter,
        val returnType: Type,
        val isExtension: Boolean
) {
    override fun toString() = "$name($parameter): $returnType"
}
