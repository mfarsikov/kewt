package com.github.mfarsikov.kewt.processor

fun String.extractPackage() = if(contains(".")) substringBeforeLast(".") else ""
