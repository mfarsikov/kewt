plugins{
    kotlin("jvm") version "1.3.71" apply false
    id("com.jfrog.bintray") version "1.8.5" apply false
    id("com.github.mfarsikov.kewt-versioning") version "0.1.1-SNAPSHOT" apply false
}

repositories {
    mavenLocal()
    mavenCentral()
}
