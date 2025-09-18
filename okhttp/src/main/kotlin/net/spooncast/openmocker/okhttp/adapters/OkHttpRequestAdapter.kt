package net.spooncast.openmocker.okhttp.adapters

import net.spooncast.openmocker.core.HttpRequest
import okhttp3.Request

/**
 * Adapter that converts OkHttp Request to the core HttpRequest interface.
 *
 * This adapter provides a bridge between OkHttp's Request class and the platform-independent
 * HttpRequest interface defined in the core module. It extracts only the essential information
 * needed for mocking decisions (method and path).
 *
 * Design Considerations:
 * - Lightweight: Only extracts method and path, avoiding unnecessary data copying
 * - Immutable: The underlying OkHttp Request is immutable, ensuring thread safety
 * - Path Extraction: Uses encodedPath to ensure consistent path matching across requests
 *
 * @property request The underlying OkHttp Request object
 */
class OkHttpRequestAdapter(
    private val request: Request
) : HttpRequest {

    /**
     * HTTP method extracted from the OkHttp Request.
     * Examples: "GET", "POST", "PUT", "DELETE", etc.
     */
    override val method: String
        get() = request.method

    /**
     * URL path extracted from the OkHttp Request.
     * Uses encodedPath to ensure consistent encoding and exclude query parameters.
     *
     * Examples:
     * - https://api.example.com/users/123?name=John -> "/users/123"
     * - https://api.example.com/search?q=test -> "/search"
     */
    override val path: String
        get() = request.url.encodedPath

    /**
     * Provides a string representation for debugging purposes.
     *
     * @return A string representation of this adapter
     */
    override fun toString(): String {
        return "OkHttpRequestAdapter(method='$method', path='$path', url='${request.url}')"
    }

    /**
     * Compares this adapter with another object for equality.
     * Two adapters are equal if they wrap the same OkHttp Request.
     *
     * @param other The other object to compare with
     * @return true if the objects are equal, false otherwise
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OkHttpRequestAdapter) return false
        return request == other.request
    }

    /**
     * Returns the hash code for this adapter.
     * Uses the underlying OkHttp Request's hash code.
     *
     * @return The hash code
     */
    override fun hashCode(): Int {
        return request.hashCode()
    }
}