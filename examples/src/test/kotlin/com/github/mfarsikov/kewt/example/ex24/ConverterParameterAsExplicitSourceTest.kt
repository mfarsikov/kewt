package com.github.mfarsikov.kewt.example.ex24

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ConverterParameterAsExplicitSourceTest {
    @Test
    fun test() {
        val res = PersonMapper24Impl().toEmployee(Person("John"))

        res.upperCasedName shouldBe "JOHN"
    }
}