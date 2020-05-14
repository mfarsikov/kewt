package com.github.mfarsikov.kewt.processor.render

import com.github.mfarsikov.kewt.processor.Type
import com.github.mfarsikov.kewt.processor.mapper.Language
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import java.time.OffsetDateTime

fun renderExt(converter: RenderConverterClass, version: String, date: OffsetDateTime): String {
    val fsb = FileSpec.builder(converter.type.packageName, converter.type.name + "Impl")

    val generatedAnnotation = AnnotationSpec.builder(ClassName("javax.annotation", "Generated"))
            .addMember("value = [%S]", "com.github.mfarsikov.kewt.processor.KewtMapperProcessor")
            .addMember("date = %S", date)
            .addMember("comments = %S", version)
            .build()
    fsb.addAnnotation(generatedAnnotation)



    converter.converterFunctions.forEach { function ->
        val funcBuilder = FunSpec.builder(function.name).receiver(ClassName(function.parameters.first().type.packageName, function.parameters.first().type.name))

        function.parameters.drop(1).forEach { parameter ->
            val paramBuilder = ParameterSpec.builder(parameter.name, ClassName(parameter.type.packageName, parameter.type.name))

            funcBuilder.addParameter(paramBuilder.build())
        }

        funcBuilder.returns(ClassName(function.returnType.packageName, function.returnType.name))

        val receiverName = function.parameters.first().name

        val ms = function.mappings.map {
            if (it.parameterName == receiverName) it.copy(parameterName = "this@${function.name}") else it
        }

        val functionBody = when (function.returnTypeLanguage) {
            Language.KOTLIN -> generateKotlinConstructorCallExt(function.returnType, ms, function.targetParameterName)
            Language.PROTO -> generateProtobufBuilderCall(function.name, function.returnType, ms)
            Language.JAVA -> TODO("generate java setters")
        }

        funcBuilder.addCode(functionBody)

        fsb.addFunction(funcBuilder.build())
    }

    return fsb.build().toString()
}

private fun generateProtobufBuilderCall(fname: String, returnType: Type, mappings: List<RenderPropertyMappings>): CodeBlock {
    val codeBuilder = CodeBlock.builder()
    codeBuilder.add("""@Suppress("UNNECESSARY_SAFE_CALL")""")
    codeBuilder.beginControlFlow("return ${returnType.name}.newBuilder().apply {")
    codeBuilder.indent()
    codeBuilder.add(
            mappings.map {
                "${it.parameterName}.${it.sourcePropertyName}" +
                        if (it.conversionContext.conversionFunction != null) {
                            if (it.conversionContext.usingElementMapping) {
                                "?.map { ${functionCall(it.conversionContext.conversionFunction.name, "it", it.conversionContext.conversionFunction.isExtension)} }"
                            } else {
                                "?.let { ${functionCall(it.conversionContext.conversionFunction.name, "it",  it.conversionContext.conversionFunction.isExtension)} }"
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

fun functionCall(fname: String, parameterName: String, isExtension: Boolean) = if (isExtension)   "$parameterName.$fname()" else "$fname($parameterName)"


private fun generateKotlinConstructorCallExt(type: Type, mappings: List<RenderPropertyMappings>, targetParameterName: String?): CodeBlock {
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


