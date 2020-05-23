package com.github.mfarsikov.kewt.processor.parser

import com.github.mfarsikov.kewt.processor.Function
import com.github.mfarsikov.kewt.processor.FunctionParameter
import com.github.mfarsikov.kewt.processor.ParsedMapper
import com.github.mfarsikov.kewt.processor.Type
import com.github.mfarsikov.kewt.processor.mapper.AnnotationConfig
import com.github.mfarsikov.kewt.processor.mapper.aliases
import com.github.mfarsikov.kewt.processor.toSimpleType
import com.github.mfarsikov.kewt.processor.toType
import kotlinx.metadata.Flag
import kotlinx.metadata.KmAnnotation
import kotlinx.metadata.KmExtensionType
import kotlinx.metadata.KmPackage
import kotlinx.metadata.KmTypeExtensionVisitor
import kotlinx.metadata.KmTypeVisitor
import kotlinx.metadata.jvm.JvmTypeExtensionVisitor
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element

fun parse(element: Element, processingEnv: ProcessingEnvironment): ParsedMapper {
    val kotlinClassMetadata = read(element.getAnnotation(Metadata::class.java))

    return when (kotlinClassMetadata) {
        is KotlinClassMetadata.FileFacade -> parseFile(element, processingEnv, kotlinClassMetadata)
        is KotlinClassMetadata.Class -> parseInterface(element, processingEnv, kotlinClassMetadata)
        else -> throw RuntimeException()
    }
}

fun parseInterface(element: Element, processingEnv: ProcessingEnvironment, kotlinClassMetadata: KotlinClassMetadata.Class): ParsedMapper {

    val converterPackage = processingEnv.elementUtils.getPackageOf(element)

    val converterInterfaceMetadata = kotlinClassMetadata.toKmClass()

    //"annotated" is bad name it is not actually annotated. it just could have @Target annotation. TODO rename
    val annotatedFunctions = extractAnnotationsFromJava(element)

    val parsedFunctions = converterInterfaceMetadata.functions.map { converterFunction ->

        val sig = FunctionSignature(name = converterFunction.name, params = converterFunction.valueParameters.map { it.type!!.toType().toSimpleType() })

        val annotatedFunction = annotatedFunctions.singleOrNull { it.signature() same sig }

        val inputParams = converterFunction.valueParameters.map { parameter ->
            FunctionParameter(
                    name = parameter.name,
                    type = parameter.type!!.toType(),
                    isTarget = annotatedFunction?.parameters?.single { it.name == parameter.name }?.isTarget ?: false
            )
        }

        val annotationConfigs = annotatedFunction?.mappingsAnnotation?.value?.map { AnnotationConfig(source = it.source, target = it.target, converter = it.converter.takeIf { it.isNotBlank() }) }
                ?: emptyList()

        Function(
                name = converterFunction.name,
                parameters = inputParams,
                returnType = converterFunction.returnType.toType(),
                annotationConfigs = annotationConfigs,
                abstract = Flag.IS_ABSTRACT.invoke(converterFunction.flags),
                isExtension = false
        )
    }

    return ParsedMapper(
            type = Type(
                    packageName = converterPackage.qualifiedName.toString(),
                    name = converterInterfaceMetadata.name.substringAfterLast("/")
            ),
            functions = parsedFunctions,
            isInterface = true
    )
}

infix fun FunctionSignature.same(g: FunctionSignature): Boolean =
        name == g.name &&
                params.zip(g.params)
                        .all { (a, b) -> a in aliases[b] ?: listOf(b) }

fun parseFile(element: Element, processingEnv: ProcessingEnvironment, kotlinClassMetadata: KotlinClassMetadata.FileFacade): ParsedMapper {

    val converterPackage = processingEnv.elementUtils.getPackageOf(element)

    val fileMetadata = kotlinClassMetadata.kmFile()

    val annotatedFunctions = extractAnnotationsFromJavaPackage(element)

    val functions = fileMetadata!!.functions.map {
        it.receiverParameterType

        Function(
                name = it.name,
                parameters = it.valueParameters.map { parameter ->
                    FunctionParameter(
                            name = parameter.name,
                            type = parameter.type!!.toType(),
                            isTarget = false
                    )
                },
                returnType = it.returnType.toType(),
                annotationConfigs = emptyList(),
                abstract = false,
                isExtension = it.receiverParameterType != null
        )
    }

    val parsedFunctions = fileMetadata.properties.map { property ->


        val accumulator = mutableListOf<KmAnnotation>()

        property.returnType.accept(object : KmTypeVisitor() {
            override fun visitExtensions(type: KmExtensionType): KmTypeExtensionVisitor? {
                return object : JvmTypeExtensionVisitor() {
                    override fun visitAnnotation(annotation: KmAnnotation) {
                        accumulator.add(annotation)
                    }
                }
            }
        })

        val isExtension = accumulator.firstOrNull()?.className == "kotlin/ExtensionFunctionType"


        val sig = FunctionSignature(name = property.name, params = property.returnType.arguments.dropLast(1).map { it.type!!.toType().toSimpleType() })


        val annotatedFunction = annotatedFunctions.singleOrNull { it.signature() same sig }

        val inputParams = property.returnType.arguments.dropLast(1).mapIndexed { i, parameter ->
            val type = parameter.type!!.toType()
            FunctionParameter(
                    name = "${type.name.decapitalize()}$i",
                    type = type,
                    isTarget = annotatedFunction?.parameters?.single { it.name == "param$i" }?.isTarget
                            ?: false //TODO get parameter by index?
            )
        }


        val annotationConfigs = annotatedFunction
                ?.mappingsAnnotation
                ?.value
                ?.map {
                    AnnotationConfig(
                            source = it.source,
                            target = it.target,
                            converter = it.converter.takeIf { it.isNotBlank() }
                    )
                }
                ?: emptyList()

        Function(
                name = property.name,
                parameters = inputParams,
                returnType = property.returnType.arguments.last().type!!.toType(),
                annotationConfigs = annotationConfigs,
                abstract = true,
                isExtension = isExtension
        )
    }

    return ParsedMapper(
            type = Type(
                    packageName = converterPackage.qualifiedName.toString(),
                    name = element.simpleName.toString().substringBeforeLast("Kt")
            ),
            functions = parsedFunctions + functions,
            isInterface = false
    )
}

data class FunctionSignature(
        val name: String,
        val params: List<SimpleType>
)

fun AnnotatedFunction.signature() = FunctionSignature(
        name = name,
        params = parameters.map { it.simpleType }
)

fun KotlinClassMetadata.FileFacade.kmFile(): KmPackage? = let {
    KmPackage().apply(it::accept)
}

fun read(metadata: Metadata): KotlinClassMetadata? = metadata.let {
    KotlinClassHeader(
            it.kind,
            it.metadataVersion,
            it.bytecodeVersion,
            it.data1,
            it.data2,
            it.extraString,
            it.packageName,
            it.extraInt
    )
}
        .let { KotlinClassMetadata.read(it) }


inline fun <reified T> Any.tryCast(): T? = if (this is T) this else null