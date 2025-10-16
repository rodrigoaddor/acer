val transitiveInclude: Configuration by configurations.creating {
    exclude(group = "org.jetbrains.kotlin")
    exclude(group = "org.jetbrains.kotlinx")
    exclude(group = "com.mojang")
}

plugins {
    idea
    id("fabric-loom") version "1.11-SNAPSHOT"
    id("com.google.devtools.ksp") version "2.2.20-2.0.4"

    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

base {
    val archivesBaseName: String by project
    archivesName.set(archivesBaseName)
}

val modVersion: String by project
val mavenGroup: String by project
version = modVersion
group = mavenGroup

val minecraftVersion: String by project
val yarnMappings: String by project
val loaderVersion: String by project
val fabricVersion: String by project
val fabricKotlinVersion: String by project
val ktorVersion: String by project

dependencies {
    minecraft("com.mojang", "minecraft", minecraftVersion)
    mappings("net.fabricmc", "yarn", yarnMappings, null, "v2")
    modImplementation("net.fabricmc", "fabric-loader", loaderVersion)

    modImplementation("net.fabricmc.fabric-api", "fabric-api", fabricVersion)

    include(modImplementation("net.fabricmc", "fabric-language-kotlin", fabricKotlinVersion))
    include(modImplementation("me.lucko", "fabric-permissions-api", "0.5.0"))

    transitiveInclude(implementation("com.charleskorn.kaml", "kaml", "0.55.0"))

    include(implementation("io.ktor", "ktor-client-core", ktorVersion))
    transitiveInclude(implementation("io.ktor", "ktor-client-cio", ktorVersion))
    transitiveInclude(implementation("io.ktor", "ktor-client-content-negotiation", ktorVersion))
    transitiveInclude(implementation("io.ktor", "ktor-serialization-kotlinx-json", ktorVersion))

    ksp(implementation(project(":gen"))!!)

    transitiveInclude.resolvedConfiguration.resolvedArtifacts.forEach {
        include(it.moduleVersion.id.toString())
    }
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }

    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

idea {
    module {
        val file = file("build/generated/ksp/main/kotlin/")
        sourceDirs = sourceDirs + file
        generatedSourceDirs = generatedSourceDirs + file
    }
}

tasks {
    processResources {
        inputs.properties(
            "version" to project.version
        )

        filesMatching("fabric.mod.json") {
            expand("version" to project.version)
        }
    }

    val javaVersion = JavaVersion.VERSION_21
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(javaVersion.toString().toInt())
    }

    java {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        withSourcesJar()
    }
}