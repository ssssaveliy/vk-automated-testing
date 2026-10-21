import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm") version "2.4.21"
}

group = "ru.homework"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val junitVersion = "6.1.3"
val restAssuredVersion = "6.2.0"
val evoMasterVersion = "6.2.0"

dependencies {
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation(
        "io.rest-assured:rest-assured:$restAssuredVersion"
    )

    testImplementation(
        "org.evomaster:evomaster-client-java-controller:$evoMasterVersion"
    )

    testImplementation(
        "org.evomaster:evomaster-test-utils-java:$evoMasterVersion"
    )
}

kotlin {
    jvmToolchain(25)

    sourceSets.test {
        kotlin.srcDir("generated/evomaster")
    }
}

sourceSets.test {
    resources.srcDir("api")
}

tasks.test {
    useJUnitPlatform()

    outputs.upToDateWhen { false }
    outputs.cacheIf { false }

    maxParallelForks = 1
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
    }
}

val generatorJava = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.register<Exec>("generateApiTests") {
    group = "verification"
    description = "Generate Kotlin tests from the SWAPI OpenAPI specification"

    workingDir(rootDir)

    doFirst {
        val generatorJar = file("tools/evomaster.jar")
        val specification = file("api/openapi-swapi.yaml")

        check(generatorJar.isFile) {
            "Download EvoMaster 6.2.0 to tools/evomaster.jar"
        }

        check(specification.isFile) {
            "Missing api/openapi-swapi.yaml"
        }

        commandLine(
            generatorJava.get().executablePath.asFile.absolutePath,
            "-jar", generatorJar.absolutePath,
            "--blackBox", "true",
            "--schema", specification.absolutePath,
            "--outputFormat", "KOTLIN_JUNIT_5",
            "--outputFolder", file("generated/evomaster").absolutePath,
            "--outputFilePrefix", "ru.homework.generated.Swapi",
            "--maxTime", "60s",
            "--ratePerMinute", "60",
            "--header0", "Accept:application/json",
            "--allowInvalidData", "false"
        )
    }
}