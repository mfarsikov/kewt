package com.github.mfarsikov.kewt.example.ex21

import com.github.mfarsikov.kewt.annotations.Mapper

data class Person(val name:String)

data class KinderGarden(val children:List<Person>)

@Mapper
interface KindergardenMapper{
    fun toKindergarden(children: List<Person>): KinderGarden
}