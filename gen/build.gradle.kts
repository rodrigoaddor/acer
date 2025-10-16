plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.devtools.ksp", "symbol-processing-api", "2.2.20-2.0.4")
    implementation("com.squareup", "kotlinpoet-ksp", "2.2.0")
}
