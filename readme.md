# Kewt - Kotlin cute data-class mapping tool

inspired by [Mapstruct](https://mapstruct.org/)

## Quick start
`./gradlew publishToMavenLocal`

`build.gradle.kts`:
```kotlin
plugins {
    kotlin("kapt") version "1.3.71" //kotlinVersion
}
repositories {
    mavenLocal()
}
dependencies {
    implementation("com.github.mfarsikov:kewt-annotations:0.1.6-SNAPSHOT")
    kapt("com.github.mfarsikov:kewt-processor:0.1.6-SNAPSHOT")
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
Kewt will generate this code:
```kotlin
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
Classes can be mapped if each target field have found a single source.
Mapping cannot be done if any target property can be mapped from more than one source. 
In the same time it is allowed for sources to have not mapped (extra) fields.

### Mapping
Field mapping is possible if their type are the same, or they are convertible.

#### Type matching
Type matched if:
* Types are identical:
  * `String => String`
  * `Employee => Employee`
  * `List<String> => List<String>`
* Exist conversion function: 
  * `String => Int` if `(String) -> Int` conversion function provided
  * `Person => Employee` if `(Person) -> Employee` conversion function provided
  * `List<String> => List<Int>` if `(String) -> Int` conversion function provided
  * `List<Person> => List<Employee>` if `(Person) -> Employee` conversion function provided
  
#### Field mapping
##### Explicitly in `@Mapping` annotation
Has highest priority. Should be avoided in favor of implicit strategies.
##### Implicitly by name, if it is not ambiguous
If source property name and target property names are the same - fields match. This strategy could fail if provided more
than one source with the same parameter name, and the same parameter type.
##### Implicitly by type, if it is not ambiguous
All fields not matched by explicit and implicit name matching could be matched by type, if type matching is not ambiguous.

## Examples
Working examples can be found in `examples` sub-project. To build them run `./gradlew build`

### Implicit strategies
#### Name and type match
```kotlin
data class Person(val id: UUID, val name: String, val age: Int)
data class Employee(val id: UUID, val name: String, val age: Int)
```

Two classes have the same field names and their types. There is only one way of mapping in this case.

#### Names match and there is appropriate type-converter

```kotlin
data class Person(val id: String, accountId: String)
data class Employee(val id: UUID, accountId: UUID)
```

```kotlin
@Mapper
interface PersonMapper{

    fun toEmployee(person: Person): Employee 
    
    fun toUuid(s: String) = UUID.fromString(s) // Non-abstract converter (String) -> UUID
}
```
Kewt will map fields by name, and use conversion the function:
```kotlin
@Generated
class PersonMapperImpl : PersonMapper {
    override fun toEmployee(person: Person) = Employee(
        id = toUuid(person.id),
        accountId = toUuid(person.accountId)
    )
}
```

#### Type match and there is only one pair of fields could be matched

despite name is not matched there is a way to map fields based on their types. 

```kotlin
data class Person(val key: UUID)
data class Employee(val id: UUID)
```

Generated code:
```kotlin
@Generated
class PersonMapperImpl : PersonMapper {
    override fun toEmployee(person: Person) = Employee(id = person.key)
}
```

Negative example:

```kotlin
data class Person(val key: UUID, userId: UUID)
data class Employee(val id: UUID, cardId: UUID)
```

These classes cannot be mapped implicitly, because there is ambiguity in fields mapping, all four variants are valid:
`key = id`, `key = cardId`, `userId = id`, `userId = cardId`


#### There is appropriate type converter and only one pair of fields matched
```kotlin
data class Person(val id: UUID)
data class Employee(val code: String)
@Mapper
interface PersonMapper{
        fun toEmployee(person: Person): Employee 
        
        fun convertToString(uuid: UUID) = uuid.toString() 
}
```
despite fields `id` and `code` have different name, there is a function for converting `UUID` to `String` //TODO

### Explicit mapping
TODO
## Protobuf
TODO

## Configuration
TODO Spring, log, white/black listing
