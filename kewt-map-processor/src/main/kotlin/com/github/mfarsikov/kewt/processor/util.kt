package com.github.mfarsikov.kewt.processor

import kotlinx.metadata.Flag
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmType

/**
 * com.my.company.MyClass<in kotlin.Int, out kotlin.String>
 *     group 2: com.my.company
 *     group 3: MyClass
 *     group 4: <in kotlin.Int, out kotlin.String>
 */
private val typeDeclarationPattern = "^(([\\w\\.]*)\\.)?(\\w*)(<.*>)?".toRegex()

fun String.extractPackage() = typeDeclarationPattern.find(this)!!.groupValues[2]
fun String.extractClassName() = typeDeclarationPattern.find(this)!!.groupValues[3]

fun KmType.toType(): Type {
    val classifier = classifier as KmClassifier.Class
    val packageName = classifier.name.replace("/", ".").extractPackage()
    val typeName = classifier.name.replace("/", ".").extractClassName()
    val typeArguments = arguments.mapNotNull { it.type?.toType() }
    val isNullable = Flag.Type.IS_NULLABLE.invoke(this.flags)
    return Type(packageName = packageName, name = typeName, nullability = if (isNullable) Nullability.NULLABLE else Nullability.NON_NULLABLE, typeParameters = typeArguments)
}
