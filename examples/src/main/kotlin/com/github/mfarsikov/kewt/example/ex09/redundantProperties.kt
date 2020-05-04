package com.github.mfarsikov.kewt.example.ex09

import com.github.mfarsikov.kewt.annotations.Mapper
import java.util.*

data class Person(val name: String, val surname: String, val id: UUID)
data class Employee(val name: String, val surname: String)

@Mapper
interface PersonMapper {
    fun toEmployee(person: Person): Employee
}

