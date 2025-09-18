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

    // OkHttp3 dependencies
    implementation(platform(libs.okhttp3.bom))
    implementation(libs.okhttp3)

    // Coroutines
    implementation(libs.jetbrains.kotlinx.coroutines.core)

    // Serialization (for data classes)
    implementation(libs.jetbrains.kotlinx.serialization.json)

    // Testing dependencies
    testImplementation(libs.junit)
    testImplementation(libs.jetbrains.kotlinx.coroutines.test)
    testImplementation(platform(libs.okhttp3.bom))
    testImplementation("com.squareup.okhttp3:mockwebserver")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
}