package com.github.mfarsikov.kewt.processor.parser

import com.github.mfarsikov.kewt.annotations.Mappings
import com.github.mfarsikov.kewt.annotations.Target
import com.github.mfarsikov.kewt.processor.extractClassName
import com.github.mfarsikov.kewt.processor.extractPackage
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Type
import javax.lang.model.element.Element

fun extractAnnotationsFromJavaPackage(e: Element): List<AnnotatedFunction> {
    e as Symbol.ClassSymbol

    return e.members_field.elements.filterIsInstance<Symbol.VarSymbol>()
            .filter {
                it.type.tsym.type.toString().startsWith("kotlin.jvm.functions.Function")
            }
            .map {
                AnnotatedFunction(
                        name = it.name.toString(),
                        parameters = it.type.allparams().dropLast(1).mapIndexed { i, paramType ->
                            paramType as Type.WildcardType
                            AnnotatedFunctionParameter(
                                    name = "param$i",
                                    simpleType = paramType.toSimpleType(),
                                    isTarget = false
                            )
                        },
                        mappingsAnnotation = it.getAnnotation(Mappings::class.java)
                )
            }
}

fun extractAnnotationsFromJava(e: Element): List<AnnotatedFunction> {
    e as Symbol.ClassSymbol

    return e.members_field.elements.filterIsInstance<Symbol.MethodSymbol>()
            .map {
                AnnotatedFunction(
                        name = it.name.toString(),
                        parameters = it.params.map {
                            AnnotatedFunctionParameter(
                                    name = it.name.toString(),
                                    simpleType = it.type.toSimpleType(),
                                    isTarget = it.getAnnotation(Target::class.java) != null
                            )
                        },
                        mappingsAnnotation = it.getAnnotation(Mappings::class.java)
                )
            }
}

data class AnnotatedFunction(
        val name: String,
        val parameters: List<AnnotatedFunctionParameter>,
        val mappingsAnnotation: Mappings?
)

data class AnnotatedFunctionParameter(
        val name: String,
        val simpleType: SimpleType,
        val isTarget: Boolean
)


fun Type.toSimpleType() = SimpleType(
        packageName = toString().extractPackage(),
        name = toString().extractClassName()
)

fun Type.WildcardType.toSimpleType() = SimpleType(
        packageName = type.toString().extractPackage(),
        name = type.toString().extractClassName()
)

/**
 * does not include nullability or language information
 */
data class SimpleType(
        val packageName: String,
        val name: String
) {
    override fun toString() = listOf(packageName, name).filter { it.isNotEmpty() }.joinToString(".")
}