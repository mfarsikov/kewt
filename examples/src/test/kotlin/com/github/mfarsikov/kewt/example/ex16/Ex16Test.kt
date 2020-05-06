package com.github.mfarsikov.kewt.example.ex16

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.*

class Ex16Test {
    @Test
    fun test(){
        val res = PesronMapperImpl().toEmployee(Person(UUID.fromString("00000000-0000-0000-0000-000000000000")))

        res.id shouldBe "00000000-0000-0000-0000-000000000000"
    }
}