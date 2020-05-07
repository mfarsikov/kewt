package com.github.mfarsikov.kewt.example.ex18

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class Ex18Test {
    @Test
    fun test() {
        val res = PersonMapper18Impl().toEmployee(Person("John"))

        res.name shouldBe "John"
        res.id shouldBe 1
    }
}