plugins{
    kotlin("jvm") version "1.3.71" apply false
    id("com.jfrog.bintray") version "1.8.5" apply false
}

repositories {
    mavenLocal()
    mavenCentral()
}

allprojects {
    version = "0.1.10"
}
