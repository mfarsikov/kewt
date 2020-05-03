package com.github.mfarsikov.kewt.annotations

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Mapper

@Repeatable
@Target(AnnotationTarget.FUNCTION)
annotation class Mapping(
        val source: String = "",
        val target: String = "",
        val converter: String = "",
        val provider: String = "",
        val value: String = "",
        val ignore: Boolean = false
)

annotation class Mappings(
        val value: Array<Mapping>
)