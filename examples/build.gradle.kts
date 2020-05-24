import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
    idea
}

repositories {
    mavenLocal()
    mavenCentral()
}

group = "com.example"
java.sourceCompatibility = JavaVersion.VERSION_1_8

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation(project(":kewt-map-annotations"))
    implementation(project(":examples-proto-lib"))

    kapt(project(":kewt-map-processor"))
    kapt(project(":examples-proto-lib"))

    implementation("io.grpc:grpc-protobuf:1.26.0")

    testImplementation("io.kotest:kotest-assertions-core-jvm:4.0.3")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.0.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

kapt {
    arguments {
//        arg("kewt.log.level", "debug")
 //       arg("kewt.whitelist", "com.github.mfarsikov.kewt.example.ex24")
//        arg("kewt.blacklist", "com.github.mfarsikov.kewt.example.proto")
    }
}
