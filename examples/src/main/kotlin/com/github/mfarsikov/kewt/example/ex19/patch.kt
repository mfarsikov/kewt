package com.github.mfarsikov.kewt.example.ex19

import com.github.mfarsikov.kewt.annotations.Mapper
import com.github.mfarsikov.kewt.annotations.Target
data class Person(val name: String?, val age: Int?)

@Mapper
interface PersonMapper19 {
    fun update(
            @Target target: Person,
            source: Person
    ): Person
}
