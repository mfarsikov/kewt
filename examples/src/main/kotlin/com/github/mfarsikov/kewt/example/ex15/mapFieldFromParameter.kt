package com.github.mfarsikov.kewt.example.ex15

import com.github.mfarsikov.kewt.annotations.Mapper

data class Person(val name: String)

data class Employee(val id: String, val name: String, val happy: Boolean)

@Mapper
interface PersonMapper {
    fun toEmployee(person: Person, id: String, happy: Boolean = true): Employee
}
