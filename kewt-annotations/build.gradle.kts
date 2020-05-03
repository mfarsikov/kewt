plugins{
    kotlin("jvm")
    id("maven-publish")
}

repositories {
    mavenCentral()
}

group = "com.github.mfarsikov"
java.sourceCompatibility = JavaVersion.VERSION_1_8

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group as String
            artifactId = project.name
            version = project.version as String

            from(components["java"])
        }
    }
}
