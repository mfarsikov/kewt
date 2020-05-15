package com.github.mfarsikov.kewt.processor

import com.github.mfarsikov.kewt.processor.mapper.MapperConversionFunction
import com.github.mfarsikov.kewt.processor.mapper.TypeMatcher
import spock.lang.Specification
import spock.lang.Unroll

class TypeMatcherTest extends Specification {

    @Unroll
    def "using conversion function #n"(int n, String from, String funcParam, String returnType, String to, Boolean success) {
        expect:
        def res = new TypeMatcher([new MapperConversionFunction(
                "f",
                new Parameter("x", toType(funcParam)),
                toType(returnType)
        )]).findConversion(
                toType(from),
                toType(to),
                null
        )

        (res != null) == success

        where:
        n  | from                                      | funcParam        | returnType    | to                                     | success
        1  | "kotlin.String"                           | "kotlin.String"  | "kotlin.Int"  | "kotlin.Int"                           | true
        2  | "kotlin.String"                           | "kotlin.String?" | "kotlin.Int"  | "kotlin.Int"                           | true
        3  | "kotlin.String"                           | "kotlin.String?" | "kotlin.Int?" | "kotlin.Int"                           | false
        4  | "kotlin.String?"                          | "kotlin.String"  | "kotlin.Int"  | "kotlin.Int"                           | false
        5  | "kotlin.String?"                          | "kotlin.String"  | "kotlin.Int"  | "kotlin.Int?"                          | true
        6  | "kotlin.String?"                          | "kotlin.String"  | "kotlin.Int?" | "kotlin.Int"                           | false
        7  | "kotlin.String?"                          | "kotlin.String?" | "kotlin.Int?" | "kotlin.Int"                           | false
        8  | "kotlin.String?"                          | "kotlin.String?" | "kotlin.Int"  | "kotlin.Int?"                          | true
        9  | "kotlin.String?"                          | "kotlin.String?" | "kotlin.Int?" | "kotlin.Int?"                          | true
        10 | "kotlin.collections.List<kotlin.String>"  | "kotlin.String"  | "kotlin.Int"  | "kotlin.collections.List<kotlin.Int>"  | true
        11 | "kotlin.collections.List<kotlin.String>?" | "kotlin.String"  | "kotlin.Int"  | "kotlin.collections.List<kotlin.Int>?" | true
        12 | "kotlin.collections.List<kotlin.String>?" | "kotlin.String"  | "kotlin.Int"  | "kotlin.collections.List<kotlin.Int>"  | false
    }

    @Unroll
    def "direct assignment #n"(int n, String from, String to, Boolean success) {
        expect:
        def res = new TypeMatcher([]).findConversion(
                toType(from),
                toType(to),
                null
        )
        (res != null) == success

        where:
        n  | from                                   | to                                            | success
        1  | "kotlin.Int"                           | "kotlin.Int"                                  | true
        2  | "kotlin.Int?"                          | "kotlin.Int"                                  | false
        3  | "kotlin.Int"                           | "kotlin.Int?"                                 | true
        4  | "kotlin.Int?"                          | "kotlin.Int?"                                 | true
        5  | "kotlin.collections.List<kotlin.Int>"  | "kotlin.collections.List<kotlin.Int>"         | true
        6  | "kotlin.collections.List<kotlin.Int>?" | "kotlin.collections.List<kotlin.Int>?"        | true
        7  | "kotlin.collections.List<kotlin.Int>?" | "kotlin.collections.List<kotlin.Int>"         | false
        8  | "kotlin.collections.List<kotlin.Int>"  | "kotlin.collections.List<kotlin.Int>?"        | true
        9  | "kotlin.collections.List<kotlin.Int?>" | "kotlin.collections.List<kotlin.Int>"         | false
        10 | "kotlin.collections.List<kotlin.Int>"  | "kotlin.collections.List<kotlin.Int?>"        | true
        11 | "kotlin.collections.List<kotlin.Int?>" | "kotlin.collections.List<kotlin.Int?>"        | true
        12 | "kotlin.collections.List<kotlin.Int>?" | "kotlin.collections.List<kotlin.Int>"         | false
        13 | "java.lang.Integer"                    | "kotlin.Int"                                  | true
        14 | "java.lang.Integer!"                   | "kotlin.Int"                                  | true
        15 | "java.util.List<kotlin.Int>"           | "kotlin.collections.List<java.lang.Integer>"  | true
        16 | "java.util.List<kotlin.Int>!"          | "kotlin.collections.List<java.lang.Integer>?" | true
        17 | "java.lang.Integer!"                   | "java.lang.Integer!"                          | true
        18 | "java.lang.Integer?"                   | "java.lang.Integer!"                          | true
        19 | "java.lang.String!"                    | "java.lang.Integer!"                          | false
        20 | "java.lang.String!"                    | "java.lang.String!"                           | true
        21 | "long!"                                | "long!"                                       | true
    }

    static Type toType(String s) {
        def m = s =~ '(([\\w.]*)\\.)?(\\w*)(<(([\\w*.]*)\\.)?(\\w*)([\\?!]?)>)?([\\?!]?)'

        String pkg = m[0][2] ?: ""
        String type = m[0][3]
        String typeParamPkg = m[0][6]
        String typeParam = m[0][7]
        String typeParamNullSign = m[0][8]
        String typeNullSign = m[0][9]

        Map<String, Nullability> signToNullability = ["?": Nullability.@NULLABLE, "!": Nullability.@PLATFORM, "": Nullability.@NON_NULLABLE]


        Nullability subtypeNullability = signToNullability[typeParamNullSign]

        List<Type> subtypes = []
        if (typeParam != null) {
            subtypes = [new Type(typeParamPkg, typeParam, subtypeNullability, [])]
        }

        Nullability typeNullability = signToNullability[typeNullSign]
        def r = new Type(
                pkg,
                type,
                typeNullability,
                subtypes
        )
        return r
    }
}
