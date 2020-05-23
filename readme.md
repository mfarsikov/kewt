# Kewt - Kotlin cute data-class mapping tool

inspired by [Mapstruct](https://mapstruct.org/)

## Quick start

`build.gradle.kts`:
```kotlin
plugins {
    kotlin("kapt") version "1.3.72" //kotlinVersion
}
repositories {
    jcenter()
}
dependencies {
    implementation("com.github.mfarsikov:kewt-annotations:0.6.0")
    kapt("com.github.mfarsikov:kewt-map-processor:0.6.0")
}
```

Having data classes:
```kotlin
data class Person(val id: UUID, val name: String, val age: Int)
data class Employee(val id: UUID, val name: String, val age: Int)
```

Create interface 
```kotlin
import com.github.mfarsikov.kewt.annotations.Mapper

@Mapper
interface PersonMapper {
    fun toEmployee(person: Person): Employee
}
```
Kewt generates this code:
```kotlin
@Generated
class PersonMapperImpl : PersonMapper {
  override fun toEmployee(person: Person) = Employee(
      age = person.age,
      id = person.id,
      name = person.name
  )
}
```

## Mapping strategies

### General concepts
* Classes can be mapped if each target field have found a single source.
* Mapping cannot be done if any target property can be mapped from more than one source. 
* In the same time it is allowed to have not mapped (extra) sources.

### Implicitly by type, if it is not ambiguous
Type matched if:
* Types are identical:
  * `String => String`
  * `Employee => Employee`
  * `List<String> => List<String>`
* Exist conversion function (abstract or not): 
  * `String => Int` if `(String) -> Int` conversion function provided
  * `Person => Employee` if `(Person) -> Employee` conversion function provided
  * `List<String> => List<Int>` if `(String) -> Int` conversion function provided
  * `List<Person> => List<Employee>` if `(Person) -> Employee` conversion function provided
#### Example

##### Type match

```kotlin
data class Person  (val personId: UUID,   val personName: String,   val personAge: Int  )
data class Employee(val employeeId: UUID, val employeeName: String, val employeeAge: Int)
```
There is only one way to map properties by their types.

Generated code:
```kotlin
@Generated
class PersonMapperImpl : PersonMapper {
    override fun toEmployee(person: Person) = Employee(
        employeeId = person.personId,
        employeeName = person.personName,
        employeeAge = person.personAge
    )
}
```

##### Negative example (ambiguous mapping):
```kotlin
data class Person  (val key: UUID, userId: UUID)
data class Employee(val id: UUID,  cardId: UUID)
```

These classes cannot be mapped implicitly, because there is ambiguity in fields mapping, all four variants are valid:
`key = id`, `key = cardId`, `userId = id`, `userId = cardId`

To solve this see [Explicit mapping](#explicit-mapping)

##### Type match using a converter function

```kotlin
data class Person  (val personId: String)
data class Employee(val employeeId: UUID)
```
Note that names are not matched and types are different.
```kotlin
@Mapper
interface PersonMapper{

    fun toEmployee(person: Person): Employee 
    
    fun toUuid(s: String) = UUID.fromString(s) // Non-abstract converter (String) -> UUID
}
```
Kewt maps fields by type using conversion function:
```kotlin
@Generated
class PersonMapperImpl : PersonMapper {
    override fun toEmployee(person: Person) = Employee(
        employeeId = toUuid(person.personId)
    )
}
```

### Implicitly by name, if it is not ambiguous
If source property name and target property names are the same - fields match. This strategy could fail if provided more
than one source with the same parameter name, and the same parameter type.
#### Example 
```kotlin
data class Person  (val id: UUID, userId: UUID)
data class Employee(val id: UUID, userId: UUID)
```

### Explicitly in `@Mapping` annotation
Has highest priority. Should be avoided in favor of implicit strategies.
#### Example
##### Explicit mapping
```kotlin
data class Person  (val key: UUID, userId: UUID)
data class Employee(val id: UUID,  cardId: UUID)
```

```kotlin
interface PersonMapper {
    @Mappings([
        Mapping(source="key",    target="id"    ),
        Mapping(source="userId", target="cardId")
    ])  
    fun toEmployee(person: Person): Employee
}
```

It is not required to specify all the fields, it is enough just to resolve ambiguity, the rest kewt maps automatically: 
```kotlin
interface PersonMapper {
    @Mappings([
        Mapping(source="key", target="id")
    ])  
    fun toEmployee(person: Person): Employee
}
```

##### Explicit converter

```kotlin
data class Person  (val id: String)
data class Employee(val id: UUID  )
```
There are two functions `(String) -> UUID` and to solve this ambiguity `converter` explicitly:
```kotlin

interface PersonMapper {

    fun parseUuid(string: String): UUID = UUID.fromString(string) 
    fun invalidConverter(string: String): UUID = throw RuntimeException()

    @Mappings([
        Mapping(source="id", target="id", converter="parseUuid")
    ])  
    fun toEmployee(person: Person): Employee
}
```

### Examples
Working examples can be found in `examples` sub-project. To build them run `./gradlew build`
Generated code is under `build/generated/source/kapt/main`.

## Field lifting
```kotlin
data class Person(val name: Name)
data class Name(val firstName: String)

data class Employee(val name: String)

@Mapper
interface PersonMapper {
    @Mappings([
        Mapping(source = "person.name.firstName", target = "name")
    ])
    fun toEmployee(person: Person): Employee
}
```
Generated code:
```kotlin
@Generated
class PersonMapperImpl : PersonMapper {
  override fun toEmployee(person: Person) = Employee(
      name = person.name.firstName
  )
}
```

## Map collection elements
```kotlin

data class Person(val ids: List<UUID>)
data class Employee(val ids: List<String>)

@Mapper
interface PersonMapper {
    fun toEmployee(person: Person): Employee
    fun uuidToString(uuid: UUID): String = uuid.toString()
}
```

Generated code: 
```kotlin
@Generated
class PersonMapperImpl : PersonMapper {
  override fun toEmployee(person: Person): Employee = Employee(
      ids = person.ids.map { uuidToString(it) }
  )
}
```

## Protobuf
Kewt can map protobuf classes using their builders.

Add library with generated protobuf classes to `kapt` configurtion.

## Dependencies
All libraries used in mappers should be added to `kapt` configuration:
```kotlin 
dependencies {
    kapt("com.nowhere:my-library:1.0.0")
}
```

## Configuration
```kotlin
kapt {
    arguments {          
        arg("kewt.generate.spring", "true")
        arg("kewt.log.level", "debug")
        arg("kewt.whitelist", "com.github.mfarsikov.kewt.example.proto.ex02")
        arg("kewt.blacklist", "com.github.mfarsikov.kewt.example.proto")
    }
}           
```

### Spring
```kotlin
arg("kewt.generate.spring", "true") 
```
Add a `@org.springframework.stereotype.Component` to the generated class.

### Debugging
#### Logging
```kotlin
arg("kewt.log.level", "debug")
```
Values: `error`, `warn`, `debug`, `trace`. Default: `warn`

#### White/black listing packages
```kotlin
arg("kewt.whitelist", "com.github.mfarsikov.kewt.example.proto.ex02")
arg("kewt.blacklist", "com.github.mfarsikov.kewt.example.proto")
```
