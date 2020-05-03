import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.google.protobuf") version "0.8.8"
    idea
}


repositories {
    mavenLocal()
    mavenCentral()
}

group = "com.github.mfarsikov"
java.sourceCompatibility = JavaVersion.VERSION_1_8

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("io.grpc:grpc-protobuf:1.26.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.10.0"
    }
}
