package com.github.mfarsikov.kewt.processor.mapper

import com.github.mfarsikov.kewt.processor.AmbiguousMappingException
import com.github.mfarsikov.kewt.processor.ExplicitConverter
import com.github.mfarsikov.kewt.processor.KewtException
import com.github.mfarsikov.kewt.processor.NameMapping
import com.github.mfarsikov.kewt.processor.Nullability
import com.github.mfarsikov.kewt.processor.Parameter
import com.github.mfarsikov.kewt.processor.Type
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
        val res = calculateMappings1(
                sources = listOf(Source(parameterName = "person", path = listOf("name"), type = STRING)),
                targets = listOf(Parameter(name = "name", type = STRING))
        )

        res.size shouldBe 1

        with(res.first()) {
            source.parameterName shouldBe "person"
            source.path.first() shouldBe "name"
            target.name shouldBe "name"
        }
    }

    @Test
    fun `lift property`() {
        val res = calculateMappings1(
                sources = listOf(
                        Source(parameterName = "person", path = listOf("name"), type = Type(packageName = "com.person.hub", name = "Name")),
                        Source(parameterName = "person", path = listOf("name", "firstName"), type = STRING)
                ),
                targets = listOf(Parameter(name = "name", type = STRING)),
                nameMappings = listOf(NameMapping(parameterName = "person", sourcePath = listOf("name", "firstName"), targetParameterName = "name"))
        )

        res.size shouldBe 1

        with(res.first()) {
            source.parameterName shouldBe "person"
            source.path.joinToString(".") shouldBe "name.firstName"
            target.name shouldBe "name"
        }
    }

    @Test
    fun `cannot map property if type is different`() {
        shouldThrow<KewtException> {
            calculateMappings1(
                    sources = listOf(
                            Source(parameterName = "person", path = listOf("name"), type = Type("my", "String"))
                    ),
                    targets = listOf(Parameter(name = "name", type = STRING ))
            )
        }
    }

    @Test
    fun `map property by with renaming`() {
        val res = calculateMappings1(
                sources = listOf(
                        Source(parameterName = "person", path = listOf("name"),type = STRING )
                ),
                targets = listOf(Parameter(name = "lastName", type = STRING)),
                nameMappings = listOf(NameMapping(parameterName = "person", sourcePath = listOf( "name"), targetParameterName = "lastName"))
        )

        res.size shouldBe 1
        with(res.first()) {
            source.parameterName shouldBe "person"
            source.path.first() shouldBe "name"
            target.name shouldBe "lastName"
        }
    }

    @Test
    fun `map by type if not ambiuous`() {
        val res = calculateMappings1(
                sources = listOf(Source(parameterName = "person", path = listOf("name"), type = STRING)),
                targets = listOf( Parameter(name = "lastName", type = STRING))
        )

        res.size shouldBe 1

        with(res.first()) {
            source.parameterName shouldBe "person"
            source.path.first() shouldBe "name"
            target.name shouldBe "lastName"
        }
    }

    @Test
    fun `cannot map by type if type is ambiguous`() {
        shouldThrow<AmbiguousMappingException> {
            calculateMappings1(
                    sources = listOf(
                            Source(parameterName = "person", path = listOf("name"), type = STRING),
                            Source(parameterName = "person", path = listOf("surname"), type = STRING)
                    ),
                    targets = listOf(
                            Parameter(name = "firstName", type = STRING),
                            Parameter(name = "lastName", type = STRING)
                    )
            )
        }
    }

    @Test
    fun `solve type ambiguity using name match`() {
        val res = calculateMappings1(
                sources = listOf(
                        Source(parameterName = "person", path = listOf("firstName"), type = STRING),
                        Source(parameterName = "person", path = listOf("surname"), type = STRING)
                ),
                targets = listOf(
                        Parameter(name = "firstName", type = STRING),
                        Parameter(name = "lastName", type = STRING)
                )
        )
        res.size shouldBe 2


        res.any { it.source.path.single() == "firstName" && it.target.name == "firstName" } shouldBe true
        res.any { it.source.path.single() == "surname" && it.target.name == "lastName" } shouldBe true
    }

    @Test
    fun `map collections`() {
        val res = calculateMappings1(
                sources = listOf(
                        Source(parameterName = "person", path = listOf("keys"), type = Type("kotlin.collections", "List", typeParameters = listOf(INT)))
                ),
                targets = listOf(
                        Parameter(name = "keys", type = Type("kotlin.collections", "List", typeParameters = listOf(INT)))
                )
        )

        res.size shouldBe 1
    }

    @Test
    fun `do not map if collection's type parameters are different`() {
        shouldThrow<KewtException> {
            calculateMappings1(
                    sources = listOf(Source(parameterName = "peson", path = listOf("keys"), type = Type("kotlin.collections", "List", typeParameters = listOf(STRING)))),
                    targets = listOf(Parameter(name = "keys", type = Type("kotlin.collections", "List", typeParameters = listOf(INT))))
            )
        }
    }

    @Test
    fun `map using name match and type conversion`() {
        val res = calculateMappings1(
                sources = listOf(Source(parameterName = "person", path = listOf("id"), type = INT)),
                targets = listOf(Parameter(name = "id", type = STRING)),
                conversionFunctions = listOf(MapperConversionFunction(
                        name = "f",
                        parameter = Parameter("x", type = INT),
                        returnType = STRING
                ))
        )

        res.size shouldBe 1

        with(res.single()) {
            source.path.first() shouldBe "id"
            target.name shouldBe "id"
            conversionContext?.conversionFunction?.name shouldBe "f"
        }
    }

    @Test
    fun `map using type conversion`() {
        val res = calculateMappings1(
                sources = listOf(Source(parameterName = "person", path = listOf("id"), type = INT)),
                targets = listOf(Parameter(name = "uuid", type = STRING)),
                conversionFunctions = listOf(MapperConversionFunction(
                        name = "f",
                        parameter = Parameter("x", type = INT),
                        returnType = STRING
                ))
        )

        res.size shouldBe 1

        with(res.single()) {
            source.path.first() shouldBe "id"
            target.name shouldBe "uuid"
            conversionContext?.conversionFunction?.name shouldBe "f"
        }
    }

    @Test
    fun `map collection elements using type conversion`() {
        val res = calculateMappings1(
                sources = listOf(
                        Source(parameterName = "person", path = listOf("ids"), type = Type("kotlin.collections", "List", typeParameters = listOf(INT)))
                ),
                targets = listOf(
                        Parameter(name = "ids", type = Type("kotlin.collections", "List", typeParameters = listOf(STRING)))
                ),
                conversionFunctions = listOf(MapperConversionFunction(
                        name = "f",
                        parameter = Parameter("x", type = INT),
                        returnType = STRING
                ))
        )

        res.size shouldBe 1

        with(res.single()) {
            source.path.first() shouldBe "ids"
            target.name shouldBe "ids"
            conversionContext?.conversionFunction?.name shouldBe "f"
            conversionContext?.usingElementMapping shouldBe true
        }
    }

    @Test
    fun `property mapping could contain parameter name`() {
        val res = calculateMappings1(
                sources = listOf(
                        Source(parameterName = "person", path = listOf("name"), type = STRING),
                        Source(parameterName = "person", path = listOf("surname"), type = STRING)
                ),
                targets = listOf(
                        Parameter(name = "lastName", type = STRING),
                        Parameter(name = "firstName", type = STRING)
                ),
                nameMappings = listOf(
                        NameMapping(parameterName = "person", sourcePath = listOf("name"), targetParameterName = "firstName"),
                        NameMapping(parameterName = "person", sourcePath = listOf("surname"), targetParameterName = "lastName")
                )
        )

        with(res) {
            size shouldBe 2

            this shouldHaveAtLeastOne {
                source.parameterName shouldBe "person"
                source.path.first() shouldBe "name"
                target.name shouldBe "firstName"
            }
            this shouldHaveAtLeastOne {
                source.parameterName shouldBe "person"
                source.path.first() shouldBe "surname"
                target.name shouldBe "lastName"
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
            calculateMappings1(
                    sources = listOf(
                            Source(parameterName = "person", path = listOf("name"), type = STRING)
                    ),
                    targets = listOf(Parameter(name = "lastName", type = STRING)),
                    nameMappings = listOf(NameMapping(parameterName = "person", sourcePath = listOf("nameX"), targetParameterName = "lastName"))
            )
        }.message shouldContain "Not existing source"
    }

    @Test
    fun `use parameter as explicit source`() {
           val res =  calculateMappings1(
                    sources = listOf(
                            Source(parameterName = "person", path = listOf(), type = Type("myPkg", "Person"))
                    ),
                    targets = listOf(Parameter(name = "lastName", type = STRING)),
                    nameMappings = listOf(NameMapping(parameterName = "person", sourcePath = emptyList(), targetParameterName = "lastName")),
                    conversionFunctions = listOf(
                            MapperConversionFunction(
                                    name = "f",
                                    parameter =  Parameter(name = "x", type = Type("myPkg", "Person")),
                                    returnType = STRING
                            )
                    )
            )

        with(res.single()){
            this.source.parameterName shouldBe "person"
            this.source.path shouldBe emptyList()
            this.conversionContext!!.conversionFunction!!.name shouldBe "f"
            this.target.name shouldBe "lastName"
        }
    }

    @Test
    fun `merge two sources`() {
        val res = calculateMappings1(
                sources = listOf(
                        Source(parameterName = "person", path = listOf("name"), type = STRING),
                        Source(parameterName = "pet", path = listOf("name"), type = STRING)
                ),
                targets = listOf(
                        Parameter(name = "name", type = STRING),
                        Parameter(name = "petName", type = STRING)
                ),
                nameMappings = listOf(
                        NameMapping(parameterName = "pet", sourcePath = listOf("name"), targetParameterName = "petName"),
                        NameMapping(parameterName = "person", sourcePath = listOf("name"), targetParameterName = "name")
                )
        )

        with(res) {
            size shouldBe 2

            this shouldHaveAtLeastOne {
                source.parameterName shouldBe "pet"
                source.path.first() shouldBe "name"
                target.name shouldBe "petName"
            }
            this shouldHaveAtLeastOne {
                source.parameterName shouldBe "person"
                source.path.first() shouldBe "name"
                target.name shouldBe "name"
            }
        }
    }


    @Test
    fun `merge two sources, infer implicit matching`() {
        val res = calculateMappings1(
                sources = listOf(
                        Source(parameterName = "person", path = listOf("name"), type = STRING),
                        Source(parameterName = "pet", path = listOf("name"), type = STRING)
                ),
                targets = listOf(
                        Parameter(name = "name", type = STRING),
                        Parameter(name = "petName", type = STRING)
                ),
                nameMappings = listOf(NameMapping(parameterName = "pet", sourcePath = listOf("name"), targetParameterName = "petName"))
        )

        with(res) {
            size shouldBe 2

            this shouldHaveAtLeastOne {
                source.parameterName shouldBe "pet"
                source.path.first() shouldBe "name"
                target.name shouldBe "petName"
            }
            this shouldHaveAtLeastOne {
                source.parameterName shouldBe "person"
                source.path.first() shouldBe "name"
                target.name shouldBe "name"
            }
        }
    }


    @Test
    fun `solve if two sources have ambiguous name mapping but explicit mapping is present`() {
        val res = calculateMappings1(
                sources = listOf(
                        Source(parameterName = "person", path = listOf("name"), type = STRING),
                        Source(parameterName = "pet", path = listOf("name"), type = STRING)
                ),
                targets = listOf(
                        Parameter(name = "name", type = STRING),
                        Parameter(name = "petName", type = STRING)
                ),
                nameMappings = listOf(NameMapping(parameterName = "pet", sourcePath = listOf("name"), targetParameterName = "petName"))
        )

        with(res) {
            size shouldBe 2
            this shouldHaveAtLeastOne {
                source.parameterName shouldBe "pet"
                source.path.first() shouldBe "name"
                target.name shouldBe "petName"
            }
            this shouldHaveAtLeastOne {
                source.parameterName shouldBe "person"
                source.path.first() shouldBe "name"
                target.name shouldBe "name"
            }
        }
    }

    @Test
    fun `use explicit function if there are more than one function is applicable`() {
        val res = calculateMappings1(
                sources = listOf(
                        Source(parameterName = "person", path = listOf("id"), type = STRING)
                ),
                targets = listOf(
                        Parameter(name = "id", type = INT)
                ),
                nameMappings = listOf(NameMapping(parameterName = "person", sourcePath = listOf("id"), targetParameterName = "id")),
                conversionFunctions = listOf(
                        MapperConversionFunction("f", Parameter("x", STRING), INT),
                        MapperConversionFunction("g", Parameter("x", STRING), INT)
                ),
                explicitConverters = listOf(ExplicitConverter("id", "f"))
        )
        res.single().conversionContext!!.conversionFunction!!.name shouldBe "f"
    }
    @Test
    fun `map from parameter`() {
        val res = calculateMappings1(
                sources = listOf(
                        Source(parameterName = "person", path = listOf("name"), type = STRING),
                        Source(parameterName = "age", path = listOf(), type = INT)
                ),
                targets = listOf(
                        Parameter(name = "name", type = STRING),
                        Parameter(name = "age", type = INT)
                )
        )
        res shouldHaveAtLeastOne {
            source.parameterName shouldBe "age"
            source.path shouldBe emptyList()
            target.name shouldBe "age"
        }
    }

    @Test
    fun `map java list of strings to kotlin`() {
        val res = calculateMappings1(
                sources = listOf(
                        Source(parameterName = "person", path = listOf("ids"), type = Type(packageName = "java.util", name = "List", nullability = Nullability.PLATFORM, typeParameters = listOf(Type(packageName = "java.lang", name = "String", nullability = Nullability.PLATFORM))))
                ),
                targets = listOf(
                        Parameter(name = "ids", type = Type(packageName = "kotlin.collections", name = "List", typeParameters = listOf(STRING)))
                )
        )
        with(res.single()){
            source.parameterName shouldBe "person"
            source.path shouldBe listOf("ids")
            target.name shouldBe "ids"
            with(conversionContext!!){
                usingElementMapping shouldBe false
            }
        }
    }
}

fun calculateMappings1(
        sources: List<Source>,
        targets: List<Parameter>,
        nameMappings: List<NameMapping> = emptyList(),
        conversionFunctions: List<MapperConversionFunction> = emptyList(),
        explicitConverters: List<ExplicitConverter> = emptyList()
): List<PropertyMapping> {
    return calculateMappings(
            sources = sources,
            targets = targets.toSet(),
            nameMappings = nameMappings,
            explicitConverters = explicitConverters,
            conversionFunctions = conversionFunctions,
            returnPropertiesWithDefaultValues = emptySet()
    )
}