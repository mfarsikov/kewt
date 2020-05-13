@file:Mapper
package com.github.mfarsikov.kewt.example.ex20

import com.github.mfarsikov.kewt.annotations.Isomorphism
import com.github.mfarsikov.kewt.annotations.Mapper

data class Person(val name: String)
data class Employee(val name: String)



lateinit var toEmployee: Person.() -> Employee

fun main() {
    Person("").toEmployee()
}
