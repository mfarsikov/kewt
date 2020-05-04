package com.github.mfarsikov.kewt.example.ex02

import com.github.mfarsikov.kewt.annotations.Mapper
import java.util.*

data class Person(val id: UUID, val licenceId: UUID)
data class Employee(val id: String, val licenceId: String)

@Mapper
interface PersonMapper {
    fun toEmployee(person: Person): Employee

    fun uuidToString(uuid: UUID): String = uuid.toString()
}
