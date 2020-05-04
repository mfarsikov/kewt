package com.github.mfarsikov.kewt.example.ex02

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.*

class Ex2Test {
    @Test
    fun test() {
        val res = PersonMapperImpl().toEmployee(Person(
                id = UUID.fromString("00000000-0000-0000-0000-000000000000"),
                licenceId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        ))

        with(res) {
            id shouldBe "00000000-0000-0000-0000-000000000000"
            licenceId shouldBe "00000000-0000-0000-0000-000000000001"
        }
    }
}