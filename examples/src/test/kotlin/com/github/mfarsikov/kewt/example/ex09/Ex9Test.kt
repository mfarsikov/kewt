package com.github.mfarsikov.kewt.example.ex09

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.*

class Ex9Test {
    @Test
    fun test() {
        val res = PersonMapperImpl().toEmployee(Person(
                name = "John",
                surname = "Doe",
                id = UUID.fromString("00000000-0000-0000-0000-000000000000")
        ))

        with(res) {
            name shouldBe "John"
            surname shouldBe "Doe"
        }
    }
}