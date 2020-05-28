plugins {
    kotlin("jvm")
    id("maven-publish")
    id("groovy")
    id("com.jfrog.bintray")
}

repositories {
    mavenLocal()
    mavenCentral()
}

group = "com.github.mfarsikov"
java.sourceCompatibility = JavaVersion.VERSION_1_8

dependencies {
    implementation(project(":kewt-map-annotations"))

    implementation("com.google.protobuf:protobuf-java:3.11.4")
    implementation("com.squareup:kotlinpoet:1.5.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.2.0")
    implementation(files("${System.getProperty("java.home")}/../lib/tools.jar"))

    testImplementation("io.kotest:kotest-assertions-core-jvm:4.3.0")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.3.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
    testImplementation("org.spockframework:spock-core")
    testImplementation(platform("org.spockframework:spock-bom:2.0-M1-groovy-2.5"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

task("generateVersionFile") {
    val buildInfoFile = file("$buildDir/generated/resources/META-INF/build-info.properties")
    inputs.property("version", project.version)
    outputs.file(buildInfoFile)
    doLast {
        file(buildInfoFile.parent).mkdirs()
        buildInfoFile.writeText("version=${project.version}")
    }
}

tasks.withType<Jar>().named("jar") {
    dependsOn("generateVersionFile")
    from("$buildDir/generated/resources")
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("bintray") {
            groupId = project.group as String
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])

            pom {
                name.set("Kewt mapper")
                description.set("Data class mapper for Kotlin")
                url.set("https://github.com/mfarsikov/kewt")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        name.set("Max Farsikov")
                        email.set("farsikovmax@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/mfarsikov/kewt.git")
                    developerConnection.set("scm:git:ssh://github.com/mfarsikov/kewt.git")
                    url.set("https://github.com/mfarsikov/kewt")
                }
            }

        }
    }
}

bintray {
    user = System.getenv("BINTRAY_USER")
    key = System.getenv("BINTRAY_KEY")
    setPublications("bintray")
    isPublish = true
    with(pkg) {
        repo = "kewt-map"
        name = "kewt-map-processor"
        userOrg = System.getenv("BINTRAY_USER")
        setLicenses("Apache-2.0")
        vcsUrl = "https://github.com/mfarsikov/kewt"
        with(version) {
            name = project.version.toString()
            desc = "Kotlin data class mapping annotations processor"
            //released = yyyy-MM-dd'T'HH:mm:ss.SSSZZ
        }
    }
}
