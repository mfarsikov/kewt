package com.github.mfarsikov.kewt.example.ex10

import com.github.mfarsikov.kewt.annotations.Mapper
import com.github.mfarsikov.kewt.annotations.Mapping
import com.github.mfarsikov.kewt.annotations.Mappings

data class Person(val name: Name)
data class Name(val firstName: String)

data class Employee(val name: String)

@Mapper
interface PersonMapping {
    @Mappings([
        Mapping(source = "person.name.firstName", target = "name")
    ])
    fun toEmployee(person: Person): Employee
}
