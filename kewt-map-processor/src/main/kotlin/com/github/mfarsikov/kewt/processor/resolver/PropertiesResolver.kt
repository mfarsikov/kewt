package com.github.mfarsikov.kewt.processor.resolver

import com.github.mfarsikov.kewt.processor.ConstructorParameter
import com.github.mfarsikov.kewt.processor.KewtException
import com.github.mfarsikov.kewt.processor.Logger
import com.github.mfarsikov.kewt.processor.NameMapping
import com.github.mfarsikov.kewt.processor.Nullability
import com.github.mfarsikov.kewt.processor.Parameter
import com.github.mfarsikov.kewt.processor.Type
import com.github.mfarsikov.kewt.processor.extractPackage
import com.github.mfarsikov.kewt.processor.mapper.Language
import com.github.mfarsikov.kewt.processor.mapper.ResolvedType
import com.github.mfarsikov.kewt.processor.mapper.Source
import com.github.mfarsikov.kewt.processor.parser.tryCast
import com.github.mfarsikov.kewt.processor.toType
import com.google.protobuf.GeneratedMessageV3
import kotlinx.metadata.Flag
import kotlinx.metadata.KmClass
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element


class PropertiesResolver(
        roundEnvironment: RoundEnvironment,
        private val processingEnv: ProcessingEnvironment
) {
    private val cache: MutableMap<String, ResolvedType<ConstructorParameter>> = mutableMapOf(*types)

    private val elementsByQualifiedName: Map<String, Element> = roundEnvironment.rootElements.associateBy {
        "${processingEnv.elementUtils.getPackageOf(it).qualifiedName}.${it.simpleName}"
    }

    fun nestedParameterProperties(nameMappings: List<NameMapping>, parameter: Parameter): List<Source> = nameMappings
            .map { it.sourcePath }
            .mapNotNull { sourcePath ->
                findType(sourcePath, parameter.type)?.let { type ->
                    Source(parameterName = parameter.name, path = sourcePath, type = type)
                }
            }


    @Synchronized
    fun resolveType(type: Type): ResolvedType<ConstructorParameter> = cache.computeIfAbsent(type.qualifiedName()) {
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
                                val listSuffix = if (it.name.endsWith("List")) "List" else ""
                                val name = it.name.substringBeforeLast("List")
                                "${name}OrBuilder${listSuffix}"
                            }

                    val realGetters = getters.filter { it.name !in anotherProtoFields }
                    val properties = realGetters.map {
                        ConstructorParameter(
                                name = it.name.convertFromGetter(),
                                type = it.genericReturnType.toType(),
                                hasDefaultValue = false //TODO proto3 all fields optional
                        )
                    }
                    ResolvedType(
                            type = type,
                            properties = properties.toSet(),
                            language = Language.PROTO
                    )
                } else {
                    Logger.debug("Java class: ${type.qualifiedName()} ")
                    val properties: List<ConstructorParameter> = TODO()
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
                val properties = loadFromLibrary.getAnnotation(Metadata::class.java).kmClass()!!
                        .constructors.single { Flag.Constructor.IS_PRIMARY.invoke(it.flags) }
                        .valueParameters
                        .map {//TODO fallback to java if km is null?
                    ConstructorParameter(
                            name = it.name,
                            type = it.type!!.toType(),
                            hasDefaultValue = Flag.ValueParameter.DECLARES_DEFAULT_VALUE.invoke(it.flags)
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
            val c = element.getAnnotation(Metadata::class.java).kmClass()!!//TODO fallback to java if km is null?
            val properties = c.constructors.single { Flag.Constructor.IS_PRIMARY.invoke(it.flags) }
                    .valueParameters
                    .map {
                        ConstructorParameter(
                                name = it.name,
                                type = it.type!!.toType(),
                                hasDefaultValue = Flag.ValueParameter.DECLARES_DEFAULT_VALUE.invoke(it.flags)
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

    fun findType(nestedSourceNames: List<String>, type: Type): Type? =
            nestedSourceNames.fold(type) { typet: Type?, name ->
                typet?.let { resolveType(typet) }
                        ?.properties
                        ?.singleOrNull { it.name == name }
                        ?.type
            }
}

private fun java.lang.reflect.Type.toType(): Type =
        when (this) {
            is ParameterizedTypeImpl -> Type(
                    packageName = this.rawType.`package`.name,
                    name = rawType.simpleName,
                    nullability = Nullability.PLATFORM,
                    typeParameters = actualTypeArguments.map { it.toType() }
            )
            else -> if (this.typeName == "com.google.protobuf.ProtocolStringList") {
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
private val cache: MutableMap<Type, ResolvedType<Parameter>> = mutableMapOf()

val types = listOf(//TODO check could it be skipped
        Type("kotlin", "String"),
        Type("kotlin", "Long"),
        Type("kotlin", "Int"),
        Type("kotlin", "Double"),
        Type("kotlin", "Float"),
        Type("kotlin", "Boolean"),
        Type("java.util", "UUID")
).map {
    ResolvedType<ConstructorParameter>(
            type = it,
            properties = emptySet(),
            language = Language.KOTLIN
    )
}
        .map { it.type.qualifiedName() to it }
        .toTypedArray()

data class A(val x: String = "X", val y: Int, val z: Boolean = true)

fun Metadata.kmClass(): KmClass? =
        let {
            KotlinClassHeader(
                    it.kind,
                    it.metadataVersion,
                    it.bytecodeVersion,
                    it.data1,
                    it.data2,
                    it.extraString,
                    it.packageName,
                    it.extraInt
            )
        }
                .let { KotlinClassMetadata.read(it) }
                ?.tryCast<KotlinClassMetadata.Class>()
                ?.toKmClass()