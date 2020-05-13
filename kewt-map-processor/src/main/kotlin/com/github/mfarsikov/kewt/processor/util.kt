package com.github.mfarsikov.kewt.processor

fun String.extractPackage() = if(contains(".")) substringBeforeLast(".").substringAfter("in "). substringAfter("out ") else ""
