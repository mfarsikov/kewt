import org.jetbrains.dokka.gradle.DokkaTask
import java.time.LocalDate

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") version "0.10.1"
    id("maven-publish")
    id("com.jfrog.bintray")
}

repositories {
    mavenCentral()
    jcenter()
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

tasks {
    val dokka by getting(DokkaTask::class) {
        outputFormat = "javadoc"
        outputDirectory = "$buildDir/javadoc"
    }
}

java {
    withSourcesJar()
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    this.archiveClassifier.set("javadoc")
    from(tasks.dokka)
}


publishing {
    publications {
        create<MavenPublication>("bintray") {
            groupId = project.group as String
            artifactId = project.name
            version = project.version.toString()
            from(components["java"])
            artifact(dokkaJar)

            pom {
                name.set("Kewt mapper annotations")
                description.set("Annotations for data class mapper for Kotlin")
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
        name = "kewt-map-annotations"
        userOrg = System.getenv("BINTRAY_USER")
        setLicenses("Apache-2.0")
        vcsUrl = "https://github.com/mfarsikov/kewt"
        with(version) {
            name = project.version.toString()
            desc = "Annotations for Kotlin data class mapping"
           // released = LocalDate.now().toString()
        }
    }
}
