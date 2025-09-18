package net.spooncast.openmocker.okhttp

import kotlinx.coroutines.runBlocking
import net.spooncast.openmocker.core.MemoryMockRepository
import net.spooncast.openmocker.core.MockRepository
import net.spooncast.openmocker.okhttp.utils.OkHttpUtils
import net.spooncast.openmocker.okhttp.utils.toMockKey
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Modern OkHttp interceptor implementation using the new core architecture.
 *
 * This interceptor serves as a bridge between OkHttp's interceptor pattern and the
 * new MockerEngine-based architecture. It maintains full backward compatibility
 * with the existing OpenMockerInterceptor API while leveraging the improved
 * multi-module design.
 *
 * Key Features:
 * - 100% backward compatible with existing OpenMockerInterceptor API
 * - Uses the new MockerEngine and MockRepository abstractions
 * - Thread-safe operations through coroutine integration
 * - Proper resource management for response bodies
 * - Support for artificial delays and custom responses
 *
 * Architecture Benefits:
 * - Separation of concerns between HTTP handling and mock logic
 * - Testable components with dependency injection
 * - Consistent behavior across different HTTP client implementations
 * - Future-proof design supporting multiple platforms
 *
 * @property engine The MockerEngine instance for handling mock operations
 */
class OkHttpMockerInterceptor private constructor(
    private val engine: OkHttpMockerEngine
) : Interceptor {

    /**
     * Intercepts HTTP requests and applies mocking logic.
     *
     * This method follows the standard OkHttp interceptor pattern:
     * 1. Check if a mock response exists for the request
     * 2. If mock exists, return the mock response with optional delay
     * 3. If no mock, proceed with normal request and cache the real response
     *
     * The implementation uses coroutines for async operations while maintaining
     * compatibility with OkHttp's synchronous interceptor interface.
     *
     * @param chain The interceptor chain
     * @return The response (either mocked or real)
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val mockKey = request.toMockKey()

        // Check for existing mock using the engine
        val mockResponse = runBlocking {
            engine.shouldMock(mockKey.method, mockKey.path)
        }

        // Return mock response if available
        if (mockResponse != null) {
            return runBlocking {
                // Apply artificial delay if specified
                OkHttpUtils.applyMockDelay(mockResponse)

                // Create and return mock response
                OkHttpUtils.createMockOkHttpResponse(mockResponse, request)
            }
        }

        // Proceed with real request
        val realResponse = chain.proceed(request)

        // Cache the real response for potential future mocking
        runBlocking {
            // Use peekBody to avoid consuming the response body
            val bodyString = try {
                realResponse.peekBody(Long.MAX_VALUE).string()
            } catch (e: Exception) {
                ""
            }

            engine.cacheResponse(
                method = request.method,
                path = request.url.encodedPath,
                code = realResponse.code,
                body = bodyString
            )
        }

        return realResponse
    }

    /**
     * Builder class for creating OkHttpMockerInterceptor instances.
     *
     * This builder maintains backward compatibility with the existing API while
     * allowing for future extensibility with custom repositories or configurations.
     */
    class Builder {
        private var repository: MockRepository = MemoryMockRepository()

        /**
         * Sets a custom MockRepository for the interceptor.
         *
         * This method allows for dependency injection and custom storage
         * implementations while maintaining the builder pattern.
         *
         * @param repository The custom MockRepository to use
         * @return This builder instance for method chaining
         */
        fun repository(repository: MockRepository): Builder {
            this.repository = repository
            return this
        }

        /**
         * Builds the OkHttpMockerInterceptor instance.
         *
         * This method creates the interceptor with the configured repository
         * and maintains full compatibility with the legacy builder API.
         *
         * @return A configured OkHttpMockerInterceptor instance
         */
        fun build(): OkHttpMockerInterceptor {
            val engine = OkHttpMockerEngine(repository)
            return OkHttpMockerInterceptor(engine)
        }
    }

    /**
     * Companion object providing factory methods and constants.
     *
     * Maintains compatibility with existing usage patterns while providing
     * access to the underlying engine for advanced use cases.
     */
    companion object {
        /**
         * Message returned in mock responses to identify them as mocked.
         * Maintains compatibility with the existing constant.
         */
        const val MOCKER_MESSAGE = "OpenMocker enabled"

        /**
         * Creates a new builder instance.
         *
         * @return A new Builder instance
         */
        fun builder(): Builder = Builder()

        /**
         * Convenience method to create an interceptor with default settings.
         *
         * @return A configured OkHttpMockerInterceptor with default MemoryMockRepository
         */
        fun create(): OkHttpMockerInterceptor = Builder().build()

        /**
         * Creates an interceptor with a custom repository.
         *
         * @param repository The custom MockRepository to use
         * @return A configured OkHttpMockerInterceptor
         */
        fun create(repository: MockRepository): OkHttpMockerInterceptor {
            return Builder().repository(repository).build()
        }
    }

    /**
     * Provides access to the underlying MockerEngine for advanced operations.
     *
     * This method allows access to the engine for operations not directly
     * supported by the interceptor interface, such as programmatic mock management.
     *
     * @return The underlying OkHttpMockerEngine instance
     */
    fun getEngine(): OkHttpMockerEngine = engine

    /**
     * Gets all currently configured mocks.
     *
     * This method provides access to the mock configuration for debugging
     * and management purposes.
     *
     * @return A map of all mock keys to their responses
     */
    suspend fun getAllMocks() = engine.getAllMocks()

    /**
     * Gets all cached responses.
     *
     * This method provides access to cached real responses for debugging
     * and providing options in management UI.
     *
     * @return A map of all cached response keys to their responses
     */
    suspend fun getAllCachedResponses() = engine.getAllCachedResponses()

    /**
     * Clears all mocks and cached responses.
     *
     * This is a destructive operation that cannot be undone.
     */
    suspend fun clearAll() = engine.clearAll()
}