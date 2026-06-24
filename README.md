# SpoonOpenMocker

[![JitPack](https://jitpack.io/v/spooncast/SpoonOpenMocker.svg)](https://jitpack.io/#spooncast/SpoonOpenMocker)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://opensource.org/licenses/Apache-2.0)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-28-orange.svg)](https://developer.android.com/about/versions/pie)

SpoonOpenMocker is an Android library that provides powerful HTTP request mocking capabilities for both OkHttp and Ktor clients. It allows developers to easily intercept network requests, cache responses, and mock API responses during development and testing.

Developed and maintained by **SpoonLabs Android Team**.

## Features

- 🔌 **Multi-Client Support**: Works with both OkHttp (via Interceptor) and Ktor (via Plugin)
- 🔁 **Event Injection**: Inject realtime events (WebSocket, SSE, FCM, polling — any transport) into your app through an `OpenMockerEventInjector`
- 🖥️ **IDE Plugin**: Drive mocks from an Android Studio / IntelliJ tool window over a local control server
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
    implementation("com.github.spooncast:SpoonOpenMocker:0.0.19")
}
```

## Quick Start

SpoonOpenMocker can be driven two ways:

- **Built-in Compose UI** — call `OpenMocker.show(context)` to manage mocks on the device itself.
- **IDE Plugin** — manage mocks and inject realtime events from Android Studio / IntelliJ over a local control server (see [IDE Plugin](#ide-plugin-android-studio--intellij)).

Pick whichever fits your workflow — both share the same in-memory mock store. To use the IDE plugin (and event injection), start the control server once in your `Application`:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {                 // debug builds only
            OpenMocker.control.start()           // binds loopback 127.0.0.1:8099
            // OpenMocker.control.start(port = 9000)  // pick another port if 8099 is taken
        }
    }
}
```

> The control server binds to the loopback address only and is intended for **debug builds**. Call `OpenMocker.control.stop()` to stop it.

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

### Event Injection

OpenMocker is **transport-agnostic** about realtime events — it does **not** intercept WebSocket
frames the way it does HTTP. Instead, your app exposes an **event injector** that the IDE plugin
(or any `curl`) can push raw payloads into via `POST /inject/{id}`. The same mechanism works for
WebSocket, SSE, FCM, long-polling, or anything else — your injector decides how to deliver the payload.

The easiest way is to extend `BufferedEventInjector`, which keeps a ring buffer of recorded frames
(so the plugin can list and replay them) and auto-assigns each a `sequence` — you only implement
`deliver`:

```kotlin
class ChatEventInjector(private val socket: MySocketClient) : BufferedEventInjector(
    id = "chat",                                   // matches POST /inject/{id}; must be [A-Za-z0-9._-]
    name = "Realtime Chat",                        // shown in the plugin UI
) {
    // Raw payload is delivered as-is; your app decides how to interpret it.
    override fun deliver(payload: String) {
        socket.emit(payload)                       // feed it into your app's realtime stream
    }
}
```

> Prefer the full interface? Implement `OpenMockerEventInjector` directly. `recorded()` and
> `clearRecorded()` then default to no-ops (opt-in) — injectors that don't track history can leave
> them as-is.

Register the injector alongside the control server:

```kotlin
override fun onCreate() {
    super.onCreate()
    if (BuildConfig.DEBUG) {
        OpenMocker.control.start()
        OpenMocker.control.registerInjector(ChatEventInjector(socketClient))   // unregisterInjector(id) to remove
    }
}
```

> **Tip:** Make the injection side and the consuming side (e.g. your `ViewModel`) share the **same
> singleton** socket client, so a payload injected from the plugin flows straight into your UI.
> The injector `id` becomes a URL path segment (`/inject/{id}`), so it must match `[A-Za-z0-9._-]`;
> registering an id with other characters throws `IllegalArgumentException`.

### Opening the Mocker UI

```kotlin
// Show the mocker activity
OpenMocker.show(context)

// Or show as notification (requires POST_NOTIFICATIONS permission)
// On API 33+ the caller is responsible for requesting the runtime permission.
OpenMocker.showNotification(activity)
```

## IDE Plugin (Android Studio / IntelliJ)

The OpenMocker plugin gives you a desktop control panel for a connected device — edit REST mocks and
fire realtime events without leaving the IDE. It talks to the library's control server over a local
`adb` port forward.

**Prerequisites**

1. The app integrates the library and calls `OpenMocker.control.start()` (see [Quick Start](#quick-start)).
2. The app is running on a connected device/emulator (debug build).
3. The OpenMocker plugin is installed in Android Studio / IntelliJ (see below).

**Installing the plugin**

The plugin is distributed internally as a `.zip` (it is **not** published to the JetBrains Marketplace).

1. Get the latest `openmocker-plugin-<version>.zip` from the team (shared drive / chat), or build it
   yourself (see [Building & distributing](#building--distributing-the-plugin-internal)).
2. In Android Studio / IntelliJ open **Settings → Plugins → ⚙️ → Install Plugin from Disk…** and pick
   the `.zip`.
3. Restart the IDE when prompted.

> Requires a build **242 or newer** (Android Studio Ladybug 2024.2+ / IntelliJ IDEA 2024.2+). There is
> no upper bound, so newer IDE versions are supported without reinstalling a rebuilt plugin.

**Usage**

1. Open the **OpenMocker** tool window (right-hand tool bar).
2. Pick your device from the dropdown — the plugin runs `adb forward tcp:8099 tcp:8099` and connects
   (status bar: *Idle → Forwarding → Connected*).
3. **REST tab** — inspect recorded HTTP requests, edit the response (status code / body / delay), and
   save to mock it. Un-mock a single entry or **Clear all** at once.
4. **Event Injection tab** — pick a registered injector, type or edit a payload and send it
   (`POST /inject/{id}`), and watch it land in the running app. Recorded frames are polled and listed;
   select one to copy it back into the editor for a quick edit-and-replay.

Under the hood the control server exposes a small loopback-only HTTP API:

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/rest/recorded` | List recorded HTTP entries |
| `POST` | `/rest/mock` | Create / update a mock |
| `DELETE` | `/rest/mock?method=&path=` | Un-mock one entry |
| `DELETE` | `/rest/mock?all=true` | Clear all records / mocks |
| `GET` | `/inject/injectors` | List registered injectors |
| `POST` | `/inject/{id}` | Inject a raw payload into an injector |
| `GET` | `/inject/{id}/recorded` | List frames the injector recorded |
| `DELETE` | `/inject/{id}/recorded` | Clear the injector's recorded buffer |

Because it's a plain HTTP API, you can also drive it from `curl` for scripting:

```bash
adb forward tcp:8099 tcp:8099
curl -X POST localhost:8099/inject/chat -d '{"event":"chat","text":"hello"}'
```

### Building & distributing the plugin (internal)

The plugin lives in `plugin/` as a standalone Gradle build. To produce a distributable archive:

```bash
cd plugin
./gradlew buildPlugin
```

This writes `plugin/build/distributions/openmocker-plugin-<version>.zip`. Share that `.zip` with the
team and install it via **Install Plugin from Disk…** (see [Installing the plugin](#ide-plugin-android-studio--intellij)).

Optional checks before sharing:

```bash
./gradlew verifyPluginStructure   # validate plugin.xml + archive layout (fast)
./gradlew verifyPlugin            # run the IntelliJ Plugin Verifier (downloads target IDEs)
```

Distribution is **manual zip sharing**; the JetBrains Marketplace is not used. Hosting an internal
plugin repository (`updatePlugins.xml`) for in-IDE auto-updates, and signing the archive
(`./gradlew signPlugin`), are possible future options but are not set up today.

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
- **`app/`**: Demo application with weather API + event injection (WebSocket) examples
- **`plugin/`**: Android Studio / IntelliJ plugin (desktop control panel)

## Requirements

### Minimum Requirements

- **Android**: API 28 (Android 9.0) or higher
- **Kotlin**: 2.2.0 or higher
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
Copyright 2024-2026 SpoonLabs Android Team

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