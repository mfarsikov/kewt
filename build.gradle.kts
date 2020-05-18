import com.github.mfarsikov.kewt.versioning.plugin.Incrementer.MINOR
import com.github.mfarsikov.kewt.versioning.plugin.Incrementer.PATCH

plugins {
    kotlin("jvm") version "1.3.71" apply false
    id("com.jfrog.bintray") version "1.8.5" apply false
    id("com.github.mfarsikov.kewt-versioning") version "0.6.0"
}

repositories {
    mavenLocal()
    mavenCentral()
}
kewtVersioning {
    configuration {
        branches {
            clear()
            add {
                regexes = mutableListOf("master".toRegex())
                incrementer = MINOR
                stringify = stringifier(useBranch = false, useSha = false, useTimestamp = false)
            }
            add {
                regexes = mutableListOf("fix/.*".toRegex())
                incrementer = PATCH
                stringify = stringifier(useSha = false, useTimestamp = false)
            }
            add {
                regexes = mutableListOf(".*".toRegex())
                incrementer = MINOR
                stringify = stringifier(useSha = false, useTimestamp = false)
            }
        }
    }
}
val v = kewtVersioning.version

subprojects {
    version = v
}
