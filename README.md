# OpenMocker

[![Maven Central](https://img.shields.io/badge/Maven%20Central-0.0.18-blue.svg)](https://search.maven.org/artifact/net.spooncast/openmocker)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://opensource.org/licenses/Apache-2.0)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-28-orange.svg)](https://developer.android.com/about/versions/pie)

OpenMocker is an Android library that provides powerful HTTP request mocking capabilities for both OkHttp and Ktor clients. It allows developers to easily intercept network requests, cache responses, and mock API responses during development and testing.

## Features

- 🔌 **Multi-Client Support**: Works with both OkHttp (via Interceptor) and Ktor (via Plugin)
- 💾 **Response Caching**: Automatically caches real API responses for later mocking
- ⏱️ **Configurable Delays**: Simulate network latency with custom delay times
- 🎨 **Built-in Compose UI**: Beautiful Material 3 interface for managing mocks
- 📝 **Response Editing**: Modify response codes, bodies, and delays on the fly
- 🔔 **Notification System**: Quick access to mocker UI via persistent notification
- 🏗️ **Clean Architecture**: Client-agnostic design with adapter pattern

## Installation

Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("net.spooncast:openmocker:0.0.18")
}
```

## Quick Start

### OkHttp Integration

```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(OpenMocker.getInterceptor())
    .build()

val retrofit = Retrofit.Builder()
    .baseUrl("https://api.example.com/")
    .client(client)
    .build()
```

### Ktor Integration

```kotlin
val client = HttpClient(Android) {
    install(OpenMockerPlugin) {
        enabled = true
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

See the demo app for a complete example using the OpenWeatherMap API:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(OpenMocker.getInterceptor())
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }
}
```

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
- **HttpClientAdapter**: Abstraction for normalizing different HTTP client types
- **CacheRepo**: In-memory singleton storing mocked responses by method + path
- **UI Layer**: Jetpack Compose MVVM architecture with two-pane layout

## Tech Stack

- **Language**: Kotlin 2.2.0
- **Min SDK**: 28 (Android 9.0+)
- **UI**: Jetpack Compose with Material 3
- **HTTP Clients**: OkHttp 4.3.1, Ktor 3.0.0
- **Serialization**: Kotlinx Serialization, Gson
- **Testing**: JUnit 5, MockK

## Configuration Options

### Disable OpenMocker

You can disable OpenMocker entirely by setting the config:

```kotlin
// For Ktor
install(OpenMockerPlugin) {
    enabled = false  // Disable in production builds
}
```

### Custom Response Delays

Use the UI to configure response delays per endpoint:

1. Open OpenMocker UI
2. Select an API endpoint
3. Edit the "Delay (ms)" field
4. Save changes

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
Copyright 2024 Spooncast

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