package net.spooncast.openmocker.okhttp.utils

import net.spooncast.openmocker.core.MockKey
import net.spooncast.openmocker.core.MockResponse
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlinx.coroutines.delay

/**
 * Utility functions and extension methods for OkHttp integration with OpenMocker.
 *
 * This object provides convenient extension functions and utility methods to bridge
 * the gap between OkHttp types and core OpenMocker types.
 *
 * Key Features:
 * - Extension functions for easy conversion between OkHttp and core types
 * - Mock response creation utilities
 * - Error handling for resource management
 * - Support for custom delays and response customization
 */
object OkHttpUtils {

    /**
     * Default media type for JSON responses.
     */
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * Default media type for plain text responses.
     */
    private val TEXT_MEDIA_TYPE = "text/plain; charset=utf-8".toMediaType()

    /**
     * Creates a mock OkHttp Response from a MockResponse and the original Request.
     *
     * This method constructs a complete OkHttp Response object that can be returned
     * by interceptors. It handles proper media type detection and response building.
     *
     * @param mockResponse The mock response configuration
     * @param originalRequest The original HTTP request
     * @param mediaType Optional media type; defaults to JSON if not specified
     * @return A complete OkHttp Response object
     */
    fun createMockOkHttpResponse(
        mockResponse: MockResponse,
        originalRequest: Request,
        mediaType: MediaType? = null
    ): Response {
        val responseMediaType = mediaType ?: detectMediaType(mockResponse.body)
        val responseBody = mockResponse.body.toResponseBody(responseMediaType)

        return Response.Builder()
            .request(originalRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(mockResponse.code)
            .message(getHttpMessage(mockResponse.code))
            .body(responseBody)
            .build()
    }

    /**
     * Applies the delay specified in a MockResponse.
     *
     * This suspend function introduces an artificial delay before returning,
     * simulating network latency or slow server responses.
     *
     * @param mockResponse The mock response containing delay information
     */
    suspend fun applyMockDelay(mockResponse: MockResponse) {
        if (mockResponse.delay > 0) {
            delay(mockResponse.delay)
        }
    }

    /**
     * Detects the appropriate media type based on response body content.
     *
     * This method performs simple content inspection to determine the most
     * appropriate media type for the response.
     *
     * @param body The response body content
     * @return The detected MediaType
     */
    private fun detectMediaType(body: String): MediaType {
        return when {
            body.trim().startsWith("{") || body.trim().startsWith("[") -> JSON_MEDIA_TYPE
            body.trim().startsWith("<") -> "application/xml; charset=utf-8".toMediaType()
            else -> TEXT_MEDIA_TYPE
        }
    }

    /**
     * Gets the standard HTTP message for a given status code.
     *
     * @param code The HTTP status code
     * @return The corresponding HTTP message
     */
    private fun getHttpMessage(code: Int): String {
        return when (code) {
            200 -> "OK"
            201 -> "Created"
            204 -> "No Content"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            403 -> "Forbidden"
            404 -> "Not Found"
            405 -> "Method Not Allowed"
            500 -> "Internal Server Error"
            502 -> "Bad Gateway"
            503 -> "Service Unavailable"
            else -> "Unknown"
        }
    }
}

/**
 * Extension function to convert an OkHttp Request to a MockKey.
 *
 * This extension provides a convenient way to create MockKey instances
 * from OkHttp Request objects for use in mocking operations.
 *
 * @return A MockKey representing this request
 */
fun Request.toMockKey(): MockKey {
    return MockKey(method = this.method, path = this.url.encodedPath)
}

/**
 * Extension function to convert an OkHttp Response to a MockResponse.
 *
 * This extension safely extracts the response data and converts it to
 * a MockResponse for storage in the repository.
 *
 * Note: This will consume the response body. Use with caution in interceptors
 * where the body needs to be preserved for the actual HTTP client.
 *
 * @param delay Optional delay to include in the mock response
 * @return A MockResponse representing this response
 */
fun Response.toMockResponse(delay: Long = 0L): MockResponse {
    val bodyString = try {
        this.body?.string() ?: ""
    } catch (e: Exception) {
        ""
    }

    return MockResponse(
        code = this.code,
        body = bodyString,
        delay = delay
    )
}

/**
 * Extension function to safely peek at the response body without consuming it.
 *
 * This extension creates a copy of the response body that can be read multiple times,
 * which is essential for interceptors that need to both cache and forward the response.
 *
 * @return A new Response with a peekable body
 */
fun Response.peekBody(): Response {
    val responseBody = this.body ?: return this

    val source = responseBody.source()
    source.request(Long.MAX_VALUE) // Request the entire body
    val buffer = source.buffer.clone()

    val peekableBody = buffer.readString(Charsets.UTF_8).toResponseBody(responseBody.contentType())

    return this.newBuilder()
        .body(peekableBody)
        .build()
}

/**
 * Extension function to create a MockResponse from request parameters.
 *
 * This is useful for creating mock responses programmatically without
 * having an actual OkHttp Response object.
 *
 * @param code HTTP status code
 * @param body Response body content
 * @param delay Optional delay in milliseconds
 * @return A MockResponse with the specified parameters
 */
fun Request.createMockResponse(
    code: Int,
    body: String,
    delay: Long = 0L
): MockResponse {
    return MockResponse(
        code = code,
        body = body,
        delay = delay
    )
}

/**
 * Extension function to check if a Response represents a successful HTTP status.
 *
 * @return true if the response code is in the 200-299 range
 */
fun MockResponse.isSuccessful(): Boolean {
    return this.code in 200..299
}

/**
 * Extension function to check if a Response represents a client error.
 *
 * @return true if the response code is in the 400-499 range
 */
fun MockResponse.isClientError(): Boolean {
    return this.code in 400..499
}

/**
 * Extension function to check if a Response represents a server error.
 *
 * @return true if the response code is in the 500-599 range
 */
fun MockResponse.isServerError(): Boolean {
    return this.code in 500..599
}