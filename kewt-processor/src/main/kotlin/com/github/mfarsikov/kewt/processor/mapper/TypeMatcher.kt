package com.github.mfarsikov.kewt.processor.mapper

import com.github.mfarsikov.kewt.processor.ConversionFunction
import com.github.mfarsikov.kewt.processor.Nullability.NON_NULLABLE
import com.github.mfarsikov.kewt.processor.Nullability.NULLABLE
import com.github.mfarsikov.kewt.processor.Nullability.PLATFORM
import com.github.mfarsikov.kewt.processor.Type

class TypeMatcher(
        //TODO validation that there is no duplications (by input and return types)
        private val conversionFunctions: List<ConversionFunction>
) {

    fun findConversion(from: Type, to: Type): ConversionContext? {

        if (from canBeAssignedTo to) return ConversionContext()


        val conversionFunction = conversionFunctions.filter { from canBeAssignedTo it.parameter.type && it.returnType canBeAssignedTo to }
                .singleOrNull()
        return when {
            conversionFunction != null -> ConversionContext(conversionFunction)
            from.nullability == NULLABLE && to.nullability == NULLABLE -> findConversion(from.copy(nullability = NON_NULLABLE), to)?.copy(usingNullSafeCall = true)
            from.isList() && to.isList() && (from.copy(typeParameters = emptyList()) canBeAssignedTo to.copy(typeParameters = emptyList())) -> findConversion(from.typeParameters.single(), to.typeParameters.single())
                    ?.copy(usingElementMapping = true)
            else -> null
        }
    }

    private fun Type.isList() = (name == "List" || name == "MutableList") &&
            (packageName == "kotlin.collections" || packageName == "java.util")

    private infix fun Type.canBeAssignedTo(type: Type): Boolean =
            (type.qualifiedName() in aliases[this.qualifiedName()] ?: listOf(this.qualifiedName()))
                    && (this.nullability == NON_NULLABLE || type.nullability == NULLABLE || type.nullability == PLATFORM || this.nullability == PLATFORM)
                    && this.typeParameters == type.typeParameters //TODO variance
}

val aliases = listOf(
        listOf("kotlin.Int", "java.lang.Integer", "int"),
        listOf("kotlin.Long", "java.lang.Long", "long"),
        listOf("kotlin.Double", "java.lang.Double", "double"),
        listOf("kotlin.Float", "java.lang.Float", "float"),
        listOf("kotlin.String", "java.lang.String"),
        listOf("kotlin.Boolean", "java.lang.Boolean", "boolean"),
        listOf("kotlin.collections.List", "java.util.List")
).flatMap { list ->
    list.flatMap { item ->
        list.map { it to item }
    }
}.groupBy({ it.first }) { it.second }