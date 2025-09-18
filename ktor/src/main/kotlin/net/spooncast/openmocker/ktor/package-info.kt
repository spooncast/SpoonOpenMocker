/**
 * # OpenMocker Ktor Integration
 *
 * This package provides Ktor client plugin integration for the OpenMocker HTTP mocking library.
 * It implements the core OpenMocker interfaces for the Ktor HTTP client, enabling seamless
 * HTTP request mocking and testing in Ktor-based applications.
 *
 * ## Architecture
 *
 * The Ktor integration follows the same multi-layered architecture as the OkHttp implementation:
 *
 * - **Plugin Layer**: [OpenMocker] Ktor client plugin using `createClientPlugin` API
 * - **Configuration Layer**: [OpenMockerConfig] DSL-based configuration with sensible defaults
 * - **Engine Layer**: [KtorMockerEngine] implements [net.spooncast.openmocker.core.MockerEngine]
 * - **Adapter Layer**: Extension functions and utilities for converting between Ktor and core types
 * - **Core Integration**: Uses shared [net.spooncast.openmocker.core.MockRepository] interface
 *
 * ## Installation
 *
 * Add the OpenMocker plugin to your Ktor HttpClient:
 *
 * ```kotlin
 * val client = HttpClient {
 *     install(OpenMocker) {
 *         repository = MemoryMockRepository  // Shared with OkHttp implementation
 *         isEnabled = true
 *         interceptAll = true
 *     }
 * }
 * ```
 *
 * ## Configuration Options
 *
 * - **repository**: [net.spooncast.openmocker.core.MockRepository] implementation for storing mocks
 * - **isEnabled**: Enable/disable plugin functionality (default: true)
 * - **interceptAll**: Cache all requests for potential mocking (default: true)
 * - **maxCacheSize**: Maximum cached responses (-1 for unlimited)
 * - **autoEnableInDebug**: Automatically enable in debug builds (default: false)
 *
 * ## Usage Patterns
 *
 * ### Basic Usage
 * ```kotlin
 * val client = HttpClient {
 *     install(OpenMocker)
 * }
 *
 * // Requests are automatically cached for mocking
 * val response = client.get("https://api.example.com/data")
 * ```
 *
 * ### Programmatic Mocking
 * ```kotlin
 * val engine = createKtorMockerEngine()
 * val mockKey = MockKey("GET", "/api/data")
 * val mockResponse = MockResponse(200, """{"result": "mocked"}""", delay = 100)
 *
 * engine.mock(mockKey, mockResponse)
 * ```
 *
 * ### Custom Repository
 * ```kotlin
 * val customRepository = MyCustomMockRepository()
 * val client = HttpClient {
 *     install(OpenMocker) {
 *         repository = customRepository
 *     }
 * }
 * ```
 *
 * ## Implementation Status
 *
 * **Phase 2.1 (Current)**: ✅ Plugin skeleton structure complete
 * - Plugin configuration and registration
 * - Basic request interception framework
 * - Core interface integration
 * - Adapter structure definition
 *
 * **Phase 2.2 (Next)**: 🔄 Response creation and body handling
 * - Mock response creation and injection
 * - Safe response body reading without stream consumption
 * - Content-type and header handling
 * - Error handling and edge cases
 *
 * **Phase 2.3 (Future)**: 📋 Testing and optimization
 * - Comprehensive test suite with BDD-style tests
 * - Performance optimization and benchmarking
 * - Integration testing with real Ktor applications
 * - Documentation and examples
 *
 * ## Integration with Core
 *
 * The Ktor implementation shares the same core interfaces and repository with the OkHttp
 * implementation, ensuring:
 *
 * - **Consistent API**: Same mock configuration works across HTTP clients
 * - **Shared State**: Mocks configured in OkHttp are available in Ktor and vice versa
 * - **Unified Testing**: Single test infrastructure for multi-client applications
 * - **Repository Compatibility**: Any MockRepository implementation works with both clients
 *
 * ## Thread Safety
 *
 * All components in this package are designed to be thread-safe:
 *
 * - **Plugin**: Thread-safe request/response interception using Ktor's pipeline model
 * - **Engine**: Thread-safe repository operations using coroutines
 * - **Configuration**: Immutable configuration after plugin installation
 * - **Repository**: Thread-safety provided by the repository implementation
 *
 * @see net.spooncast.openmocker.core
 * @see OpenMocker
 * @see OpenMockerConfig
 * @see KtorMockerEngine
 *
 * @since 1.0.0
 * @author Spoon Android Team
 */
@file:JvmName("KtorOpenMockerIntegration")

package net.spooncast.openmocker.ktor