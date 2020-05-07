package com.github.mfarsikov.kewt.example.ex18

import com.github.mfarsikov.kewt.annotations.Mapper

data class Person(val name: String)

data class Employee(val name: String, val id: Int = 1)

@Mapper
interface PersonMapper18 {
    fun toEmployee(person: Person): Employee
}