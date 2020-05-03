package com.github.mfarsikov.kewt.example.ex10

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.*

class Ex10Test {
    @Test
    fun test() {
        val res = PersonMapperImpl().toEmployee(Person(
                name = Name(firstName = "John")
        ))

        with(res) {
            name shouldBe "John"
        }
    }
}