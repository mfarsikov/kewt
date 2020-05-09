# Plan
## Mandatory
* Preserve annotations in generated classes
* import other converters 
  * create an instance
  * static functions could be used in case of generating extension funcitons 

## Nice to have
* validations:
  * function has return type
* allow mutations using setters? (allow Unit return type?) 
* Generate extension functions? Companion objects?
  * scan for annotated vareables (just to avoid using interfaces) `@Mapping lateinit var toEmployee: Person.() -> Employee`
  * `@Isomorphism` generate extension functions in two directions
  
## Something to think of
* Add errors to rendered file as comments?
* Log error (recommendations) if not all properties mapped
* Use setters if there is no value in constructor (Java?)
* Collection conversions:
  * list-set
  * Support Sets element mappings
    * against: if projection function is surjective, result set will lose elements
    * introduce annotation `@Injective`?
* rename `@Target` to `@DefaultSource`?
