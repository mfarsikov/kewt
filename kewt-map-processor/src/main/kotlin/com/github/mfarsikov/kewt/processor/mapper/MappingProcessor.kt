package com.github.mfarsikov.kewt.processor.mapper

import com.github.mfarsikov.kewt.processor.AmbiguousMappingException
import com.github.mfarsikov.kewt.processor.ExplicitConverter
import com.github.mfarsikov.kewt.processor.KewtException
import com.github.mfarsikov.kewt.processor.Logger
import com.github.mfarsikov.kewt.processor.NameMapping
import com.github.mfarsikov.kewt.processor.NotMappedTarget
import com.github.mfarsikov.kewt.processor.Parameter
import com.github.mfarsikov.kewt.processor.Type

fun calculateMappings(
        sources: List<Source>,
        targets: Set<Parameter>,
        nameMappings: List<NameMapping>,
        explicitConverters: List<ExplicitConverter>,
        conversionFunctions: List<MapperConversionFunction>,
        returnPropertiesWithDefaultValues: Set<String>
): List<PropertyMapping> {

    Logger.debug("Mapping input: sources=$sources, targets=$targets, nameMappings=$nameMappings, explicitConverters=$explicitConverters, conversionFunctions=$conversionFunctions")


    //TODO move validation out of mapping calculations
//    nameMappings.groupBy { it.targetParameterName }
//            .filterValues { it.size > 1 }
//            .takeIf { it.isNotEmpty() }
//            ?.let { throw KewtException("more than one source mapped to the same target: ${it.values.flatten()}}") }

//    val allSources = parameters.flatMap { parameter -> parameter.resolvedType.properties.map { "${parameter.name}.${it.name}" } }
//    val explicitlyMappedSources =nameMappings.map { "${it.parameterName}.${it.sourcePath}"}
//    val notExistingSources = explicitlyMappedSources - allSources
//    if (notExistingSources.isNotEmpty()) throw KewtException("Not existing sources: $notExistingSources")
//
//    val explicitTargets = nameMappings.map { it.targetParameterName }
//    val allTargets = returnType.properties.map { it.name }
//    val notExistingTargets = explicitTargets - allTargets
//    if (notExistingTargets.isNotEmpty()) throw KewtException("Not existing targets: $notExistingTargets")

    val typeMatcher = TypeMatcher(conversionFunctions)

//    val sources = parameters.flatMap { parameter ->
//        parameter.resolvedType.properties.map {
//            FlatProperty(
//                    parameterName = parameter.name,
//                    propertyName = it.name,
//                    propertyType = it.type
//            )
//        }
//    }

    val explicitMappings = targets.mapNotNull { targetProperty ->
        val explicitNameMapping = nameMappings.filter { it.targetParameterName == targetProperty.name }.singleOrNull()
        explicitNameMapping?.let { nameMapping ->
            explicitMapping(
                    sources = sources,
                    explicitNameMapping = nameMapping,
                    typeMatcher = typeMatcher,
                    targetProperty = targetProperty,
                    explicitConverter = explicitConverters.singleOrNull { it.targetName == targetProperty.name }?.converterName
            )
        }
    }

    val explicitlyMappedTargets = explicitMappings.map { it.target }.toSet()
    val explicityMappedSources = explicitMappings.map { it.source }.toSet()

    val targets = targets.filter { it !in explicitlyMappedTargets }
    val sources = sources.filter { it !in explicityMappedSources }

    sources.filter { it.path.size > 1 }.takeIf { it.isNotEmpty() }?.let { throw KewtException("not matched lifted property: $it") }

    val matchedByName = targets.mapNotNull { targetProperty ->
        val sourcesWithSameName = sources.filter { it.path.firstOrNull() ?: it.parameterName == targetProperty.name }
        val matchedByName = sourcesWithSameName.mapNotNull { sourceProperty ->
            typeMatcher.findConversion(sourceProperty.type, targetProperty.type)?.let {
                PropertyMapping(
                        source = sourceProperty,
                        target = targetProperty,
                        conversionContext = it
                )
            }.also {
                if (it == null) {
                    Logger.debug("Not matched: $targetProperty <= $sourceProperty")
                } else {
                    Logger.trace("Matched: $it")
                }
            }
        }

        if (matchedByName.size > 1) throw AmbiguousMappingException("ambiguous mapping: $matchedByName")

        matchedByName.singleOrNull()
    }

    val targetsMappedByName = matchedByName.map { it.target }.toSet()
    val sourcesMappedByName = matchedByName.map { it.source }.toSet()

    val t = targets.filter { it !in targetsMappedByName }
    val s = sources.filter { it !in sourcesMappedByName }


    val mappedByType = t.mapNotNull { targetProperty ->
        val typeMatches = s.mapNotNull { sourceProperty ->
            typeMatcher.findConversion(sourceProperty.type, targetProperty.type)?.let {
                PropertyMapping(
                        source = sourceProperty,
                        target = targetProperty,
                        conversionContext = it
                )
            }
        }
        if (typeMatches.size > 1) throw AmbiguousMappingException("ambiguous mapping: $typeMatches")
        typeMatches.singleOrNull()
    }

    val k2 = mappedByType.map { it.target }.toSet()

    val notMappedTargets = t.filter { it !in k2 && it.name !in returnPropertiesWithDefaultValues }

    if (notMappedTargets.isNotEmpty()) throw NotMappedTarget(notMappedTargets)

    return explicitMappings + matchedByName + mappedByType
}

private fun explicitMapping(
        sources: List<Source>,
        explicitNameMapping: NameMapping,
        typeMatcher: TypeMatcher,
        targetProperty: Parameter,
        explicitConverter: String?
): PropertyMapping {
    val sourceParameter = sources.filter { it.parameterName == explicitNameMapping.parameterName }

    if (sourceParameter.isEmpty()) throw KewtException("not found parameter with name: ${explicitNameMapping.parameterName}")

    val property = sourceParameter.singleOrNull { it.path == explicitNameMapping.sourcePath }
            ?: throw KewtException("Not existing source: ${explicitNameMapping.parameterName}.${explicitNameMapping.sourcePath}, among: ${sourceParameter.map { it.path }} ")


    val c = typeMatcher.findConversion(property.type, targetProperty.type, explicitConverter)
            ?: throw KewtException("Cannot map properties, source { $property }, target { $targetProperty }")

    return PropertyMapping(
            source = property,
            target = targetProperty,
            conversionContext = c
    )
}

data class ResolvedParameter<T>(
        val name: String,
        val resolvedType: ResolvedType<T>
)

data class ResolvedType<T>(
        val type: Type,
        val properties: Set<T>,
        val language: Language
) {
    override fun toString() = "$language: $type { ${properties.joinToString(", ")} }"
    fun <R> mapParameter(f: (T) -> R): ResolvedType<R> = ResolvedType(
            type = type,
            properties = properties.map { f(it) }.toSet(),
            language = language
    )
}

enum class Language { KOTLIN, JAVA, PROTO }

data class Source(
        val parameterName: String,
        val path: List<String>,
        val type: Type
) {
    override fun toString() = """${(listOf(parameterName) + path).joinToString(".")}: $type"""
}

data class PropertyMapping(
        val source: Source,
        val target: Parameter,
        val conversionContext: MappingConversionContext? = null
) {
    override fun toString() = "$target <= $source, $conversionContext"
}

data class AnnotationConfig(
        val source: String,
        val target: String,
        val converter: String?
)


data class MappingConversionContext(
        val conversionFunction: MapperConversionFunction? = null,
        val usingElementMapping: Boolean = false,
        val usingNullSafeCall: Boolean = false,
        val nullableAssignedToPlatform: Boolean = false
) {
    override fun toString() = "using elvis: $usingNullSafeCall, element mapping: $usingElementMapping, ${conversionFunction?.let { ", func: $it" } ?: ""}"
}

data class MapperConversionFunction(
        val name: String,
        val parameter: Parameter,
        val returnType: Type
) {
    override fun toString() = "$name($parameter): $returnType"
}
