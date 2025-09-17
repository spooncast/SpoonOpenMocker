/**
 * OpenMocker Core - Platform-independent HTTP mocking abstractions.
 *
 * This package contains the core interfaces and data models that provide
 * platform-independent abstractions for HTTP request mocking. It serves as
 * the foundation for specific HTTP client implementations (OkHttp, Ktor, etc.).
 *
 * ## Key Components
 *
 * ### [MockerEngine]
 * The main interface that defines the contract for HTTP mocking engines.
 * Implementations of this interface handle the platform-specific details
 * of intercepting HTTP requests and serving mock responses.
 *
 * ### [HttpRequest] and [HttpResponse]
 * Platform-independent abstractions for HTTP requests and responses.
 * These interfaces allow the core mocking logic to work with different
 * HTTP client libraries without tight coupling.
 *
 * ### [MockKey] and [MockResponse]
 * Data models that represent the key-value pairs used in mocking.
 * - [MockKey]: Identifies a unique HTTP request (method + path)
 * - [MockResponse]: Defines the mock response (code + body + delay)
 *
 * ## Usage Pattern
 *
 * 1. **Create a platform-specific implementation** of [MockerEngine]
 * 2. **Integrate with HTTP client** by intercepting requests
 * 3. **Check for mocks** using [MockerEngine.shouldMock]
 * 4. **Cache responses** using [MockerEngine.cacheResponse]
 * 5. **Control mocking** using [MockerEngine.mock] and [MockerEngine.unmock]
 *
 * ## Design Principles
 *
 * - **Platform Independence**: No dependencies on Android or specific HTTP libraries
 * - **Async Support**: All methods are suspend functions for coroutine compatibility
 * - **Extensibility**: Interfaces designed for easy extension and customization
 * - **Type Safety**: Strong typing with serializable data classes
 *
 * @since 1.0.0
 * @author Spoon Android Team
 */
package net.spooncast.openmocker.core