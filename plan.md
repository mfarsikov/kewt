# Plan
## Mandatory
* Check name matching, with same name in defferent sources (ambiguous)
  * with same name in different sources but with differnet types (not ambiguous)
* Explicit converter for any property `@Mapping(source = "x", target = "y", converter = "f")`
* Map target field from parameter
* Allow converter functions to use whole object
* Allow implicitly ignore properties with default values

## Nice to have
* Use setters if there is no value in constructor
* Preserve annotations in generated classes
* `@Generated` annotation
* import other converters 

## Something to think of
* Add errors to rendered file as comments?
* Generate extension functions? Companion objects?
* Log error (recommendations) if not all properties mapped
