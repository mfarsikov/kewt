package com.github.mfarsikov.kewt.annotations

/**
 * Is used on interfaces. Kewt generates implementation classes for such interfaces.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
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
 */
@Repeatable
@Target(AnnotationTarget.FUNCTION)
annotation class Mapping(
        val source: String = "",
        val target: String = ""
)

/**
 * Is used on abstract methods as a wrapper for array of Mapping annotations.
 * Most probably will be removed when Kotlin will support repeatable annotations.
 */
@Target(AnnotationTarget.FUNCTION)
annotation class Mappings(
        val value: Array<Mapping>
)