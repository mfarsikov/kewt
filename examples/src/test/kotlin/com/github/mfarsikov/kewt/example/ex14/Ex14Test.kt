package com.github.mfarsikov.kewt.example.ex14

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class Ex14Test {
    @Test
    fun test() {

        val res = PersonMapperImpl().convert(Person(id = 1))

        res.id shouldBe "1"
    }
}