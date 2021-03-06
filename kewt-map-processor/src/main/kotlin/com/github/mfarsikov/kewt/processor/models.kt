package com.github.mfarsikov.kewt.processor

import com.github.mfarsikov.kewt.processor.mapper.AnnotationConfig
import com.github.mfarsikov.kewt.processor.parser.SimpleType


data class ParsedMapper(
        val type: Type,
        val functions: Iterable<Function>,
        val isInterface:Boolean
)

data class Function(
        val name: String,
        val parameters: List<FunctionParameter>,
        val returnType: Type,
        val annotationConfigs: List<AnnotationConfig>,
        val abstract: Boolean,
        val isExtension: Boolean
) {
    override fun toString() = "$name(${parameters.map { it }.joinToString()}): ${returnType}"
}

data class FunctionParameter(
        val name: String,
        val type: Type,
        val isTarget: Boolean
){
    override fun toString() = toParameter().toString()
}

fun FunctionParameter.toParameter() = Parameter(name, type)

data class Parameter(
        val name: String,
        val type: Type
) {
    override fun toString() = "$name: $type"
}

data class ConstructorParameter(
        val name: String,
        val type: Type,
        val hasDefaultValue: Boolean
) {
    override fun toString() = "$name: $type"
}

data class Type(
        val packageName: String,
        val name: String,
        val nullability: Nullability = Nullability.NON_NULLABLE,
        val typeParameters: List<Type> = emptyList()
) {
    override fun toString() = "${qualifiedName()}${params()}${nullableSign()}"

    fun qualifiedName() = "${packageName.takeIf { it.isNotEmpty() }?.let { "$it." } ?: ""}$name"
    private fun nullableSign() = when (nullability) {
        Nullability.NULLABLE -> "?"
        Nullability.NON_NULLABLE -> ""
        Nullability.PLATFORM -> "!"
    }

    private fun params() = typeParameters.takeIf { it.isNotEmpty() }?.joinToString(prefix = "<", postfix = ">") ?: ""
}

fun Type.toSimpleType() = SimpleType(packageName = packageName, name = name)
enum class Nullability {
    NULLABLE {
        override fun toString() = "?"
    },
    NON_NULLABLE {
        override fun toString() = ""
    },
    PLATFORM {
        override fun toString() = "!"
    }
}
