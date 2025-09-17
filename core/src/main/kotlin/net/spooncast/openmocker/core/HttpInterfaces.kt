package net.spooncast.openmocker.core

/**
 * Platform-independent abstraction for HTTP requests.
 *
 * This interface provides a unified view of HTTP requests across different
 * HTTP client implementations (OkHttp, Ktor, etc.).
 */
interface HttpRequest {
    /**
     * HTTP method (GET, POST, PUT, DELETE, etc.)
     */
    val method: String

    /**
     * URL path without query parameters or fragment.
     * Should be the encoded path from the URL.
     */
    val path: String
}

/**
 * Platform-independent abstraction for HTTP responses.
 *
 * This interface provides a unified view of HTTP responses across different
 * HTTP client implementations (OkHttp, Ktor, etc.).
 */
interface HttpResponse {
    /**
     * HTTP status code (200, 404, 500, etc.)
     */
    val code: Int

    /**
     * Response body as string.
     * For binary content, this should be the string representation.
     */
    val body: String
}