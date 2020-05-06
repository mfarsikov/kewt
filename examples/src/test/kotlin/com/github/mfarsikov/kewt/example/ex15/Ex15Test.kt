package com.github.mfarsikov.kewt.example.ex15

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class Ex15Test{
    @Test
    fun test(){
        val res = PersonMapperImpl().toEmployee(Person("John"), "X")

        res.name shouldBe "John"
        res.id shouldBe "X"
        res.happy shouldBe true
    }
}