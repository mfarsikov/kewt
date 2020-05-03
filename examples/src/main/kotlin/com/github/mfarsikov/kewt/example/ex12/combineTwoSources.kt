package com.github.mfarsikov.kewt.example.ex12

import com.github.mfarsikov.kewt.annotations.Mapper
import com.github.mfarsikov.kewt.annotations.Mapping
import com.github.mfarsikov.kewt.annotations.Mappings

data class Person(val name: String)
data class Pet(val name: String)

data class Employee(val name: String, val petName: String)

@Mapper
interface PersonMapper {
    @Mappings([
        Mapping(source = "pet.name", target = "petName")
    ])
    fun toEmployee(person: Person, pet: Pet): Employee
}