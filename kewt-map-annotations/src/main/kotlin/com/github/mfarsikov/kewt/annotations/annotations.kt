package com.github.mfarsikov.kewt.annotations

import kotlin.annotation.AnnotationTarget.*

/**
 * Is used on interfaces. Kewt generates implementation classes for such interfaces.
 */
@Retention(AnnotationRetention.SOURCE)
@kotlin.annotation.Target(CLASS, FILE)
annotation class Mapper

/**
 * Explicit property mapping.
 * Is used on abstract methods, as an element inside Mappings annotation
 *
 * Example:
 * ```kotlin
 * @Mappings([
 *     Mapping(source = "person.name", target = "firstName")
 * ])
 * fun convert(person: Person): Employee
 * ```
 *
 * If function accepts a single parameter, parameter name could be omitted.
 *
 * "Lifting" properties:
 * ```kotlin
 * @Mappings([
 *     Mapping(source = "person.company.name", target = "companyName")
 * ])
 * fun convert(person: Person): Employee
 * ```
 *
 * @param source source property selector. May include property name; may have path to nested properties
 * @param target target property selector
 * @param converter function name, used if there more than one function with the same input and output types
 */
@Repeatable
@kotlin.annotation.Target(FUNCTION, FIELD)
annotation class Mapping(
        val source: String = "",
        val target: String = "",
        val converter: String = ""
)

/**
 * Is used on abstract methods as a wrapper for array of Mapping annotations.
 * Most probably will be removed when Kotlin will support repeatable annotations.
 * @param value array of explicit mappings
 */
@kotlin.annotation.Target(FUNCTION, FIELD)
annotation class Mappings(
        val value: Array<Mapping>
)

@kotlin.annotation.Target(FUNCTION, FIELD)
annotation class Isomorphism

/**
 * Specifies mapping target (one of function parameters)
 */
@kotlin.annotation.Target(VALUE_PARAMETER)
annotation class Target