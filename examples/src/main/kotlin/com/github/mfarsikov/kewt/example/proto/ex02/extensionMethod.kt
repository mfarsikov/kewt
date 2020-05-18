@file:Mapper


package com.github.mfarsikov.kewt.example.proto.ex02

import com.github.mfarsikov.kewt.annotations.Mapper
import com.github.mfarsikov.kewt.annotations.Mapping
import com.github.mfarsikov.kewt.annotations.Mappings
import protohub.Employee
import protohub.ID
import protohub.Passport
import protohub.Person


@Mappings([
    Mapping(source = "name.name", target = "firstName"),
    Mapping(source = "name.surname", target = "lastName")
])
lateinit var convert: Person.() -> Employee
fun intToString(x: Int) = x.toString()
lateinit var toId: Passport.() -> ID


data class MyEmployee(val ids: List<String>)

lateinit var toMyEmployee: Employee.() -> MyEmployee
