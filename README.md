# SpoonOpenMocker

[![JitPack](https://jitpack.io/v/spooncast/SpoonOpenMocker.svg)](https://jitpack.io/#spooncast/SpoonOpenMocker)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://opensource.org/licenses/Apache-2.0)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-28-orange.svg)](https://developer.android.com/about/versions/pie)

SpoonOpenMocker is an Android library that provides powerful HTTP request mocking capabilities for both OkHttp and Ktor clients. It allows developers to easily intercept network requests, cache responses, and mock API responses during development and testing.

Developed and maintained by **SpoonLabs Android Team**.

## Features

- 🔌 **Multi-Client Support**: Works with both OkHttp (via Interceptor) and Ktor (via Plugin)
- 💾 **Response Caching**: Automatically caches real API responses for later mocking
- ⏱️ **Configurable Delays**: Simulate network latency with custom delay times
- 🎨 **Built-in Compose UI**: Beautiful Material 3 interface for managing mocks
- 📝 **Response Editing**: Modify response codes, bodies, and delays on the fly
- 🔔 **Notification System**: Quick access to mocker UI via persistent notification
- 🏗️ **Clean Architecture**: Client-agnostic design with adapter pattern

## Installation

### Step 1: Add JitPack repository

Add JitPack to your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }  // Add this line
    }
}
```

Or in your root `build.gradle.kts` (legacy):

```kotlin
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }  // Add this line
    }
}
```

### Step 2: Add the dependency

Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.spooncast:SpoonOpenMocker:0.0.18")
}
```

## Quick Start

### OkHttp Integration

Simply add the OpenMocker interceptor to your OkHttpClient:

```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(OpenMocker.getInterceptor())
    .build()

// Use with Retrofit
val retrofit = Retrofit.Builder()
    .baseUrl("https://api.example.com/")
    .client(client)
    .build()
```

### Ktor Integration

Install the OpenMocker plugin in your Ktor HttpClient:

```kotlin
val client = HttpClient(Android) {
    install(OpenMockerPlugin) {
        enabled = true  // Set to false to disable
    }
}
```

### Opening the Mocker UI

```kotlin
// Show the mocker activity
OpenMocker.show(context)

// Or show as notification (requires POST_NOTIFICATIONS permission)
OpenMocker.showNotification(activity)
```

## How It Works

1. **Initial Request**: When your app makes an HTTP request, OpenMocker intercepts it
2. **Check for Mock**: If a mock exists for that endpoint, it returns the cached response
3. **Real Network Call**: If no mock exists, the real network call proceeds
4. **Auto-Cache**: The real response is automatically cached for future mocking
5. **Toggle & Edit**: Use the UI to enable/disable mocks and edit response details

## Usage Example

The library itself **does not require dependency injection**. It uses internal singletons for state management.

### OkHttp + Retrofit Example

```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(OpenMocker.getInterceptor())
    .build()

val retrofit = Retrofit.Builder()
    .baseUrl("https://api.openweathermap.org/data/2.5/")
    .addConverterFactory(GsonConverterFactory.create())
    .client(client)
    .build()

val weatherService = retrofit.create(WeatherApiService::class.java)
```

### Ktor Example

```kotlin
val client = HttpClient(Android) {
    install(Logging) {
        logger = Logger.ANDROID
        level = LogLevel.ALL
    }

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }

    install(OpenMockerPlugin) {
        enabled = true
    }
}

// Use the client for API calls
suspend fun getWeather(): WeatherResponse {
    return client.get("https://api.openweathermap.org/data/2.5/weather") {
        parameter("lat", "44.34")
        parameter("lon", "10.99")
    }.body()
}
```

> **Note**: The demo app uses Hilt for dependency injection, but this is **not required** to use SpoonOpenMocker. The library works with or without DI frameworks.

## Architecture

OpenMocker uses a clean, modular architecture:

```
┌─────────────────────────────────────────┐
│         HTTP Client (OkHttp/Ktor)       │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│  Interceptor/Plugin (Client-Specific)   │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│         MockingEngine<T, R>             │
│     (Client-Agnostic Orchestration)     │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│      HttpClientAdapter Interface        │
│   (OkHttpAdapter / KtorAdapter)         │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│        CacheRepo (In-Memory)            │
│    (Singleton Storage for Mocks)        │
└─────────────────────────────────────────┘
```

### Key Components

- **MockingEngine**: Generic orchestration layer coordinating HTTP clients and cache
- **HttpClientAdapter**: Abstraction for normalizing different HTTP client types (OkHttpAdapter, KtorAdapter)
- **CacheRepo**: In-memory singleton storing mocked responses indexed by HTTP method + URL path
- **OpenMockerInterceptor**: OkHttp interceptor implementation for request interception
- **OpenMockerPlugin**: Ktor plugin implementation using client plugin API
- **UI Layer**: Jetpack Compose with Material 3, MVVM architecture, two-pane layout

## Tech Stack

- **Language**: Kotlin 2.2.0
- **Min SDK**: 28 (Android 9.0+)
- **UI**: Jetpack Compose with Material 3
- **HTTP Clients**: OkHttp 4.3.1, Ktor 3.0.0
- **Serialization**: Kotlinx Serialization, Gson
- **Testing**: JUnit 5, MockK

## Configuration Options

### Disable SpoonOpenMocker

For Ktor, you can disable the plugin:

```kotlin
install(OpenMockerPlugin) {
    enabled = false  // Disable in production builds
}
```

For OkHttp, simply don't add the interceptor in production builds:

```kotlin
val client = OkHttpClient.Builder()
    .apply {
        if (BuildConfig.DEBUG) {
            addInterceptor(OpenMocker.getInterceptor())
        }
    }
    .build()
```

### Custom Response Delays

Use the built-in UI to configure response delays per endpoint:

1. Call `OpenMocker.show(context)` or tap the notification
2. Select an API endpoint from the list
3. Edit the "Delay (ms)" field in the detail pane
4. Changes are applied immediately

## Module Structure

- **`lib/`**: The OpenMocker library (publishable artifact)
- **`app/`**: Demo application with weather API examples

## Requirements

### Minimum Requirements

- **Android**: API 28 (Android 9.0) or higher
- **Kotlin**: 1.9.0 or higher
- **Java**: JDK 17 or higher

### Dependency Version Requirements

OpenMocker is compiled with the following dependency versions. Using lower versions may cause compatibility issues:

| Dependency | Minimum Version | Notes |
|------------|----------------|-------|
| **OkHttp** | `4.3.1+` | Required for OkHttp integration |
| **Ktor Client** | `3.0.0+` | Required for Ktor integration (not compatible with 2.x) |
| **Compose BOM** | `2025.06.01+` | Required for UI functionality |
| **Kotlin** | `2.2.0+` | Recommended for best compatibility |

⚠️ **Important**: If your project uses lower versions of these dependencies, you may encounter:
- Compilation errors
- Runtime crashes
- API incompatibilities
- Missing method exceptions

**Recommendation**: Match or exceed these versions in your project, or use dependency resolution strategies to handle conflicts.

## License

```
Copyright 2024 SpoonLabs Android Team

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Team

SpoonOpenMocker is developed and maintained by the **SpoonLabs Android Team**.

We are committed to providing high-quality Android development tools that improve developer productivity and app quality.