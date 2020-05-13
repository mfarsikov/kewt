package com.github.mfarsikov.kewt.example.ex20

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ExtensionfunctionTest {
    @Test
    fun test(){
        val res: Employee = Person("John").toEmployee()

        res.name shouldBe "John"
    }
}