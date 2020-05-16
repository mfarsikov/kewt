import com.github.mfarsikov.kewt.versioning.plugin.BranchConfig
import java.time.ZoneId

plugins {
    kotlin("jvm") version "1.3.71" apply false
    id("com.jfrog.bintray") version "1.8.5" apply false
    id("com.github.mfarsikov.kewt-versioning") version "0.4.0-dirty"
}

repositories {
    mavenLocal()
    mavenCentral()
}
kewtVersioning {
    branches = mutableListOf(
            BranchConfig().apply {
                regexes = mutableListOf("master".toRegex())
                stringify = smartVersionStringifier(useBranch = false)
            },
            BranchConfig().apply {
                regexes = mutableListOf(".*".toRegex())
                stringify = smartVersionStringifier(useTimestamp = true, zoneId = ZoneId.of("Europe/Kiev"))
            }
    )
}
val v = kewtVersioning.version
subprojects {
    version = v
}
