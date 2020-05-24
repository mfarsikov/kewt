package com.github.mfarsikov.kewt.example.ex24

import com.github.mfarsikov.kewt.annotations.Mapper
import com.github.mfarsikov.kewt.annotations.Mapping
import com.github.mfarsikov.kewt.annotations.Mappings

data class Person(val name: String)
data class Employee(val upperCasedName: String)

@Mapper
interface PersonMapper24 {
    @Mappings([
        Mapping(source="person", target = "upperCasedName")
    ])
    fun toEmployee(person: Person): Employee

    fun nameToUpperCase(person: Person) = person.name.toUpperCase()
}