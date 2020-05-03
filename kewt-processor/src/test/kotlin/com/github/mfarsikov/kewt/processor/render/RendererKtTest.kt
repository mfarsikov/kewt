package com.github.mfarsikov.kewt.processor.render

import com.github.mfarsikov.kewt.processor.ConversionFunction
import com.github.mfarsikov.kewt.processor.Parameter
import com.github.mfarsikov.kewt.processor.Type
import com.github.mfarsikov.kewt.processor.mapper.ConversionContext
import io.kotest.matchers.shouldBe
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class RendererKtTest {
    val STRING = Type("kotlin", "String")
    val INT = Type("kotlin", "Int")

    @Test
    fun `should render`() {
        val res = render(RenderConverterClass(
                type = Type(packageName = "mypckg", name = "MyConveretr"),
                converterFunctions = listOf(RenderConverterFunction(
                        name = "doconvert",
                        returnTypeLanguage = com.github.mfarsikov.kewt.processor.mapper.Language.KOTLIN,
                        parameters = listOf(Parameter(
                                name = "person",
                                type = Type(
                                        name = "Person",
                                        packageName = "com.personPackage"
                                )
                        )),
                        returnType = Type(
                                name = "Employee",
                                packageName = "com.employeePackage"
                        ),
                        mappings = listOf(
                                propertyMapping(
                                        parameterName = "person",
                                        sourceProperty = "firstName",
                                        targetProperty = "firstName"
                                ),
                                propertyMapping(
                                        parameterName = "person",
                                        sourceProperty = "lastName",
                                        targetProperty = "lastName",
                                        conversionFunction = "convert"
                                )
                        )
                )),
                springComponent = false
        ))

        @Language("kotlin")
        val l = """
             package mypckg

             import com.employeePackage.Employee
             import com.personPackage.Person
             
             class MyConveretrImpl : MyConveretr {
               override fun doconvert(person: Person): Employee = Employee(
                   firstName = person.firstName,
                   lastName = convert(person.lastName)
               )}

         """.trimIndent()

        res shouldBe l
    }

    private fun propertyMapping(
            parameterName: String,
            sourceProperty: String,
            targetProperty: String,
            conversionFunction: String? = null,
            usingElementMapping: Boolean = false
    ) = RenderPropertyMappings(
            parameterName = parameterName,
            sourcePropertyName = sourceProperty,
            targetPropertyName = targetProperty,
            conversionContext = conversionFunction?.let {
                ConversionContext(
                        conversionFunction = ConversionFunction(it, Parameter("", INT), returnType = INT),
                        usingElementMapping = usingElementMapping
                )
            }
                    ?: ConversionContext()
    )


    @Test
    fun `should render2`() {
        val res = render(RenderConverterClass(
                type = Type(packageName = "mypckg", name = "MyConveretr"),
                converterFunctions = listOf(
                        RenderConverterFunction(
                                name = "convert1",
                                returnTypeLanguage = com.github.mfarsikov.kewt.processor.mapper.Language.KOTLIN,
                                parameters = listOf(Parameter(
                                        name = "person",
                                        type = Type(
                                                name = "Person",
                                                packageName = "com.personPackage"
                                        )
                                )),
                                returnType = Type(
                                        name = "Employee",
                                        packageName = "com.employeePackage"
                                ),
                                mappings = listOf(
                                        propertyMapping(
                                                parameterName = "person",
                                                sourceProperty = "firstName",
                                                targetProperty = "firstName"
                                        ),
                                        propertyMapping(
                                                parameterName = "person",
                                                sourceProperty = "lastName",
                                                targetProperty = "lastName",
                                                conversionFunction = "convert"
                                        )
                                )
                        ),
                        RenderConverterFunction(
                                name = "convert2",
                                returnTypeLanguage = com.github.mfarsikov.kewt.processor.mapper.Language.KOTLIN,
                                parameters = listOf(Parameter(
                                        name = "person",
                                        type = Type(name = "Person", packageName = "com.personPackage")
                                )),
                                returnType = Type(name = "Employee", packageName = "com.employeePackage"),
                                mappings = listOf(
                                        propertyMapping(
                                                parameterName = "person",
                                                sourceProperty = "firstName",
                                                targetProperty = "firstName"
                                        ),
                                        propertyMapping(
                                                parameterName = "person",
                                                sourceProperty = "lastName",
                                                targetProperty = "lastName",
                                                conversionFunction = "convert"
                                        )
                                )
                        )
                ),
                springComponent = false
        ))


        @Language("kotlin")
        val l = """
             package mypckg

             import com.employeePackage.Employee
             import com.personPackage.Person
             
             class MyConveretrImpl : MyConveretr {
               override fun convert1(person: Person): Employee = Employee(
                   firstName = person.firstName,
                   lastName = convert(person.lastName)
               )
               override fun convert2(person: Person): Employee = Employee(
                   firstName = person.firstName,
                   lastName = convert(person.lastName)
               )}

         """.trimIndent()

        res shouldBe l
    }

    @Test
    fun `render collection mapping`() {
        val res = render(RenderConverterClass(
                type = Type(packageName = "mypckg", name = "MyConveretr"),
                converterFunctions = listOf(RenderConverterFunction(
                        name = "convert",
                        returnTypeLanguage = com.github.mfarsikov.kewt.processor.mapper.Language.KOTLIN,
                        parameters = listOf(Parameter(
                                name = "person",
                                type = Type(
                                        name = "Person",
                                        packageName = "com.personPackage"
                                )
                        )),
                        returnType = Type(
                                name = "Employee",
                                packageName = "com.employeePackage"
                        ),
                        mappings = listOf(
                                propertyMapping(
                                        parameterName = "person",
                                        sourceProperty = "ids",
                                        targetProperty = "ids",
                                        conversionFunction = "f",
                                        usingElementMapping = true
                                )
                        )
                )),
                springComponent = false
        ))

        @Language("kotlin")
        val l = """
             package mypckg

             import com.employeePackage.Employee
             import com.personPackage.Person
             
             class MyConveretrImpl : MyConveretr {
               override fun convert(person: Person): Employee = Employee(
                   ids = person.ids.map { f(it) }
               )}

         """.trimIndent()

        res shouldBe l
    }

    @Test
    fun `render component annotation`() {
        val res = render(RenderConverterClass(
                type = Type(packageName = "mypckg", name = "MyConveretr"),
                converterFunctions = listOf(RenderConverterFunction(
                        name = "doconvert",
                        returnTypeLanguage = com.github.mfarsikov.kewt.processor.mapper.Language.KOTLIN,
                        parameters = listOf(Parameter(
                                name = "person",
                                type = Type(
                                        name = "Person",
                                        packageName = "com.personPackage"
                                )
                        )),
                        returnType = Type(
                                name = "Employee",
                                packageName = "com.employeePackage"
                        ),
                        mappings = listOf(
                                propertyMapping(
                                        parameterName = "person",
                                        sourceProperty = "firstName",
                                        targetProperty = "firstName"
                                ),
                                propertyMapping(
                                        parameterName = "person",
                                        sourceProperty = "lastName",
                                        targetProperty = "lastName",
                                        conversionFunction = "convert"
                                )
                        )
                )),
                springComponent = true
        ))

        @Language("kotlin")
        val l = """
             package mypckg

             import com.employeePackage.Employee
             import com.personPackage.Person
             import org.springframework.stereotype.Component
             
             @Component
             class MyConveretrImpl : MyConveretr {
               override fun doconvert(person: Person): Employee = Employee(
                   firstName = person.firstName,
                   lastName = convert(person.lastName)
               )}

         """.trimIndent()

        res shouldBe l
    }

    @Test
    fun `render protobuf`() {
        val res = render(RenderConverterClass(
                type = Type(packageName = "mypckg", name = "MyConveretr"),
                converterFunctions = listOf(RenderConverterFunction(
                        name = "doconvert",
                        returnTypeLanguage = com.github.mfarsikov.kewt.processor.mapper.Language.PROTO,
                        parameters = listOf(Parameter(
                                name = "person",
                                type = Type(
                                        name = "Person",
                                        packageName = "com.personPackage"
                                )
                        )),
                        returnType = Type(
                                name = "Employee",
                                packageName = "com.employeePackage"
                        ),
                        mappings = listOf(
                                propertyMapping(
                                        parameterName = "person",
                                        sourceProperty = "firstName",
                                        targetProperty = "firstName"
                                ),
                                propertyMapping(
                                        parameterName = "person",
                                        sourceProperty = "lastName",
                                        targetProperty = "lastName"
                                )
                        )
                )),
                springComponent = false
        ))

        @Language("kotlin")
        val l = """
             package mypckg

             import com.employeePackage.Employee
             import com.personPackage.Person
             
             class MyConveretrImpl : MyConveretr {
               override fun doconvert(person: Person): Employee {
                 @Suppress("UNNECESSARY_SAFE_CALL")return Employee.newBuilder().apply {
                     person.firstName?.also { firstName = it }
                     person.lastName?.also { lastName = it }
                 }
                 .build()
               }
             }

         """.trimIndent()

        res shouldBe l
    }
}