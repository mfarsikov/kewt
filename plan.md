# Plan
## Mandatory
* Explicit converter for any property `@Mapping(source = "x", target = "y", converter = "f")`
* Map target field from function parameter 
  * parameter with default value could solve problem with constant values
  * Allow converter functions to use whole object
* Allow implicitly ignore properties with default values
* Support Sets element mappings 
  * against: if projection function is surjective, result set will lose elements
    * introduce annotation `@Injective`?

## Nice to have
* Preserve annotations in generated classes
* import other converters 

## Something to think of
* Add errors to rendered file as comments?
* Generate extension functions? Companion objects?
* Log error (recommendations) if not all properties mapped
* Use setters if there is no value in constructor (Java?)
