package com.github.mfarsikov.kewt.example.ex5

import com.github.mfarsikov.kewt.annotations.Mapper
import java.util.*

data class Person(val ids: List<UUID>)
data class Employee(val ids: List<String>)

@Mapper
interface PersonMapper {
    fun toEmployee(person: Person): Employee
    fun uuidToString(uuid: UUID): String = uuid.toString()
}