import org.jetbrains.dokka.gradle.DokkaTask

plugins{
    kotlin("jvm")
    id ("org.jetbrains.dokka") version "0.10.1"
    id("maven-publish")
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
    this.archiveClassifier.set( "javadoc")
    from(tasks.dokka)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group as String
            artifactId = project.name
            version = project.version as String
            from(components["java"])
            artifact(dokkaJar)
        }
    }
}
