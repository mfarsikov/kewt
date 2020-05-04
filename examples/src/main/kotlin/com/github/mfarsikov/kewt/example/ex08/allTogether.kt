package com.github.mfarsikov.kewt.example.ex08

import com.github.mfarsikov.kewt.annotations.Mapper


@Mapper
interface PersonMapper {
    fun toStudent(person: Person): Student

    fun toStudentId(passport: PersonPassport): StudentId
}


data class Person(
        val firstName: String,
        val lastName: String,
        val age: Int,
        val keys: List<String>,
        val dog: Dog,
        val passports: List<PersonPassport>
)

data class Student(
        val name: String,
        val lastName: String,
        val completeYears: Int,
        val keys: List<String>,
        val pet: Dog,
        val studentIds: List<StudentId>
)

data class Dog(val name: String)

data class PersonPassport(val passportNumber: String)


data class StudentId(val number: String)