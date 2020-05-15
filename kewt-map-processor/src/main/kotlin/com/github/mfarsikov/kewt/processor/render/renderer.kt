package com.github.mfarsikov.kewt.processor.render

import com.github.mfarsikov.kewt.processor.ConversionFunction
import com.github.mfarsikov.kewt.processor.Parameter
import com.github.mfarsikov.kewt.processor.Type
import com.github.mfarsikov.kewt.processor.mapper.Language
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import java.time.OffsetDateTime

fun render(converter: RenderConverterClass): String = if (converter.isInterface) {
    renderInterface(
            converter = converter,
            version = converter.version,
            date = converter.date
    )
} else {
    renderExt(
            converter = converter,
            version = converter.version,
            date = converter.date
    )
}


fun renderInterface(converter: RenderConverterClass, version: String, date: OffsetDateTime): String {
    val fsb = FileSpec.builder(converter.type.packageName, converter.type.name)
    val classBuilder = TypeSpec.classBuilder("${converter.type.name}Impl")
    classBuilder.addSuperinterface(ClassName(converter.type.packageName, converter.type.name))

    val generatedAnnotation = AnnotationSpec.builder(ClassName("javax.annotation", "Generated"))
            .addMember("value = [%S]", "com.github.mfarsikov.kewt.processor.KewtMapperProcessor")
            .addMember("date = %S", date)
            .addMember("comments = %S", version)
            .build()
    classBuilder.addAnnotation(generatedAnnotation)

    if (converter.springComponent) {
        val springComponent = AnnotationSpec.builder(ClassName("org.springframework.stereotype", "Component")).build()
        classBuilder.addAnnotation(springComponent)
    }

    converter.converterFunctions.forEach { function ->
        val funcBuilder = FunSpec.builder(function.name).addModifiers(KModifier.OVERRIDE)

        function.parameters.forEach { parameter ->
            val paramBuilder = ParameterSpec.builder(parameter.name, ClassName(parameter.type.packageName, parameter.type.name))

            funcBuilder.addParameter(paramBuilder.build())
        }

        funcBuilder.returns(ClassName(function.returnType.packageName, function.returnType.name))

        val functionBody = when (function.returnTypeLanguage) {
            Language.KOTLIN -> generateKotlinConstructorCall(function.returnType, function.mappings, function.targetParameterName)
            Language.PROTO -> generateProtobufBuilderCall(function.returnType, function.mappings)
            Language.JAVA -> TODO("generate java setters")
        }

        funcBuilder.addCode(functionBody)

        classBuilder.addFunction(funcBuilder.build())
    }
    fsb.addType(classBuilder.build())

    return fsb.build().toString()
}

private fun generateProtobufBuilderCall(returnType: Type, mappings: List<RenderPropertyMappings>): CodeBlock {
    val codeBuilder = CodeBlock.builder()
    codeBuilder.add("""@Suppress("UNNECESSARY_SAFE_CALL")""")
    codeBuilder.beginControlFlow("return ${returnType.name}.newBuilder().apply {")
    codeBuilder.indent()
    codeBuilder.add(
            mappings.map {
                "${it.parameterName}.${it.sourcePropertyName}" +
                        if (it.conversionContext.conversionFunction != null) {
                            if (it.conversionContext.usingElementMapping) {
                                "?.map { ${it.conversionContext.conversionFunction.name}(it) }"
                            } else {
                                "?.let { ${it.conversionContext.conversionFunction.name}(it) }"
                            }
                        } else {
                            ""
                        } +
                        if (it.targetPropertyName.endsWith("List")) {
                            "?.also { addAll${it.targetPropertyName.capitalize().substringBefore("List")}(it) }"
                        } else {
                            "?.also { ${it.targetPropertyName} = it }"
                        }
            }
                    .joinToString(separator = "\n")
    )
    codeBuilder.add("\n")
    codeBuilder.unindent()

    codeBuilder.endControlFlow()
    codeBuilder.add(".build()\n")
    return codeBuilder.build()
}

fun generateKotlinConstructorCall(type: Type, mappings: List<RenderPropertyMappings>, targetParameterName: String?): CodeBlock {
    val codeBuilder = CodeBlock.builder().indent().add("return ${type.name}(")
    codeBuilder.indent().indent()

    codeBuilder.add(mappings.map {

        val sourceExtraction = listOf(it.parameterName, it.sourcePropertyName).filter { it.isNotBlank() }.joinToString(".")

        val rightPart = when {
            it.conversionContext.usingElementMapping -> "$sourceExtraction.map { ${it.conversionContext.conversionFunction!!.name}(it) }"
            it.conversionContext.conversionFunction != null -> "${it.conversionContext.conversionFunction.name}($sourceExtraction)"
            else -> sourceExtraction
        }

        val copyFromTargetBlock = if (targetParameterName != null) " ?: $targetParameterName.${it.targetPropertyName}" else ""

        "${it.targetPropertyName} = $rightPart" + copyFromTargetBlock
    }
            .joinToString(separator = ",\n", prefix = "\n", postfix = "\n"))

    codeBuilder.unindent().unindent()
    codeBuilder.add(")")
    codeBuilder.unindent()
    return codeBuilder.build()
}

data class RenderConverterClass(
        val type: Type,
        val springComponent: Boolean,
        val converterFunctions: List<RenderConverterFunction>,
        val isInterface: Boolean,
        val version: String,
        val date: OffsetDateTime
)

data class RenderConverterFunction(
        val name: String,
        val returnTypeLanguage: Language,
        val parameters: List<Parameter>,
        val returnType: Type,
        val mappings: List<RenderPropertyMappings>,
        val targetParameterName: String?
)

data class RenderPropertyMappings(
        val parameterName: String,
        val sourcePropertyName: String,
        val targetPropertyName: String,
        val conversionContext: RenderConversionContext
)

data class RenderConversionContext(
        val conversionFunction: ConversionFunction? = null,
        val usingElementMapping: Boolean = false,
        val usingNullSafeCall: Boolean = false,
        val nullableAssignedToPlatform: Boolean = false
)
