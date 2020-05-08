package com.github.mfarsikov.kewt.processor.parser

import com.github.mfarsikov.kewt.annotations.Mappings
import com.github.mfarsikov.kewt.annotations.Target
import com.github.mfarsikov.kewt.processor.extractPackage
import com.squareup.kotlinpoet.asTypeName
import com.sun.tools.javac.code.Symbol
import javax.lang.model.element.Element

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


fun com.sun.tools.javac.code.Type.toSimpleType() = SimpleType(
        packageName = asTypeName().toString().extractPackage(),
        name = asTypeName().toString().substringAfterLast(".")
)

/**
 * does not include nullability or language information
 */
data class SimpleType(
        val packageName: String,
        val name: String
)