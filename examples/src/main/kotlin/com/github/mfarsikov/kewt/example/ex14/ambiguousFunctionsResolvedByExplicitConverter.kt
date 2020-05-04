package com.github.mfarsikov.kewt.example.ex14

import com.github.mfarsikov.kewt.annotations.Mapper
import com.github.mfarsikov.kewt.annotations.Mapping
import com.github.mfarsikov.kewt.annotations.Mappings

data class Person(val id: Int)
data class Employee(val id: String)

@Mapper
interface PersonMapper {
    @Mappings([
        Mapping(source = "id", target = "id", converter = "validConverter")
    ])
    fun convert(person: Person): Employee

    fun validConverter(x: Int) = x.toString()
    fun invalidConverter(x: Int): String = throw RuntimeException()
}