package com.github.mfarsikov.kewt.example.proto.ex02

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import protohub.Employee
import protohub.Passport
import protohub.Person

class Ex02Test {
    @Test
    fun test() {
        val person = Person.newBuilder().apply {
            name = nameBuilder.apply {
                name = "John"
                surname = "Doe"
            }.build()
            idLong = 10
            happyOrNot = true
            addAllIds(listOf(1, 2, 3))
            pet = petBuilder.apply {
                name = "Jack"
                age = 1
            }.build()
            addAllPassports(listOf(Passport.newBuilder().apply {
                id = "ID"
            }.build()))
        }.build()

        val res = person.convert()

        with(res) {
            firstName shouldBe "John"
            lastName shouldBe "Doe"
            id shouldBe 10
            happy shouldBe true
            idsList shouldBe listOf("1", "2", "3")
            with(pet) {
                name shouldBe "Jack"
                age shouldBe 1
            }
            identitiesList.single().id shouldBe "ID"
        }
    }

    @Test
    fun `map list of java strings to list of kotlin strings`() {
        val res = Employee.newBuilder().addAllIds(listOf("1", "2", "3")).build().toMyEmployee()

        res.ids shouldBe listOf("1", "2", "3")
    }
}