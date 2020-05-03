package com.github.mfarsikov.kewt.example.proto.ex1

import com.github.mfarsikov.kewt.annotations.Mapper
import com.github.mfarsikov.kewt.annotations.Mapping
import com.github.mfarsikov.kewt.annotations.Mappings
import protohub.Employee
import protohub.ID
import protohub.Passport
import protohub.Person


@Mapper
interface PersonConverter {
    @Mappings([
    Mapping(source = "name.name" , target = "firstName"),
    Mapping(source = "name.surname" , target = "lastName")
    ])
    fun convert(person: Person): Employee
    fun intToString(x:Int) = x.toString()
    fun toId(passport: Passport): ID
}

