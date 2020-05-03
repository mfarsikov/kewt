plugins {
    kotlin("jvm")
    id("maven-publish")
    id("groovy")
}

repositories {
    mavenLocal()
    mavenCentral()
}

group = "com.github.mfarsikov"
java.sourceCompatibility = JavaVersion.VERSION_1_8

dependencies {
    implementation("com.github.mfarsikov:kewt-annotations:$version")
    implementation("com.google.protobuf:protobuf-java:3.11.4")
    implementation("com.squareup:kotlinpoet:1.5.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.1.0")
    implementation(files("${System.getProperty("java.home")}/../lib/tools.jar"))

    testImplementation("io.kotest:kotest-assertions-core-jvm:4.0.3")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.0.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
    testImplementation("org.spockframework:spock-core")
    testImplementation(platform("org.spockframework:spock-bom:2.0-M1-groovy-2.5"))
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
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