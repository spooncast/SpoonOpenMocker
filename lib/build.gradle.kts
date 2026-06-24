plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.kapt)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.jetbrains.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.parcelize)
    id("maven-publish")
    jacoco
}

android {
    namespace = "net.spooncast.openmocker.lib"
    compileSdk = 35

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeCompiler {
        enableStrongSkippingMode = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.useJUnitPlatform()
            }
        }
    }
}

dependencies {
    // AndroidX
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    // Activity Compose
    implementation(libs.androidx.activity.compose)

    // Test
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit5.vintage.engine)

    // Okhttp
    implementation(libs.okhttp3)
    implementation(libs.okhttp3.logging.interceptor)

    // Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.http)
    implementation(libs.ktor.utils)
    implementation(libs.ktor.client.mock)
    implementation(libs.ktor.client.json)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.serialization)
    testImplementation(libs.ktor.client.mock)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Serialization
    implementation(libs.jetbrains.kotlinx.serialization.json)

    // Gson
    implementation(libs.gson)

    // Navigation Compose
    implementation(libs.navigation.compose)
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        html.required.set(true)
        xml.required.set(true)
    }

    // Compose UI / 자동 생성 코드는 단위 테스트 대상이 아니므로 커버리지 집계에서 제외한다.
    val excludes = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*_Factory.*",
        "**/ui/**",
        "**/*\$Companion*.*",
        "**/ComposableSingletons*.*",
        "**/*\$\$serializer.*"
    )

    val kotlinClasses = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        setExcludes(excludes)
    }

    sourceDirectories.setFrom(files("${projectDir}/src/main/java"))
    classDirectories.setFrom(files(kotlinClasses))
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) {
            setIncludes(listOf("**/testDebugUnitTest.exec", "**/*.ec"))
        }
    )
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "net.spooncast"
            artifactId = "openmocker"
            version = "0.1.0-beta1"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}