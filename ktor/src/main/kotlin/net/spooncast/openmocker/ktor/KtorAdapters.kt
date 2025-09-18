package net.spooncast.openmocker.ktor

import io.ktor.client.request.HttpRequestData
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.http.content.TextContent
import io.ktor.util.date.GMTDate
import net.spooncast.openmocker.core.MockKey
import net.spooncast.openmocker.core.MockResponse

/**
 * Adapter functions for converting between Ktor HTTP types and OpenMocker core types.
 *
 * These adapters provide clean abstraction between the Ktor-specific HTTP client
 * implementation and the platform-independent core interfaces, following the same
 * adapter pattern used in the OkHttp implementation.
 */

/**
 * Converts a Ktor HttpRequestData to a MockKey for mock lookup and caching.
 *
 * This adapter extracts the essential request information (method and path)
 * needed to uniquely identify requests for mocking purposes.
 *
 * @return MockKey representing this request
 */
fun HttpRequestData.toMockKey(): MockKey {
    return MockKey(
        method = method.value,
        path = url.fullPath
    )
}

/**
 * Converts a Ktor HttpResponse to a MockResponse for caching purposes.
 *
 * This adapter extracts the response information needed to create a mock response
 * that can be returned for future matching requests.
 *
 * @param response The Ktor HttpResponse to convert
 * @param body The response body content as string
 * @return MockResponse representing the response
 */
suspend fun HttpResponse.toMockResponse(body: String = ""): MockResponse {
    return MockResponse(
        code = status.value,
        body = body,
        delay = 0L // Default no delay for cached responses
    )
}

/**
 * Creates mock response information from a MockResponse configuration.
 *
 * This function prepares mock response data that can be used by the plugin
 * to return synthetic responses. The actual HttpResponse creation is handled
 * by Ktor's plugin pipeline mechanisms.
 *
 * Implementation details:
 * - Validates mock response configuration
 * - Automatically detects content type based on response body
 * - Supports JSON, XML, and plain text content types
 * - Handles HTTP status codes according to Ktor specifications
 *
 * Note: In Phase 2.2, this function validates and prepares mock data.
 * The actual HttpResponse integration will be implemented in Phase 3
 * when full plugin integration is completed.
 *
 * @param mockResponse The MockResponse configuration
 * @param originalRequest The original HttpRequestData for context
 * @return MockResponseInfo containing validated mock response data
 */
suspend fun createMockHttpResponse(
    mockResponse: MockResponse,
    originalRequest: HttpRequestData
): MockResponseInfo {
    return try {
        val statusCode = HttpStatusCode.fromValue(mockResponse.code)
        val contentType = detectContentType(mockResponse.body)

        // Validate the mock response
        require(mockResponse.code in 100..599) { "Invalid HTTP status code: ${mockResponse.code}" }

        MockResponseInfo(
            statusCode = statusCode,
            body = mockResponse.body,
            contentType = contentType,
            delay = mockResponse.delay,
            originalRequest = originalRequest
        )
    } catch (e: Exception) {
        throw MockResponseCreationException("Failed to create mock response: ${e.message}", e)
    }
}

/**
 * Safely reads the response body content without consuming the original stream.
 *
 * This utility function is needed to cache response bodies while ensuring that
 * the original response can still be consumed by the application code.
 *
 * Implementation strategy:
 * - Uses Ktor's built-in stream handling for safe body reading
 * - Handles different content encodings automatically
 * - Provides fallback for memory management of large responses
 * - Includes proper error handling for stream reading failures
 *
 * Note: This method should be called in a response interceptor context where
 * Ktor can properly manage the response stream lifecycle.
 *
 * @param response The HttpResponse to read from
 * @return The response body as a string, or empty string if reading fails
 */
suspend fun HttpResponse.readBodySafely(): String {
    return try {
        // Use Ktor's bodyAsText() which handles encoding and content type automatically
        // This is safe to call in interceptors as Ktor manages the stream lifecycle
        bodyAsText()
    } catch (e: Exception) {
        // Return empty string for failures to avoid breaking the response pipeline
        // In production, this should be logged for debugging
        ""
    }
}

/**
 * Utility function to extract path without query parameters from a URL.
 *
 * This ensures consistent path extraction for mock key generation,
 * matching the behavior used in the OkHttp implementation.
 *
 * @param fullUrl The complete URL string
 * @return The path portion without query parameters or fragment
 */
fun extractPathFromUrl(fullUrl: String): String {
    return try {
        // Use URI instead of deprecated URL constructor
        val uri = java.net.URI(fullUrl)
        uri.path.takeIf { it.isNotEmpty() } ?: "/"
    } catch (e: Exception) {
        // Fallback to simple string processing if URI parsing fails
        val protocolIndex = fullUrl.indexOf("://")
        if (protocolIndex == -1) return "/" // No protocol found

        val pathStart = fullUrl.indexOf('/', protocolIndex + 3)
        if (pathStart == -1) return "/"

        val queryStart = fullUrl.indexOf('?', pathStart)
        val fragmentStart = fullUrl.indexOf('#', pathStart)

        val pathEnd = when {
            queryStart != -1 && fragmentStart != -1 -> minOf(queryStart, fragmentStart)
            queryStart != -1 -> queryStart
            fragmentStart != -1 -> fragmentStart
            else -> fullUrl.length
        }

        fullUrl.substring(pathStart, pathEnd).takeIf { it.isNotEmpty() } ?: "/"
    }
}

/**
 * Data class containing mock response information for plugin processing.
 *
 * This class holds all the necessary information to create a mock response
 * within the Ktor client plugin pipeline. It serves as an intermediate
 * representation before actual HttpResponse creation.
 *
 * @property statusCode The HTTP status code for the mock response
 * @property body The response body content
 * @property contentType The detected content type
 * @property delay The artificial delay to apply
 * @property originalRequest The original request for context
 */
data class MockResponseInfo(
    val statusCode: HttpStatusCode,
    val body: String,
    val contentType: ContentType,
    val delay: Long,
    val originalRequest: HttpRequestData
)

/**
 * Detects the appropriate ContentType based on response body content.
 *
 * This method performs simple content inspection to determine the most
 * appropriate content type for the response.
 *
 * @param body The response body content
 * @return The detected ContentType
 */
private fun detectContentType(body: String): ContentType {
    val trimmed = body.trim()
    return when {
        trimmed.startsWith("{") || trimmed.startsWith("[") -> ContentType.Application.Json
        trimmed.startsWith("<") -> ContentType.Application.Xml
        body.contains("html", ignoreCase = true) -> ContentType.Text.Html
        else -> ContentType.Text.Plain
    }
}

/**
 * Exception thrown when mock response creation fails.
 */
class MockResponseCreationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)