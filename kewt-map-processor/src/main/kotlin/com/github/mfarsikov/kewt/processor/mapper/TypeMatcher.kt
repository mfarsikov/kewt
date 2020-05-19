package com.github.mfarsikov.kewt.processor.mapper

import com.github.mfarsikov.kewt.processor.KewtException
import com.github.mfarsikov.kewt.processor.Nullability.NON_NULLABLE
import com.github.mfarsikov.kewt.processor.Nullability.NULLABLE
import com.github.mfarsikov.kewt.processor.Nullability.PLATFORM
import com.github.mfarsikov.kewt.processor.Type

class TypeMatcher(
        //TODO validation that there is no duplications (by input and return types)
        private val conversionFunctions: List<MapperConversionFunction>
) {

    fun findConversion(from: Type, to: Type, explicitConverter: String? = null): MappingConversionContext? {

        if (from canBeAssignedTo to) return MappingConversionContext()


        val conversionFunctionCandidates = conversionFunctions
                .filter { explicitConverter == null || it.name == explicitConverter }
                .filter { from canBeAssignedTo it.parameter.type && it.returnType canBeAssignedTo to }

        if (conversionFunctionCandidates.size > 1) throw KewtException("More than one function can convert ($from) -> $to: $conversionFunctionCandidates")
        val conversionFunction = conversionFunctionCandidates.singleOrNull()
        return when {
            conversionFunction != null -> MappingConversionContext(conversionFunction)
            from.nullability == NULLABLE && (to.nullability == NULLABLE || to.nullability == PLATFORM) -> findConversion(from.copy(nullability = NON_NULLABLE), to, explicitConverter)?.copy(usingNullSafeCall = true)
            from.isList() && to.isList() && (from.copy(typeParameters = emptyList()) canBeAssignedTo to.copy(typeParameters = emptyList())) -> findConversion(from.typeParameters.single(), to.typeParameters.single(), explicitConverter)
                    ?.copy(usingElementMapping = true)
            else -> null
        }
    }

    private fun Type.isList() = (name == "List" || name == "MutableList") &&
            (packageName == "kotlin.collections" || packageName == "java.util")

    private infix fun Type.canBeAssignedTo(type: Type): Boolean =
            (type.qualifiedName() in aliases[this.qualifiedName()] ?: listOf(this.qualifiedName()))
                    && (this.nullability == NON_NULLABLE || type.nullability == NULLABLE || type.nullability == PLATFORM || this.nullability == PLATFORM)
                    && this.typeParameters.zip(type.typeParameters).all { (a, b) -> a canBeAssignedTo b } //TODO variance?
}

val aliases = listOf(
        listOf("kotlin.Int", "java.lang.Integer", "int"),
        listOf("kotlin.Long", "java.lang.Long", "long"),
        listOf("kotlin.Double", "java.lang.Double", "double"),
        listOf("kotlin.Float", "java.lang.Float", "float"),
        listOf("kotlin.String", "java.lang.String"),
        listOf("kotlin.Boolean", "java.lang.Boolean", "boolean"),
        listOf("kotlin.collections.List", "kotlin.collections.MutableList", "java.util.List"),
        listOf("kotlin.collections.Map", "kotlin.collections.MutableMap", "java.util.Map")
).flatMap { list ->
    list.flatMap { item ->
        list.map { it to item }
    }
}.groupBy({ it.first }) { it.second }
