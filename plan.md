# Plan
## Mandatory
* Support Sets element mappings
  * against: if projection function is surjective, result set will lose elements
    * introduce annotation `@Injective`?
* allow mutations using setters? (allow Unit return type?) 

## Nice to have
* Preserve annotations in generated classes
* import other converters 
* validations:
  * function has return type
  * in case of patch -- return type matches @Target

## Something to think of
* Add errors to rendered file as comments?
* Generate extension functions? Companion objects?
* Log error (recommendations) if not all properties mapped
* Use setters if there is no value in constructor (Java?)
* Collection conversions (list-set)
