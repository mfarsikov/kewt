package com.github.mfarsikov.kewt.example.ex5

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.*

class Ex5Test {
    @Test
    fun test() {
        val res = PersonMapperImpl().toEmployee(Person(
                ids = listOf(UUID.fromString("00000000-0000-0000-0000-000000000000"))
        ))

        with(res) {
            ids shouldBe listOf("00000000-0000-0000-0000-000000000000")
        }
    }
}