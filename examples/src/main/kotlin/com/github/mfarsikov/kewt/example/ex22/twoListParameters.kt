package com.github.mfarsikov.kewt.example.ex22

import com.github.mfarsikov.kewt.annotations.Mapper

data class Person(val names: List<String>, val ages: List<Int>)
data class Employee(val empNames: List<String>, val empAges: List<Int>)

@Mapper
interface EmployeeMapper22 {
    fun toEmployee(person: Person): Employee
    fun toEmployee(names: List<String>, ages: List<Int>): Employee
}