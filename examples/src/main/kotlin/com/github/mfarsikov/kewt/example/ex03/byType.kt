package com.github.mfarsikov.kewt.example.ex03

import com.github.mfarsikov.kewt.annotations.Mapper
import java.util.*

data class Person(val id: UUID)
data class Employee(val personId: UUID)

@Mapper
interface PersonMapper {
    fun toEmployee(person: Person): Employee
}

