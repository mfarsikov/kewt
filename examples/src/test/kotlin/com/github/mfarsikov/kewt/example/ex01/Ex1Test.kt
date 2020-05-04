package com.github.mfarsikov.kewt.example.ex01

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.*

class Ex1Test {
    @Test
    fun test(){
        val res = PersonMapperImpl().toEmployee(Person(
                UUID.fromString("48726135-3b71-4750-be7e-f1ff1ad38b40"),
                "Kot",
                10
        ))
        with(res){
            id shouldBe UUID.fromString("48726135-3b71-4750-be7e-f1ff1ad38b40")
            name shouldBe "Kot"
            age shouldBe 10
        }
    }
}