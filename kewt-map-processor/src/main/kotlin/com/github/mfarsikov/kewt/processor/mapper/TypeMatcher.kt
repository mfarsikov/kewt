package com.github.mfarsikov.kewt.processor.mapper

import com.github.mfarsikov.kewt.processor.KewtException
import com.github.mfarsikov.kewt.processor.Nullability.NON_NULLABLE
import com.github.mfarsikov.kewt.processor.Nullability.NULLABLE
import com.github.mfarsikov.kewt.processor.Nullability.PLATFORM
import com.github.mfarsikov.kewt.processor.Type
import com.github.mfarsikov.kewt.processor.parser.SimpleType
import com.github.mfarsikov.kewt.processor.toSimpleType

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
            (type.toSimpleType() in aliases[this.toSimpleType()] ?: listOf(this.toSimpleType()))
                    && (this.nullability == NON_NULLABLE || type.nullability == NULLABLE || type.nullability == PLATFORM || this.nullability == PLATFORM)
                    && this.typeParameters.zip(type.typeParameters).all { (a, b) -> a canBeAssignedTo b } //TODO variance?
}

val aliases = listOf(
        listOf(SimpleType("kotlin", "Int"), SimpleType("java.lang", "Integer"), SimpleType("", "int")),
        listOf(SimpleType("kotlin", "Long"), SimpleType("java.lang", "Long"), SimpleType("", "long")),
        listOf(SimpleType("kotlin", "Double"), SimpleType("java.lang", "Double"), SimpleType("", "double")),
        listOf(SimpleType("kotlin", "Float"), SimpleType("java.lang", "Float"), SimpleType("", "float")),
        listOf(SimpleType("kotlin", "String"), SimpleType("java.lang", "String")),
        listOf(SimpleType("kotlin", "Boolean"), SimpleType("java.lang", "Boolean"), SimpleType("", "boolean")),
        listOf(SimpleType("kotlin.collections", "List"), SimpleType("kotlin.collections", "MutableList"), SimpleType("java.util", "List")),
        listOf(SimpleType("kotlin.collections", "Map"), SimpleType("kotlin.collections", "MutableMap"), SimpleType("java.util", "Map"))
).flatMap { list ->
    list.flatMap { item ->
        list.map { it to item }
    }
}.groupBy({ it.first }) { it.second }
