package com.github.mfarsikov.kewt.processor.parser

import com.github.mfarsikov.kewt.processor.AClass
import com.github.mfarsikov.kewt.processor.Function
import com.github.mfarsikov.kewt.processor.FunctionParameter
import com.github.mfarsikov.kewt.processor.Type
import com.github.mfarsikov.kewt.processor.mapper.AnnotationConfig
import com.github.mfarsikov.kewt.processor.toSimpleType
import com.github.mfarsikov.kewt.processor.toType
import kotlinx.metadata.Flag
import kotlinx.metadata.KmClass
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element

fun parse(element: Element, processingEnv: ProcessingEnvironment): AClass {

    val converterPackage = processingEnv.elementUtils.getPackageOf(element)
    val converterInterfaceMetadata = element.getAnnotation(Metadata::class.java).kmClass()

    val annotatedFunctions = extractAnnotationsFromJava(element)

    val parsedFunctions = converterInterfaceMetadata!!.functions.map { converterFunction ->

        val sig = FunctionSignature(name = converterFunction.name, params = converterFunction.valueParameters.map { it.type!!.toType().toSimpleType() })

        val annotatedFunction = annotatedFunctions.singleOrNull { it.signature() == sig }

        val inputParams = converterFunction.valueParameters.map { parameter ->
            FunctionParameter(
                    name = parameter.name,
                    type = parameter.type!!.toType(),
                    isTarget = annotatedFunction?.parameters?.single { it.name == parameter.name }?.isTarget?:false
            )
        }

        val annotationConfigs = annotatedFunction?.mappingsAnnotation?.value?.map { AnnotationConfig(source = it.source, target = it.target, converter = it.converter.takeIf { it.isNotBlank() }) }
                ?: emptyList()

        Function(
                name = converterFunction.name,
                parameters = inputParams,
                returnType = converterFunction.returnType.toType(),
                annotationConfigs = annotationConfigs,
                abstract = Flag.IS_ABSTRACT.invoke(converterFunction.flags)
        )
    }

    return AClass(
            type = Type(
                    packageName = converterPackage.qualifiedName.toString(),
                    name = converterInterfaceMetadata.name.substringAfterLast("/")
            ),
            functions = parsedFunctions
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



fun Metadata.kmClass(): KmClass? =
        let {
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
                ?.tryCast<KotlinClassMetadata.Class>()
                ?.toKmClass()

private inline fun <reified T> Any.tryCast(): T? = if (this is T) this else null