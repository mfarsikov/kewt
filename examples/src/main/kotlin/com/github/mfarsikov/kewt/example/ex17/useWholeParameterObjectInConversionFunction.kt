package com.github.mfarsikov.kewt.example.ex17

import com.github.mfarsikov.kewt.annotations.Mapper
import java.util.*

data class Person(val uid: UUID)

data class Employee(val id: String)

@Mapper
interface PesronMapper {
    fun toEmployee(person: Person): Employee

    fun extractId(person: Person) //TODO fix
}
