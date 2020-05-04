package com.github.mfarsikov.kewt.example.ex04

import com.github.mfarsikov.kewt.annotations.Mapper
import java.util.*

data class Person(val id: UUID)
data class Employee(val personId: String)

@Mapper
interface PersonMapper {
    fun toEmployee(person: Person): Employee
    fun uuidToString(uuid: UUID): String = uuid.toString()
}