package com.github.mfarsikov.kewt.example.ex12

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class Ex12Test {
    @Test
    fun test() {
        val res = PersonMapperImpl().toEmployee(
                person = Person(name = "John"),
                pet = Pet(name = "Jack")
        )

        with(res) {
            name shouldBe "John"
            petName shouldBe "Jack"
        }
    }
}