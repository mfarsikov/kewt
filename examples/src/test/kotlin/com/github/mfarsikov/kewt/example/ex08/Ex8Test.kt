package com.github.mfarsikov.kewt.example.ex08

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class Ex8Test {
    @Test
    fun test() {
        val res = PersonMapperImpl().toStudent(Person(
                firstName = "John",
                lastName = "Doe",
                age = 20,
                keys = listOf("00000000-0000-0000-0000-000000000000"),
                dog = Dog(
                        name = "Jack"
                ),
                passports = listOf(PersonPassport(
                        passportNumber = "00000000-0000-0000-0000-000000000001"
                ))
        ))

        with(res) {
            name shouldBe "John"
            lastName shouldBe "Doe"
            completeYears shouldBe 20
            keys shouldBe listOf("00000000-0000-0000-0000-000000000000")
            pet shouldBe Dog(name= "Jack")
            studentIds shouldBe listOf(StudentId(number = "00000000-0000-0000-0000-000000000001"))
        }
    }
}