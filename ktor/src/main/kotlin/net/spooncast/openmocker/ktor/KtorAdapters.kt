package net.spooncast.openmocker.ktor

import io.ktor.client.request.HttpRequestData
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.fullPath
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
 * @param request The Ktor HttpRequestData to convert
 * @return MockKey representing the request
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
 * Creates a mock Ktor HttpResponse from a MockResponse configuration.
 *
 * This function constructs a Ktor HttpResponse that matches the mock configuration,
 * allowing the plugin to return mock responses that integrate seamlessly with
 * the Ktor client pipeline.
 *
 * TODO: This is a placeholder implementation. The actual implementation will require:
 * 1. Proper HttpResponse construction using Ktor's response building APIs
 * 2. Handling of different content types and headers
 * 3. Integration with the request/response pipeline
 * 4. Proper coroutine context management
 *
 * @param mockResponse The MockResponse configuration
 * @param originalRequest The original HttpRequestData for context
 * @return HttpResponse configured according to the mock
 */
suspend fun createMockResponse(
    mockResponse: MockResponse,
    originalRequest: HttpRequestData
): HttpResponse {
    // TODO: Implement proper mock response creation
    // This is a complex operation that requires:
    // 1. Creating an HttpResponseBuilder
    // 2. Setting the status code from mockResponse.code
    // 3. Setting the response body from mockResponse.body
    // 4. Handling content-type and other headers appropriately
    // 5. Ensuring proper integration with Ktor's response pipeline

    // For now, this is a placeholder that will throw an exception
    // The actual implementation will be part of Phase 2.2
    throw NotImplementedError(
        "Mock response creation is not yet implemented. " +
        "This will be implemented in Phase 2.2 of the Ktor integration."
    )
}

/**
 * Safely reads the response body content without consuming the original stream.
 *
 * This utility function is needed to cache response bodies while ensuring that
 * the original response can still be consumed by the application code.
 *
 * TODO: This is a placeholder implementation. The actual implementation will require:
 * 1. Proper stream handling to avoid consuming the original response
 * 2. Memory management for large response bodies
 * 3. Handling of different content encodings (gzip, deflate, etc.)
 * 4. Proper error handling for stream reading failures
 *
 * @param response The HttpResponse to read from
 * @return The response body as a string
 */
suspend fun HttpResponse.readBodySafely(): String {
    // TODO: Implement safe body reading
    // This requires careful handling to avoid consuming the response stream
    // that the application code might need to read later

    // For now, return empty string as placeholder
    // The actual implementation will be part of Phase 2.2
    return ""
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
        val pathStart = fullUrl.indexOf('/', fullUrl.indexOf("://") + 3)
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