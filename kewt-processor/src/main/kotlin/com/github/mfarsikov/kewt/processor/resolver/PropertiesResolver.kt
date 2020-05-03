package com.github.mfarsikov.kewt.processor.resolver

import com.github.mfarsikov.kewt.processor.KewtException
import com.github.mfarsikov.kewt.processor.Logger
import com.github.mfarsikov.kewt.processor.NameMapping
import com.github.mfarsikov.kewt.processor.Nullability
import com.github.mfarsikov.kewt.processor.Parameter
import com.github.mfarsikov.kewt.processor.Type
import com.github.mfarsikov.kewt.processor.extractPackage
import com.github.mfarsikov.kewt.processor.mapper.Language
import com.github.mfarsikov.kewt.processor.mapper.ResolvedParameter
import com.github.mfarsikov.kewt.processor.mapper.ResolvedType
import com.github.mfarsikov.kewt.processor.parser.kmClass
import com.github.mfarsikov.kewt.processor.toType
import com.google.protobuf.GeneratedMessageV3
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element


interface PropertiesResolver {
    fun resolveTypes(returnType: Type, parameters: List<Parameter>, nameMappings: List<NameMapping>): Pair<ResolvedType, List<ResolvedParameter>>
}

class PropertiesResolverImpl(
        roundEnvironment: RoundEnvironment,
        private val processingEnv: ProcessingEnvironment
) : PropertiesResolver {
    private val cache: MutableMap<Type, ResolvedType> = mutableMapOf()

    private val elementsByQualifiedName: Map<String, Element> = roundEnvironment.rootElements.associateBy {
        "${processingEnv.elementUtils.getPackageOf(it).qualifiedName}.${it.simpleName}"
    }

    override fun resolveTypes(returnType: Type, parameters: List<Parameter>, nameMappings: List<NameMapping>): Pair<ResolvedType, List<ResolvedParameter>> {
        val resolvedReturnType = resolveProperties(returnType)

        val resolvedParameters = parameters.map { parameter ->

            val sourcePaths = nameMappings.filter { it.parameterName == parameter.name }.map { it.sourcePath }

            val liftedProperties = sourcePaths.mapNotNull { source -> findType(source.split("."), parameter.type)?.let { Parameter(name = source, type = it) } }
            val resolvedProperties = resolveProperties(parameter.type)

            ResolvedParameter(
                    name = parameter.name,
                    resolvedType = resolvedProperties.copy(properties = resolvedProperties.properties + liftedProperties)
            )
        }
        return Pair(resolvedReturnType, resolvedParameters)
    }

    @Synchronized
    private fun resolveProperties(type: Type): ResolvedType = cache.computeIfAbsent(type) {
        Logger.trace("Resolving type: $type")

        val element = elementsByQualifiedName["${type.packageName}.${type.name}"]

        Logger.trace("Element found: ${element != null}")


        if (element?.getAnnotation(Metadata::class.java) == null) {
            Logger.debug("Not in sources: ${type.qualifiedName()} ")
            val loadFromLibrary = loadFromLibrary(type)
            Logger.trace("Loaded from library: ${type.qualifiedName()} ")
            if (loadFromLibrary.getAnnotation(Metadata::class.java) == null) {
                Logger.debug("Does not have metadata annotation, is Java class: ${type.qualifiedName()} ")
                if (loadFromLibrary.superclass.equals(GeneratedMessageV3::class.java)) {
                    Logger.debug("Protobuf: ${type.qualifiedName()} ")
                    //PROTOBUF
                    val protoFields = listOf(
                            "getDefaultInstance",
                            "getDefaultInstanceForType",
                            "getDescriptor",
                            "getParserForType",
                            "getSerializedSize",
                            "getUnknownFields"
                    )
                    val getters = loadFromLibrary.declaredMethods.filter { it.name.startsWith("get") }
                            .filter { it.parameterCount == 0 && !it.isSynthetic }
                            .filter { it.name !in protoFields }

                    val anotherProtoFields = getters.map { "${it.name}Bytes" }.toSet() +
                            getters.filter { it.name.endsWith("List") }.map { it.name.substringBefore("List") + "Count" } +
                            getters.map {
                            val listSuffix = if(it.name.endsWith("List"))"List" else ""
                                val name = it.name.substringBeforeLast("List")
                                "${name}OrBuilder${listSuffix}"
                            }

                    val realGetters = getters.filter { it.name !in anotherProtoFields }
                    val properties = realGetters.map {
                        Parameter(
                                name = it.name.convertFromGetter(),
                                type = it.genericReturnType.toType()
                        )
                    }
                    ResolvedType(
                            type = type,
                            properties = properties.toSet(),
                            language = Language.PROTO
                    )
                } else {
                    Logger.debug("Java class: ${type.qualifiedName()} ")
                    val properties: List<Parameter> = TODO()
                    //JAVA
                    ResolvedType(
                            type = type,
                            properties = properties.toSet(),
                            language = Language.JAVA
                    )
                    TODO("Javaa common")
                }
            } else {
                Logger.debug("Kotlin class: ${type.qualifiedName()} ")
                //KOTLIN
                val properties = loadFromLibrary.getAnnotation(Metadata::class.java).kmClass()!!.properties.map {//TODO fallback to java if km is null?
                    Parameter(
                            name = it.name,
                            type = it.returnType.toType()
                    )
                }
                ResolvedType(
                        type = type,
                        properties = properties.toSet(),
                        language = Language.KOTLIN
                )
            }
        } else {
            Logger.trace("Found in sources: ${type.qualifiedName()} ")
            val properties = element.getAnnotation(Metadata::class.java).kmClass()!!.properties.map {//TODO fallback to java if km is null?
                Parameter(
                        name = it.name,
                        type = it.returnType.toType()
                )
            }
            ResolvedType(
                    type = type,
                    properties = properties.toSet(),
                    language = Language.KOTLIN
            )

        }.also { Logger.debug("Resolved type: $it") }
    }

    private fun loadFromLibrary(type: Type): Class<*> = try {
        Logger.trace("Trying to load from library: ${type.packageName}.${type.name}")

        Class.forName(listOf(type.packageName, type.name).filter { it.isNotBlank() }.joinToString(separator = "."))
    } catch (ex: ClassNotFoundException) {
        throw KewtException("$type", ex)
    }

    private fun findType(nestedSourceNames: List<String>, type: Type): Type? =
            nestedSourceNames.fold(type) { typet: Type?, name ->
                typet?.let { resolveProperties(typet) }
                        ?.properties
                        ?.singleOrNull { it.name == name }
                        ?.type
            }
}

private fun java.lang.reflect.Type.toType():Type =
        when (this) {
            is ParameterizedTypeImpl -> Type(
                    packageName = this.rawType.`package`.name,
                    name = rawType.simpleName,
                    nullability = Nullability.PLATFORM,
                    typeParameters = actualTypeArguments.map { it.toType() }
            )
            else -> if(this.typeName == "com.google.protobuf.ProtocolStringList"){
                Type(
                        packageName = "java.util",
                        name = "List",
                        nullability = Nullability.PLATFORM,
                        typeParameters = listOf(Type(
                                packageName = "java.lang",
                                name = "String",
                                nullability = Nullability.PLATFORM
                        ))
                )
            } else
                Type(
                    packageName = typeName.extractPackage(),
                    name = typeName.substringAfterLast("."),
                    nullability = Nullability.PLATFORM
            )
        }


private fun <T> Class<T>.toType() = Type(
        packageName = canonicalName.extractPackage(),
        name = canonicalName.substringAfterLast("."),
        nullability = Nullability.PLATFORM,//TODO read non-null annotations?
        typeParameters = emptyList()
)

private fun String.convertFromGetter() = substringAfter("get").decapitalize()
