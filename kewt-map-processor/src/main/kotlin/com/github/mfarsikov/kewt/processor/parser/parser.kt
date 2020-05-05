package com.github.mfarsikov.kewt.processor.parser

import com.github.mfarsikov.kewt.annotations.Mappings
import com.github.mfarsikov.kewt.processor.AClass
import com.github.mfarsikov.kewt.processor.Function
import com.github.mfarsikov.kewt.processor.Parameter
import com.github.mfarsikov.kewt.processor.Type
import com.github.mfarsikov.kewt.processor.extractPackage
import com.github.mfarsikov.kewt.processor.mapper.AnnotationConfig
import com.github.mfarsikov.kewt.processor.toType
import com.squareup.kotlinpoet.asTypeName
import com.sun.tools.javac.code.Symbol
import kotlinx.metadata.Flag
import kotlinx.metadata.KmClass
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element

fun parse(element: Element, processingEnv: ProcessingEnvironment): AClass {

    val converterPackage = processingEnv.elementUtils.getPackageOf(element)
    val converterInterfaceMetadata = element.getAnnotation(Metadata::class.java).kmClass()
    val e = element as Symbol.ClassSymbol

    val annotationsBySignature = e.members_field.elements.filterIsInstance<Symbol.MethodSymbol>()
            .associateBy({
                FunctionSignature(
                        name = it.name.toString(),
                        params = it.params.map { it.type.toType() }
                )
            }) {
                it.getAnnotation(Mappings::class.java)
            }


    val parsedFunctions = converterInterfaceMetadata!!.functions.map { converterFunction ->

        val inputParams = converterFunction.valueParameters.map { parameter ->
            Parameter(name = parameter.name, type = parameter.type!!.toType())
        }

        val sig = FunctionSignature(name = converterFunction.name, params = inputParams.map { it.type })

        val annotation = annotationsBySignature[sig]

        val annotationConfigs = annotation?.value?.map { AnnotationConfig(source = it.source, target = it.target, converter = it.converter.takeIf { it.isNotBlank() }) }
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

private fun com.sun.tools.javac.code.Type.toType() = Type(
        packageName = asTypeName().toString().extractPackage(),
        name = asTypeName().toString().substringAfterLast(".")
)


data class FunctionSignature(
        val name: String,
        val params: List<Type>
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