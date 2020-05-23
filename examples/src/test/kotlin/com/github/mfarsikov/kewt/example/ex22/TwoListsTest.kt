package com.github.mfarsikov.kewt.example.ex22

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TwoListsTest {
    @Test
    fun `distinguish data class properties by generic`() {
        val res = EmployeeMapper22Impl().toEmployee(Person(listOf("John"), listOf(30)))

        with(res) {
            empNames shouldBe listOf("John")
            empAges shouldBe listOf(30)
        }
    }

    @Test
    fun `distinguish function parameters by generic`() {
        val res = EmployeeMapper22Impl().toEmployee(listOf("John"), listOf(30))

        with(res) {
            empNames shouldBe listOf("John")
            empAges shouldBe listOf(30)
        }
    }
}
