package com.github.mfarsikov.kewt.example.ex13

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class Ex13Test {
    @Test
    fun test() {
        val res = PersonMapperImpl().convert(
                person = Person(
                        name = "John",
                        pet = Pet(name = PetName(firstName = "Jack", lastName = "Poop")
                        )
                )
        )

        with(res) {
            name shouldBe "John"
            with(pet) {
                petFirstName shouldBe "Jack"
                petLastName shouldBe "Poop"
            }
        }
    }
}