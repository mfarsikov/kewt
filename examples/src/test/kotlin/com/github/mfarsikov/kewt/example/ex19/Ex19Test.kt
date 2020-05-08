package com.github.mfarsikov.kewt.example.ex19

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class Ex19Test {
    @Test
    fun test() {
        val source = Person(name = "John", age = null)
        val target = Person(name = null, age = 30)

        val res = PersonMapper19Impl().update(target, source)

        res.age shouldBe 30
        res.name shouldBe "John"
    }
}