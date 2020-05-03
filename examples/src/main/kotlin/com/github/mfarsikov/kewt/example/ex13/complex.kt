package com.github.mfarsikov.kewt.example.ex13

import com.github.mfarsikov.kewt.annotations.Mapper
import com.github.mfarsikov.kewt.annotations.Mapping
import com.github.mfarsikov.kewt.annotations.Mappings


@Mapper
interface RenderModelMapper {

    fun convert(person: Person): Employee

    @Mappings([
        Mapping(source = "pet.name.firstName", target = "petFirstName"),
        Mapping(source = "pet.name.lastName", target = "petLastName")
    ])
    fun convert(pet: Pet): EmployeesPet
}


data class Person(
        val name: String
)

data class Employee(
        val name: String
)


data class Pet(
        val name: PetName
)

data class PetName(
        val firstName: String,
        val lastName: String
)

data class EmployeesPet(
        val petFirstName: String,
        val petLastName: String
)
