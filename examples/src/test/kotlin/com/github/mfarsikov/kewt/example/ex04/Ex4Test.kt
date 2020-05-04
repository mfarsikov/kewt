package com.github.mfarsikov.kewt.example.ex04

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.*

class Ex4Test {
    @Test
    fun test() {
        val res = PersonMapperImpl().toEmployee(Person(
                id = UUID.fromString("00000000-0000-0000-0000-000000000000")
        ))

        with(res) {
            personId shouldBe "00000000-0000-0000-0000-000000000000"
        }
    }
}