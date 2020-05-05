package com.github.mfarsikov.kewt.processor

open class KewtException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class AmbiguousMappingException(message: String) : KewtException(message)