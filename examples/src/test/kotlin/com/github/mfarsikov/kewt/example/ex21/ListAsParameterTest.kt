package com.github.mfarsikov.kewt.example.ex21

import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test

class ListAsParameterTest {
    @Test
    fun test() {
        val res = KindergardenMapperImpl().toKindergarden(listOf(Person("John")))

        res.children shouldHaveSize 1
    }
}