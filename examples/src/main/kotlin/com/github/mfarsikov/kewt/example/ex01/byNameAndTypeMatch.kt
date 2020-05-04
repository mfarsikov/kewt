package com.github.mfarsikov.kewt.example.ex01

import com.github.mfarsikov.kewt.annotations.Mapper
import java.util.*


data class Person(val id: UUID, val name: String, val age: Int)

data class Employee(val id: UUID, val name: String, val age: Int)

@Mapper
interface PersonMapper {
    fun toEmployee(person: Person): Employee
}
