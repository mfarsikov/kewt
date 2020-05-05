package com.github.mfarsikov.kewt.processor

import com.github.mfarsikov.kewt.processor.Nullability.NON_NULLABLE
import com.github.mfarsikov.kewt.processor.Nullability.NULLABLE
import kotlinx.metadata.Flag
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmType

fun KmType.toType(): Type {
    val classifier = classifier as KmClassifier.Class
    val packageName = classifier.name.replace("/", ".").extractPackage()
    val typeName = classifier.name.substringAfterLast("/")
    val typeArguments = arguments.mapNotNull { it.type?.toType() }
    val isNullable = Flag.Type.IS_NULLABLE.invoke(this.flags)
    return Type(packageName = packageName, name = typeName, nullability = if (isNullable) NULLABLE else NON_NULLABLE, typeParameters = typeArguments)
}