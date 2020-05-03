package com.github.mfarsikov.kewt.example.ex6

import com.github.mfarsikov.kewt.annotations.Mapper
import com.github.mfarsikov.kewt.annotations.Mapping
import com.github.mfarsikov.kewt.annotations.Mappings

data class Person(val name: String, val surname: String)
data class Employee(val firstName: String, val lastName: String)

@Mapper
interface PersonMapper {
    @Mappings([
        Mapping(source = "name", target = "firstName"),
        Mapping(source = "surname", target = "lastName")
    ])
    fun toEmployee(person: Person): Employee
}

