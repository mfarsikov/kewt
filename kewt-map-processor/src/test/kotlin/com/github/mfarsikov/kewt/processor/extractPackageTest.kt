package com.github.mfarsikov.kewt.processor

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ExtractPackageTest {
    @Test
    fun `extract package`(){
        "MyClass".extractPackage() shouldBe ""
        "com.my.company.MyClass".extractPackage() shouldBe "com.my.company"
        "com.my.company.MyClass<kotlin.Int, kotlin.String>".extractPackage() shouldBe "com.my.company"
        "com.my.company.MyClass<in kotlin.Int, out kotlin.String>".extractPackage() shouldBe "com.my.company"
    }
    @Test
    fun `extract class name`(){
        "MyClass".extractClassName() shouldBe "MyClass"
        "com.my.company.MyClass".extractClassName() shouldBe "MyClass"
        "com.my.company.MyClass<kotlin.Int, kotlin.String>".extractClassName() shouldBe "MyClass"
        "com.my.company.MyClass<in kotlin.Int, out kotlin.String>".extractClassName() shouldBe "MyClass"
    }
}