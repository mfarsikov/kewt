package com.github.mfarsikov.kewt.example.proto.ex03

import com.github.mfarsikov.kewt.annotations.Mapper
import com.github.mfarsikov.kewt.annotations.Mapping
import com.github.mfarsikov.kewt.annotations.Mappings
import protohub.Employee
import protohub.ID
import protohub.Passport
import protohub.Person

@Mapper
interface PersonMapper03 {
    @Mappings([
        Mapping(source = "person.name.name", target = "firstName"),
        Mapping(source = "person.name.surname", target = "lastName")
    ])
    fun convert(person: Person, ids: List<String>): Employee

    fun toId(passport: Passport): ID

}
