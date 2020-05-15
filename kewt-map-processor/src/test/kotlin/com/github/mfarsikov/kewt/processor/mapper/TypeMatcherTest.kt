package com.github.mfarsikov.kewt.processor.mapper

import com.github.mfarsikov.kewt.processor.ConversionFunction
import com.github.mfarsikov.kewt.processor.Nullability
import com.github.mfarsikov.kewt.processor.Nullability.NON_NULLABLE
import com.github.mfarsikov.kewt.processor.Nullability.NULLABLE
import com.github.mfarsikov.kewt.processor.Parameter
import com.github.mfarsikov.kewt.processor.Type
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class TypeMatcherTest {

    fun int(nullability: Nullability = NON_NULLABLE) = Type("kotlin", "Int", nullability = nullability)
    fun string(nullability: Nullability = NON_NULLABLE) = Type("kotlin", "String", nullability = nullability)
    fun list(of: Type) = Type("kotlin.collections", "List", typeParameters = listOf(of))

    @Test
    fun `two lists not match if type parameters are not match`() {
        TypeMatcher(emptyList()).findConversion(
                list(of = int()),
                list(of = string())
        ) shouldBe null
    }

    @Test
    fun `nullable type could be mapped by non-nullable function using null-safe call`() {
        val res = f(from = int(NULLABLE), to = string(NULLABLE), funcParam = int(), returnType = string())

        res shouldNotBe null
        with(res!!) {
            conversionFunction?.name shouldBe "f"
            usingNullSafeCall shouldBe true
        }
    }

    private fun f(from: Type, to: Type, funcParam: Type, returnType: Type): MappingConversionContext? = TypeMatcher(listOf(MapperConversionFunction(
            name = "f",
            parameter = Parameter(name = "x", type = funcParam),
            returnType = returnType
    ))).findConversion(
            from,
            to
    )

}
