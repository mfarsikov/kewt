package com.github.mfarsikov.kewt.processor.mapper

import com.github.mfarsikov.kewt.processor.AmbiguousMappingException
import com.github.mfarsikov.kewt.processor.ConversionFunction
import com.github.mfarsikov.kewt.processor.ExplicitConverter
import com.github.mfarsikov.kewt.processor.KewtException
import com.github.mfarsikov.kewt.processor.Logger
import com.github.mfarsikov.kewt.processor.NameMapping
import com.github.mfarsikov.kewt.processor.Parameter
import com.github.mfarsikov.kewt.processor.Type


fun calculateMappings(
        function: ReadyForMappingFunction,
        conversionFunctions: List<ConversionFunction>
): MappedFunction {


    //TODO move validation out of mapping calculations
    function.nameMappings.groupBy { it.targetParameterName }
            .filterValues { it.size > 1 }
            .takeIf { it.isNotEmpty() }
            ?.let { throw KewtException("more than one source mapped to the same target: ${it.values.flatten()}}") }

    val allSources = function.parameters.flatMap { parameter -> parameter.resolvedType.properties.map { "${parameter.name}.${it.name}" } }
    val explicitlyMappedSources =function.nameMappings.map { "${it.parameterName}.${it.sourcePath}"}
    val notExistingSources = explicitlyMappedSources - allSources
    if (notExistingSources.isNotEmpty()) throw KewtException("Not existing sources: $notExistingSources")

    val explicitTargets = function.nameMappings.map { it.targetParameterName }
    val allTargets = function.returnType.properties.map { it.name }
    val notExistingTargets = explicitTargets - allTargets
    if (notExistingTargets.isNotEmpty()) throw KewtException("Not existing targets: $notExistingTargets")

    val typeMatcher = TypeMatcher(conversionFunctions)

    val sourceProperties = function.parameters.flatMap { parameter ->
        parameter.resolvedType.properties.map {
            FlatProperty(
                    parameterName = parameter.name,
                    propertyName = it.name,
                    propertyType = it.type
            )
        }
    }

    val explicitMappings = function.returnType.properties.mapNotNull { targetProperty ->
        val explicitNameMapping = function.nameMappings.filter { it.targetParameterName == targetProperty.name }.singleOrNull()
        explicitNameMapping?.let { nameMapping ->
            explicitMapping(
                    sources = function.parameters,
                    explicitNameMapping = nameMapping,
                    typeMatcher = typeMatcher,
                    targetProperty = targetProperty,
                    explicitConverter = function.explicitConverters.singleOrNull { it.targetName == targetProperty.name }?.converterName
            )
        }
    }

    val explicitlyMappedTargets = explicitMappings.map { it.targetProperty }.toSet()
    val explicityMappedSources = explicitMappings.map { it.parameterName + it.sourceProperty.name }.toSet()

    val targets = function.returnType.properties.filter { it !in explicitlyMappedTargets }
    val sources = sourceProperties.filter { it.parameterName + it.propertyName !in explicityMappedSources }


    val matchedByName = targets.mapNotNull { targetProperty ->
        val sourcesWithSameName = sources.filter { it.propertyName == targetProperty.name }
        val matchedByName = sourcesWithSameName.mapNotNull { sourceProperty ->
            typeMatcher.findConversion(sourceProperty.propertyType, targetProperty.type)?.let {
                PropertyMapping(
                        parameterName = sourceProperty.parameterName,
                        sourceProperty = Parameter(sourceProperty.propertyName, sourceProperty.propertyType),
                        targetProperty = targetProperty,
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

        if (matchedByName.size > 1) throw AmbiguousMappingException("ambiguous mapping: ${matchedByName.map { it.stringify() }}")

        matchedByName.singleOrNull()
    }

    val targetsMappedByName = matchedByName.map { it.targetProperty }.toSet()
    val sourcesMappedByName = matchedByName.map { it.parameterName + it.sourceProperty.name }.toSet()

    val t = targets.filter { it !in targetsMappedByName }
    val s = sources.filter { it.parameterName + it.propertyName !in sourcesMappedByName }


    val mappedByType = t.mapNotNull { targetProperty ->
        val typeMatches = s.mapNotNull { sourceProperty ->
            typeMatcher.findConversion(sourceProperty.propertyType, targetProperty.type)?.let {
                PropertyMapping(
                        parameterName = sourceProperty.parameterName,
                        sourceProperty = Parameter(sourceProperty.propertyName, sourceProperty.propertyType),
                        targetProperty = targetProperty,
                        conversionContext = it
                )
            }
        }
        if (typeMatches.size > 1) throw AmbiguousMappingException("ambiguous mapping: ${typeMatches.map { it.stringify() }}")
        typeMatches.singleOrNull()
    }

    val k2 = mappedByType.map { it.targetProperty }.toSet()

    val notMappedTargets = t.filter { it !in k2 }

    if (notMappedTargets.isNotEmpty()) throw KewtException("Not mapped targets: ${notMappedTargets}")

    return MappedFunction(
            name = function.name,
            parameters = function.parameters,
            returnType = function.returnType,
            mappings = explicitMappings + matchedByName + mappedByType
    )
}

private fun explicitMapping(
        sources: List<ResolvedParameter>,
        explicitNameMapping: NameMapping,
        typeMatcher: TypeMatcher,
        targetProperty: Parameter,
        explicitConverter: String?
): PropertyMapping {
    val sourceParameter = sources.singleOrNull() { it.name == explicitNameMapping.parameterName }
            ?: throw KewtException("not found parameter with name: ${explicitNameMapping.parameterName}")

    val property = sourceParameter.resolvedType.properties.singleOrNull { it.name == explicitNameMapping.sourcePath }
            ?: throw KewtException("not found property ${explicitNameMapping.parameterName}.${explicitNameMapping.sourcePath}")


    val c = typeMatcher.findConversion(property.type, targetProperty.type, explicitConverter)
            ?: throw KewtException("Cannot map properties, source { $property }, target { $targetProperty }")

    return PropertyMapping(
            parameterName = sourceParameter.name,
            sourceProperty = property,
            targetProperty = targetProperty,
            conversionContext = c
    )
}


fun PropertyMapping.stringify() = "${parameterName}.${sourceProperty.name}: ${sourceProperty.type} = ${targetProperty.name}: ${targetProperty.type}"

data class ReadyForMappingFunction(
        val name: String,
        val parameters: List<ResolvedParameter>,
        val returnType: ResolvedType,
        val nameMappings: List<NameMapping>,
        val explicitConverters: List<ExplicitConverter>
)

private data class FlatProperty(
        val parameterName: String,
        val propertyName: String,
        val propertyType: Type
) {
    override fun toString() = "$parameterName.$propertyName: $propertyType"
}

data class ResolvedParameter(
        val name: String,
        val resolvedType: ResolvedType
)

data class ResolvedType(
        val type: Type,
        val properties: Set<Parameter>,
        val language: Language
) {
    override fun toString() = "$language: $type { ${properties.joinToString(", ")} }"
}

enum class Language { KOTLIN, JAVA, PROTO }

data class MappedFunction(
        val name: String,
        val parameters: List<ResolvedParameter>,
        val returnType: ResolvedType,
        val mappings: List<PropertyMapping>
)

data class PropertyMapping(
        val parameterName: String,
        val sourceProperty: Parameter,
        val targetProperty: Parameter,
        val conversionContext: ConversionContext? = null
) {
    override fun toString() = "$targetProperty <= $parameterName.$sourceProperty, $conversionContext"
}

data class AnnotationConfig(
        val source: String,
        val target: String,
        val converter: String?
)


data class ConversionContext(
        val conversionFunction: ConversionFunction? = null,
        val usingElementMapping: Boolean = false,
        val usingNullSafeCall: Boolean = false,
        val nullableAssignedToPlatform: Boolean = false
) {
    override fun toString() = "using elvis: $usingNullSafeCall, element mapping: $usingElementMapping, ${conversionFunction?.let { ", func: $it" } ?: ""}"
}