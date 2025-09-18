package net.spooncast.openmocker.ktor

import net.spooncast.openmocker.core.MockKey
import net.spooncast.openmocker.core.MockResponse

/**
 * Request context management for tracking mocking state and timing information.
 * Provides thread-safe operations for concurrent request handling.
 *
 * @property mockKey The MockKey identifying this request
 * @property mockResponse The MockResponse if mocking is enabled
 * @property startTime Timestamp when request processing started
 * @property isMocked Whether this request is being mocked
 */
data class RequestContext(
    val mockKey: MockKey,
    val mockResponse: MockResponse? = null,
    val startTime: Long = System.currentTimeMillis(),
    val isMocked: Boolean = mockResponse != null
) {
    /**
     * Get elapsed time since request started.
     * @return Elapsed time in milliseconds
     */
    fun getElapsedTime(): Long = System.currentTimeMillis() - startTime

    /**
     * Create a new context with updated mock response.
     * @param response New mock response
     * @return Updated RequestContext
     */
    fun withMockResponse(response: MockResponse): RequestContext {
        return copy(mockResponse = response, isMocked = true)
    }
}