package com.github.mfarsikov.kewt.processor.render

import com.github.mfarsikov.kewt.processor.ConversionFunction
import com.github.mfarsikov.kewt.processor.Parameter
import com.github.mfarsikov.kewt.processor.Type
import com.github.mfarsikov.kewt.processor.mapper.ConversionContext
import io.kotest.matchers.shouldBe
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class RendererKtTest {
    val STRING = Type("kotlin", "String")
    val INT = Type("kotlin", "Int")

    @Test
    fun `should render`() {
        val res = render(
                RenderConverterClass(
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
                                ),
                                targetParameterName = null
                        )),
                        springComponent = false
                ),
                version = "v-1.0.0-SNAPSHOT",
                date = OffsetDateTime.parse("2020-12-31T23:59:59Z")
        )

        @Language("kotlin")
        val l = """
             package mypckg

             import com.employeePackage.Employee
             import com.personPackage.Person
             import javax.annotation.Generated
             
             @Generated(
               value = ["com.github.mfarsikov.kewt.processor.KewtMapperProcessor"],
               date = "2020-12-31T23:59:59Z",
               comments = "v-1.0.0-SNAPSHOT"
             )
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

        val res = render(
                RenderConverterClass(
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
                                        ),
                                        targetParameterName = null
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
                                        ),
                                        targetParameterName = null
                                )
                        ),
                        springComponent = false
                ),
                version = "v-1.0.0-SNAPSHOT",
                date = OffsetDateTime.parse("2020-12-31T23:59:59Z")
        )


        @Language("kotlin")
        val l = """
             package mypckg

             import com.employeePackage.Employee
             import com.personPackage.Person
             import javax.annotation.Generated
             
             @Generated(
               value = ["com.github.mfarsikov.kewt.processor.KewtMapperProcessor"],
               date = "2020-12-31T23:59:59Z",
               comments = "v-1.0.0-SNAPSHOT"
             )
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
        val res = render(
                RenderConverterClass(
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
                                ),
                                targetParameterName = null
                        )),
                        springComponent = false
                ),
                version = "v-1.0.0-SNAPSHOT",
                date = OffsetDateTime.parse("2020-12-31T23:59:59Z")
        )

        @Language("kotlin")
        val l = """
             package mypckg

             import com.employeePackage.Employee
             import com.personPackage.Person
             import javax.annotation.Generated
             
             @Generated(
               value = ["com.github.mfarsikov.kewt.processor.KewtMapperProcessor"],
               date = "2020-12-31T23:59:59Z",
               comments = "v-1.0.0-SNAPSHOT"
             )
             class MyConveretrImpl : MyConveretr {
               override fun convert(person: Person): Employee = Employee(
                   ids = person.ids.map { f(it) }
               )}

         """.trimIndent()

        res shouldBe l
    }

    @Test
    fun `render component annotation`() {
        val res = render(
                RenderConverterClass(
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
                                ),
                                targetParameterName = null
                        )),
                        springComponent = true
                ),
                version = "v-1.0.0-SNAPSHOT",
                date = OffsetDateTime.parse("2020-12-31T23:59:59Z")
        )

        @Language("kotlin")
        val l = """
             package mypckg

             import com.employeePackage.Employee
             import com.personPackage.Person
             import javax.annotation.Generated
             import org.springframework.stereotype.Component

             @Generated(
               value = ["com.github.mfarsikov.kewt.processor.KewtMapperProcessor"],
               date = "2020-12-31T23:59:59Z",
               comments = "v-1.0.0-SNAPSHOT"
             )
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
        val res = render(
                RenderConverterClass(
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
                                ),
                                targetParameterName = null
                        )),
                        springComponent = false
                ),
                version = "v-1.0.0-SNAPSHOT",
                date = OffsetDateTime.parse("2020-12-31T23:59:59Z")
        )

        @Language("kotlin")
        val l = """
             package mypckg

             import com.employeePackage.Employee
             import com.personPackage.Person
             import javax.annotation.Generated
             
             @Generated(
               value = ["com.github.mfarsikov.kewt.processor.KewtMapperProcessor"],
               date = "2020-12-31T23:59:59Z",
               comments = "v-1.0.0-SNAPSHOT"
             )
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


    @Test
    fun `render target parameter mapping`() {

        val res = render(
                RenderConverterClass(
                        type = Type(packageName = "mypckg", name = "MyConveretr"),
                        converterFunctions = listOf(
                                RenderConverterFunction(
                                        name = "convert1",
                                        returnTypeLanguage = com.github.mfarsikov.kewt.processor.mapper.Language.KOTLIN,
                                        parameters = listOf(
                                                Parameter(
                                                        name = "sourcePerson",
                                                        type = Type(
                                                                name = "Person",
                                                                packageName = "com.personPackage"
                                                        )
                                                ),
                                                Parameter(
                                                        name = "targetPerson",
                                                        type = Type(
                                                                name = "Person",
                                                                packageName = "com.personPackage"
                                                        )
                                                )
                                        ),
                                        returnType = Type(
                                                name = "Person",
                                                packageName = "com.personPackage"
                                        ),
                                        mappings = listOf(
                                                propertyMapping(
                                                        parameterName = "sourcePerson",
                                                        sourceProperty = "firstName",
                                                        targetProperty = "firstName"
                                                ),
                                                propertyMapping(
                                                        parameterName = "sourcePerson",
                                                        sourceProperty = "lastName",
                                                        targetProperty = "lastName"
                                                )
                                        ),
                                        targetParameterName = "targetPerson"
                                )
                        ),
                        springComponent = false
                ),
                version = "v-1.0.0-SNAPSHOT",
                date = OffsetDateTime.parse("2020-12-31T23:59:59Z")
        )


        @Language("kotlin")
        val l = """
             package mypckg

             import com.personPackage.Person
             import javax.annotation.Generated
             
             @Generated(
               value = ["com.github.mfarsikov.kewt.processor.KewtMapperProcessor"],
               date = "2020-12-31T23:59:59Z",
               comments = "v-1.0.0-SNAPSHOT"
             )
             class MyConveretrImpl : MyConveretr {
               override fun convert1(sourcePerson: Person, targetPerson: Person): Person = Person(
                   firstName = sourcePerson.firstName ?: targetPerson.firstName,
                   lastName = sourcePerson.lastName ?: targetPerson.lastName
               )}

         """.trimIndent()

        res shouldBe l
    }
}