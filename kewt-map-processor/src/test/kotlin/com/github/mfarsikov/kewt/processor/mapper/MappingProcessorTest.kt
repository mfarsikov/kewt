package com.github.mfarsikov.kewt.processor.mapper

import com.github.mfarsikov.kewt.processor.AmbiguousMappingException
import com.github.mfarsikov.kewt.processor.ConversionFunction
import com.github.mfarsikov.kewt.processor.ExplicitConverter
import com.github.mfarsikov.kewt.processor.KewtException
import com.github.mfarsikov.kewt.processor.NameMapping
import com.github.mfarsikov.kewt.processor.Parameter
import com.github.mfarsikov.kewt.processor.Type
import com.github.mfarsikov.kewt.processor.mapper.Language.KOTLIN
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError

class MappingProcessorTest {

    val INT = Type("kotlin", "Int")
    val STRING = Type("kotlin", "String")

    @Test
    fun `map property by name and type`() {
        val res = calculateMappings(
                sources = listOf(ResolvedParameter(
                        name = "person",
                        resolvedType = ResolvedType(
                                type = Type(packageName = "com.person.hub", name = "Person"),
                                properties = setOf(
                                        Parameter(name = "name", type = STRING)
                                ),
                                language = KOTLIN
                        )
                )),
                target = ResolvedType(
                        type = Type(packageName = "com.employee.hub", name = "Employee"),
                        properties = setOf(
                                Parameter(name = "name", type = STRING)
                        ),
                        language = KOTLIN
                ),
                nameMappings = emptyList(),
                conversionFunctions = emptyList()
        )

        res.mappings.size shouldBe 1

        with(res.mappings.first()) {
            parameterName shouldBe "person"
            sourceProperty.name shouldBe "name"
            targetProperty.name shouldBe "name"
        }
    }

    @Test
    fun `lift property`() {
        val res = calculateMappings(
                sources = listOf(ResolvedParameter(
                        name = "person",
                        resolvedType = ResolvedType(
                                type = Type(packageName = "com.person.hub", name = "Person"),
                                properties = setOf(
                                        Parameter(name = "name.firstName", type = STRING)
                                ),
                                language = KOTLIN
                        )
                )),
                target = ResolvedType(
                        type = Type(packageName = "com.employee.hub", name = "Employee"),
                        properties = setOf(
                                Parameter(name = "name", type = STRING)
                        ),
                        language = KOTLIN
                ),
                nameMappings = listOf(NameMapping(parameterName = "person", sourcePath = "name.firstName", targetParameterName = "name")),
                conversionFunctions = emptyList()
        )

        res.mappings.size shouldBe 1

        with(res.mappings.first()) {
            parameterName shouldBe "person"
            sourceProperty.name shouldBe "name.firstName"
            targetProperty.name shouldBe "name"
        }
    }

    @Test
    fun `cannot map property if type is different`() {
        shouldThrow<KewtException> {
            calculateMappings(
                    sources = listOf(ResolvedParameter(
                            name = "person",
                            resolvedType = ResolvedType(
                                    type = Type(packageName = "com.person.hub", name = "Person"),
                                    properties = setOf(
                                            Parameter(name = "name", type = Type("my", "String"))
                                    ),
                                    language = KOTLIN
                            )
                    )),
                    target = ResolvedType(
                            type = Type(packageName = "com.employee.hub", name = "Employee"),
                            properties = setOf(
                                    Parameter(name = "name", type = STRING)
                            ),
                            language = KOTLIN
                    ),
                    nameMappings = emptyList(),
                    conversionFunctions = emptyList()
            )
        }
    }

    @Test
    fun `map property by with renaming`() {
        val res = calculateMappings(
                sources = listOf(ResolvedParameter(
                        name = "person",
                        resolvedType = ResolvedType(
                                type = Type(packageName = "com.person.hub", name = "Person"),
                                properties = setOf(
                                        Parameter(name = "name", type = STRING)
                                ),
                                language = KOTLIN
                        )
                )),
                target = ResolvedType(
                        type = Type(packageName = "com.employee.hub", name = "Employee"),
                        properties = setOf(
                                Parameter(name = "lastName", type = STRING)
                        ),
                        language = KOTLIN
                ),
                nameMappings = listOf(NameMapping(parameterName = "person", sourcePath = "name", targetParameterName = "lastName")),
                conversionFunctions = emptyList()
        )

        res.mappings.size shouldBe 1
        with(res.mappings.first()) {
            parameterName shouldBe "person"
            sourceProperty.name shouldBe "name"
            targetProperty.name shouldBe "lastName"
        }
    }

    @Test
    fun `map by type if not ambiuous`() {
        val res = calculateMappings(
                sources = listOf(ResolvedParameter(
                        name = "person",
                        resolvedType = ResolvedType(
                                type = Type(packageName = "com.person.hub", name = "Person"),
                                properties = setOf(
                                        Parameter(name = "name", type = STRING)
                                ),
                                language = KOTLIN
                        )
                )),
                target = ResolvedType(
                        type = Type(packageName = "com.employee.hub", name = "Employee"),
                        properties = setOf(
                                Parameter(name = "lastName", type = STRING)
                        ),
                        language = KOTLIN
                ),
                nameMappings = emptyList(),
                conversionFunctions = emptyList()
        )

        res.mappings.size shouldBe 1

        with(res.mappings.first()) {
            parameterName shouldBe "person"
            sourceProperty.name shouldBe "name"
            targetProperty.name shouldBe "lastName"
        }
    }

    @Test
    fun `cannot map by type if type is ambiguous`() {
        shouldThrow<AmbiguousMappingException> {
            calculateMappings(
                    sources = listOf(ResolvedParameter(
                            name = "person",
                            resolvedType = ResolvedType(
                                    type = Type(packageName = "com.person.hub", name = "Person"),
                                    properties = setOf(
                                            Parameter(name = "name", type = STRING),
                                            Parameter(name = "surname", type = STRING)
                                    ),
                                    language = KOTLIN
                            )
                    )),
                    target = ResolvedType(
                            type = Type(packageName = "com.employee.hub", name = "Employee"),
                            properties = setOf(
                                    Parameter(name = "firstName", type = STRING),
                                    Parameter(name = "lastName", type = STRING)
                            ),
                            language = KOTLIN
                    ),
                    nameMappings = emptyList(),
                    conversionFunctions = emptyList()
            )
        }
    }

    @Test
    fun `solve type ambiguity using name match`() {
        val res = calculateMappings(
                sources = listOf(ResolvedParameter(
                        name = "person",
                        resolvedType = ResolvedType(
                                type = Type(packageName = "com.person.hub", name = "Person"),
                                properties = setOf(
                                        Parameter(name = "firstName", type = STRING),
                                        Parameter(name = "surname", type = STRING)
                                ),
                                language = KOTLIN
                        )
                )),
                target = ResolvedType(
                        type = Type(packageName = "com.employee.hub", name = "Employee"),
                        properties = setOf(
                                Parameter(name = "firstName", type = STRING),
                                Parameter(name = "lastName", type = STRING)
                        ),
                        language = KOTLIN
                ),
                nameMappings = emptyList(),
                conversionFunctions = emptyList()
        )
        res.mappings.size shouldBe 2


        res.mappings.any { it.sourceProperty.name == "firstName" && it.targetProperty.name == "firstName" } shouldBe true
        res.mappings.any { it.sourceProperty.name == "surname" && it.targetProperty.name == "lastName" } shouldBe true
    }

    @Test
    fun `map collections`() {
        val res = calculateMappings(
                sources = listOf(ResolvedParameter(
                        name = "person",
                        resolvedType = ResolvedType(
                                type = Type(packageName = "com.person.hub", name = "Person"),
                                properties = setOf(
                                        Parameter(name = "keys", type = Type("kotlin.collections", "List", typeParameters = listOf(INT)))
                                ),
                                language = KOTLIN
                        )
                )),
                target = ResolvedType(
                        type = Type(packageName = "com.employee.hub", name = "Employee"),
                        properties = setOf(
                                Parameter(name = "keys", type = Type("kotlin.collections", "List", typeParameters = listOf(INT)))
                        ),
                        language = KOTLIN
                ),
                nameMappings = emptyList(),
                conversionFunctions = emptyList()
        )

        res.mappings.size shouldBe 1

    }

    @Test
    fun `do not map if collection's type parameters are different`() {
        shouldThrow<KewtException> {
            calculateMappings(
                    sources = listOf(ResolvedParameter(
                            name = "person",
                            resolvedType = ResolvedType(
                                    type = Type(packageName = "com.person.hub", name = "Person"),
                                    properties = setOf(
                                            Parameter(name = "keys", type = Type("kotlin.collections", "List", typeParameters = listOf(STRING)))
                                    ),
                                    language = KOTLIN
                            )
                    )),
                    target = ResolvedType(
                            type = Type(packageName = "com.employee.hub", name = "Employee"),
                            properties = setOf(
                                    Parameter(name = "keys", type = Type("kotlin.collections", "List", typeParameters = listOf(INT)))
                            ),
                            language = KOTLIN
                    ),
                    nameMappings = emptyList(),
                    conversionFunctions = emptyList()
            )
        }
    }

    @Test
    fun `map using name match and type conversion`() {
        val res = calculateMappings(
                sources = listOf(ResolvedParameter(
                        name = "person",
                        resolvedType = ResolvedType(
                                type = Type(packageName = "com.person.hub", name = "Person"),
                                properties = setOf(
                                        Parameter(name = "id", type = INT)
                                ),
                                language = KOTLIN
                        )
                )),
                target = ResolvedType(
                        type = Type(packageName = "com.employee.hub", name = "Employee"),
                        properties = setOf(
                                Parameter(name = "id", type = STRING)
                        ),
                        language = KOTLIN
                ),
                nameMappings = emptyList(),
                conversionFunctions = listOf(ConversionFunction(
                        name = "f",
                        parameter = Parameter("x", type = INT),
                        returnType = STRING
                ))
        )

        res.mappings.size shouldBe 1

        with(res.mappings.single()) {
            sourceProperty.name shouldBe "id"
            targetProperty.name shouldBe "id"
            conversionContext?.conversionFunction?.name shouldBe "f"
        }
    }

    @Test
    fun `map using type conversion`() {
        val res = calculateMappings(
                sources = listOf(ResolvedParameter(
                        name = "person",
                        resolvedType = ResolvedType(
                                type = Type(packageName = "com.person.hub", name = "Person"),
                                properties = setOf(
                                        Parameter(name = "id", type = INT)
                                ),
                                language = KOTLIN
                        )
                )),
                target = ResolvedType(
                        type = Type(packageName = "com.employee.hub", name = "Employee"),
                        properties = setOf(
                                Parameter(name = "uuid", type = STRING)
                        ),
                        language = KOTLIN
                ),
                nameMappings = emptyList(),
                conversionFunctions = listOf(ConversionFunction(
                        name = "f",
                        parameter = Parameter("x", type = INT),
                        returnType = STRING
                ))
        )

        res.mappings.size shouldBe 1

        with(res.mappings.single()) {
            sourceProperty.name shouldBe "id"
            targetProperty.name shouldBe "uuid"
            conversionContext?.conversionFunction?.name shouldBe "f"
        }
    }

    @Test
    fun `map collection elements using type conversion`() {
        val res = calculateMappings(
                sources = listOf(ResolvedParameter(
                        name = "person",
                        resolvedType = ResolvedType(
                                type = Type(packageName = "com.person.hub", name = "Person"),
                                properties = setOf(
                                        Parameter(name = "ids", type = Type("kotlin.collections", "List", typeParameters = listOf(INT)))
                                ),
                                language = KOTLIN
                        )
                )),
                target = ResolvedType(
                        type = Type(packageName = "com.employee.hub", name = "Employee"),
                        properties = setOf(
                                Parameter(name = "ids", type = Type("kotlin.collections", "List", typeParameters = listOf(STRING)))

                        ),
                        language = KOTLIN
                ),
                nameMappings = emptyList(),
                conversionFunctions = listOf(ConversionFunction(
                        name = "f",
                        parameter = Parameter("x", type = INT),
                        returnType = STRING
                ))
        )

        res.mappings.size shouldBe 1

        with(res.mappings.single()) {
            sourceProperty.name shouldBe "ids"
            targetProperty.name shouldBe "ids"
            conversionContext?.conversionFunction?.name shouldBe "f"
            conversionContext?.usingElementMapping shouldBe true
        }
    }

    @Test
    fun `property mapping could contain parameter name`() {
        val res = calculateMappings(
                sources = listOf(ResolvedParameter(
                        name = "person",
                        resolvedType = ResolvedType(
                                type = Type(packageName = "com.person.hub", name = "Person"),
                                properties = setOf(
                                        Parameter(name = "name", type = STRING),
                                        Parameter(name = "surname", type = STRING)
                                ),
                                language = KOTLIN
                        )
                )),
                target = ResolvedType(
                        type = Type(packageName = "com.employee.hub", name = "Employee"),
                        properties = setOf(
                                Parameter(name = "lastName", type = STRING),
                                Parameter(name = "firstName", type = STRING)
                        ),
                        language = KOTLIN
                ),
                nameMappings = listOf(
                        NameMapping(parameterName = "person", sourcePath = "name", targetParameterName = "firstName"),
                        NameMapping(parameterName = "person", sourcePath = "surname", targetParameterName = "lastName")
                ),
                conversionFunctions = emptyList()
        )

        with(res) {
            mappings.size shouldBe 2

            mappings shouldHaveAtLeastOne {
                parameterName shouldBe "person"
                sourceProperty.name shouldBe "name"
                targetProperty.name shouldBe "firstName"
            }
            mappings shouldHaveAtLeastOne {
                parameterName shouldBe "person"
                sourceProperty.name shouldBe "surname"
                targetProperty.name shouldBe "lastName"
            }
        }
    }

    //TODO move to utils
    infix fun <E> Collection<E>.shouldHaveAtLeastOne(block: E.() -> Unit) {
        val mappedErrors = map {
            try {
                it.block()
                null
            } catch (ex: AssertionFailedError) {
                ex
            }
        }
        if (mappedErrors.any { it == null }) return
        else throw AssertionFailedError(mappedErrors.mapNotNull { it?.message }.toString())
    }

    @Test
    fun `fail if explicitly mapped property does not exist`() {
        shouldThrow<KewtException> {
            calculateMappings(
                    sources = listOf(ResolvedParameter(
                            name = "person",
                            resolvedType = ResolvedType(
                                    type = Type(packageName = "com.person.hub", name = "Person"),
                                    properties = setOf(
                                            Parameter(name = "name", type = STRING)
                                    ),
                                    language = KOTLIN
                            )
                    )),
                    target = ResolvedType(
                            type = Type(packageName = "com.employee.hub", name = "Employee"),
                            properties = setOf(
                                    Parameter(name = "lastName", type = STRING)
                            ),
                            language = KOTLIN
                    ),
                    nameMappings = listOf(NameMapping(parameterName = "person", sourcePath = "nameX", targetParameterName = "lastName")),
                    conversionFunctions = emptyList()
            )
        }.message shouldContain "Not existing sources"
    }

    @Test
    fun `merge two sources`() {
        val res = calculateMappings(
                sources = listOf(
                        ResolvedParameter(
                                name = "person",
                                resolvedType = ResolvedType(
                                        type = Type(packageName = "com.person.hub", name = "Person"),
                                        properties = setOf(Parameter(name = "name", type = STRING)),
                                        language = KOTLIN
                                )
                        ),
                        ResolvedParameter(
                                name = "pet",
                                resolvedType = ResolvedType(
                                        type = Type(packageName = "com.person.hub", name = "Pet"),
                                        properties = setOf(Parameter(name = "name", type = STRING)),
                                        language = KOTLIN
                                )
                        )
                ),
                target = ResolvedType(
                        type = Type(packageName = "com.employee.hub", name = "Employee"),
                        properties = setOf(
                                Parameter(name = "name", type = STRING),
                                Parameter(name = "petName", type = STRING)
                        ),
                        language = KOTLIN
                ),
                nameMappings = listOf(
                        NameMapping(parameterName = "pet", sourcePath = "name", targetParameterName = "petName"),
                        NameMapping(parameterName = "person", sourcePath = "name", targetParameterName = "name")
                ),
                conversionFunctions = listOf()
        )

        with(res) {
            mappings.size shouldBe 2

            mappings shouldHaveAtLeastOne {
                parameterName shouldBe "pet"
                sourceProperty.name shouldBe "name"
                targetProperty.name shouldBe "petName"
            }
            mappings shouldHaveAtLeastOne {
                parameterName shouldBe "person"
                sourceProperty.name shouldBe "name"
                targetProperty.name shouldBe "name"
            }
        }
    }


    @Test
    fun `merge two sources, infer implicit matching`() {
        val res = calculateMappings(
                sources = listOf(
                        ResolvedParameter(
                                name = "person",
                                resolvedType = ResolvedType(
                                        type = Type(packageName = "com.person.hub", name = "Person"),
                                        properties = setOf(Parameter(name = "name", type = STRING)),
                                        language = KOTLIN
                                )
                        ),
                        ResolvedParameter(
                                name = "pet",
                                resolvedType = ResolvedType(
                                        type = Type(packageName = "com.person.hub", name = "Pet"),
                                        properties = setOf(Parameter(name = "name", type = STRING)),
                                        language = KOTLIN
                                )
                        )
                ),
                target = ResolvedType(
                        type = Type(packageName = "com.employee.hub", name = "Employee"),
                        properties = setOf(
                                Parameter(name = "name", type = STRING),
                                Parameter(name = "petName", type = STRING)
                        ),
                        language = KOTLIN
                ),
                nameMappings = listOf(NameMapping(parameterName = "pet", sourcePath = "name", targetParameterName = "petName")),
                conversionFunctions = listOf()
        )

        with(res) {
            mappings.size shouldBe 2

            mappings shouldHaveAtLeastOne {
                parameterName shouldBe "pet"
                sourceProperty.name shouldBe "name"
                targetProperty.name shouldBe "petName"
            }
            mappings shouldHaveAtLeastOne {
                parameterName shouldBe "person"
                sourceProperty.name shouldBe "name"
                targetProperty.name shouldBe "name"
            }
        }
    }


    @Test
    fun `solve if two sources have ambiguous name mapping but explicit mapping is present`() {
        val res = calculateMappings(
                sources = listOf(
                        ResolvedParameter(
                                name = "person",
                                resolvedType = ResolvedType(
                                        type = Type(packageName = "com.person.hub", name = "Person"),
                                        properties = setOf(Parameter(name = "name", type = STRING)),
                                        language = KOTLIN

                                )
                        ),
                        ResolvedParameter(
                                name = "pet",
                                resolvedType = ResolvedType(
                                        type = Type(packageName = "com.person.hub", name = "Pet"),
                                        properties = setOf(Parameter(name = "name", type = STRING)),
                                        language = KOTLIN
                                )
                        )
                ),
                target = ResolvedType(
                        type = Type(packageName = "com.employee.hub", name = "Employee"),
                        properties = setOf(
                                Parameter(name = "name", type = STRING),
                                Parameter(name = "petName", type = STRING)
                        ),
                        language = KOTLIN

                ),
                nameMappings = listOf(NameMapping(parameterName = "pet", sourcePath = "name", targetParameterName = "petName")),
                conversionFunctions = listOf()
        )

        with(res) {
            mappings.size shouldBe 2
            mappings shouldHaveAtLeastOne {
                parameterName shouldBe "pet"
                sourceProperty.name shouldBe "name"
                targetProperty.name shouldBe "petName"
            }
            mappings shouldHaveAtLeastOne {
                parameterName shouldBe "person"
                sourceProperty.name shouldBe "name"
                targetProperty.name shouldBe "name"
            }
        }
    }

    @Test
    fun `use explicit function if there are more than one function is applicable`() {
        val res = calculateMappings(
                sources = listOf(
                        ResolvedParameter(
                                name = "person",
                                resolvedType = ResolvedType(
                                        type = Type(packageName = "com.person.hub", name = "Person"),
                                        properties = setOf(Parameter(name = "id", type = STRING)),
                                        language = KOTLIN
                                )
                        )
                ),
                target = ResolvedType(
                        type = Type(packageName = "com.employee.hub", name = "Employee"),
                        properties = setOf(Parameter(name = "id", type = INT)),
                        language = KOTLIN
                ),
                nameMappings = listOf(NameMapping(parameterName = "person", sourcePath = "id", targetParameterName = "id")),
                conversionFunctions = listOf(
                        ConversionFunction("f", Parameter("x", STRING), INT),
                        ConversionFunction("g", Parameter("x", STRING), INT)
                ),
                explicitConverters = listOf(ExplicitConverter("id", "f"))
        )
        res.mappings.single().conversionContext!!.conversionFunction!!.name shouldBe "f"
    }
}

fun calculateMappings(
        sources: List<ResolvedParameter>,
        target: ResolvedType,
        nameMappings: List<NameMapping>,
        conversionFunctions: List<ConversionFunction>,
        explicitConverters:List<ExplicitConverter> = emptyList()
) =
        calculateMappings(ReadyForMappingFunction(
                name = "f",
                parameters = sources,
                returnType = target,
                nameMappings = nameMappings,
                explicitConverters = explicitConverters
        ),
                conversionFunctions
        )