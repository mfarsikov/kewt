package com.github.mfarsikov.kewt.processor

import com.github.mfarsikov.kewt.processor.mapper.AnnotationConfig


data class AClass(
        val type: Type,
        val functions: Iterable<Function>
)

data class Function(
        val name: String,
        val parameters: List<Parameter>,
        val returnType: Type,
        val annotationConfigs: List<AnnotationConfig>,
        val abstract: Boolean
) {
    override fun toString() = "$name(${parameters.map { it }.joinToString()}): ${returnType}"
}

data class Parameter(
        val name: String,
        val type: Type
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
