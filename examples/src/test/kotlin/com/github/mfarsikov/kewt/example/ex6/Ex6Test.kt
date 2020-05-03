package com.github.mfarsikov.kewt.example.ex6

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.*

class Ex6Test {
    @Test
    fun test() {
        val res = PersonMapperImpl().toEmployee(Person(
                name = "John",
                surname = "Doe"
        ))

        with(res) {
            firstName shouldBe "John"
            lastName shouldBe "Doe"
        }
    }
}