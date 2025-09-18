plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Core module dependency
    implementation(project(":core"))

    // Ktor Client dependencies
    implementation(libs.ktor.client.core)

    // Coroutines
    implementation(libs.jetbrains.kotlinx.coroutines.core)

    // Serialization (for data classes)
    implementation(libs.jetbrains.kotlinx.serialization.json)

    // Testing dependencies
    testImplementation(libs.junit)
    testImplementation(libs.jetbrains.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
}