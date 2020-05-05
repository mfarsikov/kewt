plugins{
    kotlin("jvm") version "1.3.71"
    id("com.jfrog.bintray") version "1.8.5"
}

repositories {
    mavenLocal()
    mavenCentral()
}

allprojects {
    version = "0.1.8"
}
