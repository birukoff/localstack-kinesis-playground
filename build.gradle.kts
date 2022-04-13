plugins {
    java
}

group = "org.example"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
    consistentResolution {
        useCompileClasspathVersions()
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("software.amazon.awssdk:bom:2.17.157"))

    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("software.amazon.awssdk:kinesis")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

fun addLocalstack() {
    dependencies {
        implementation(platform("com.amazonaws:aws-java-sdk-bom:1.12.186"))
        implementation(platform("org.testcontainers:testcontainers-bom:1.16.3"))

        testImplementation("org.testcontainers:junit-jupiter")
        testImplementation("org.testcontainers:localstack")
        testImplementation("org.testcontainers:testcontainers")

        testRuntimeOnly("com.amazonaws:aws-java-sdk-core") // required for org.testcontainers:localstack
    }
}

addLocalstack()

tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            showExceptions = true
            setExceptionFormat("full")
            events("passed", "failed", "skipped")
        }
    }
}
