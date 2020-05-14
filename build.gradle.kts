plugins{
    kotlin("jvm") version "1.3.71" apply false
    id("com.jfrog.bintray") version "1.8.5" apply false
    id("com.github.mfarsikov.kewt-versioning") version "0.2.0"
}

repositories {
    mavenLocal()
    mavenCentral()
}
val v = kewtVersioning.version
subprojects {
    version = v
}
