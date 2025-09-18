package net.spooncast.openmocker.okhttp.adapters

import net.spooncast.openmocker.core.HttpResponse
import okhttp3.Response
import java.io.IOException

/**
 * Adapter that converts OkHttp Response to the core HttpResponse interface.
 *
 * This adapter provides a bridge between OkHttp's Response class and the platform-independent
 * HttpResponse interface defined in the core module. It handles proper resource management
 * for response bodies to prevent memory leaks.
 *
 * Important Notes:
 * - Response body can only be consumed once in OkHttp
 * - This adapter caches the body string to allow multiple accesses
 * - Proper error handling for I/O operations during body reading
 *
 * Memory Management:
 * - Response body is read and cached during first access
 * - Original ResponseBody is closed after reading to free resources
 * - Subsequent accesses return the cached string value
 *
 * @property response The underlying OkHttp Response object
 */
class OkHttpResponseAdapter(
    private val response: Response
) : HttpResponse {

    /**
     * Cached response body string.
     * Lazily initialized to avoid unnecessary I/O operations.
     */
    private var _cachedBody: String? = null

    /**
     * HTTP status code from the OkHttp Response.
     * Examples: 200, 404, 500, etc.
     */
    override val code: Int
        get() = response.code

    /**
     * Response body as string.
     *
     * The body is read and cached on first access to allow multiple reads.
     * If an error occurs during body reading, an empty string is returned
     * to maintain consistent behavior.
     *
     * Thread Safety: This implementation is not thread-safe. If multiple threads
     * access the body simultaneously, synchronization should be handled externally.
     *
     * @return The response body as a string, or empty string if reading fails
     */
    override val body: String
        get() {
            // Return cached body if already read
            _cachedBody?.let { return it }

            // Read and cache the body
            return try {
                val bodyString = response.body?.string() ?: ""
                _cachedBody = bodyString
                bodyString
            } catch (e: IOException) {
                // Log error in production environment
                // For now, return empty string to maintain consistent behavior
                val emptyBody = ""
                _cachedBody = emptyBody
                emptyBody
            }
        }

    /**
     * Checks if the response was successful (status code 200-299).
     *
     * @return true if the response was successful, false otherwise
     */
    fun isSuccessful(): Boolean {
        return response.isSuccessful
    }

    /**
     * Gets the response message associated with the status code.
     *
     * @return The response message (e.g., "OK", "Not Found")
     */
    fun message(): String {
        return response.message
    }

    /**
     * Gets a header value by name.
     *
     * @param name The header name
     * @return The header value, or null if not present
     */
    fun header(name: String): String? {
        return response.header(name)
    }

    /**
     * Gets all headers as a map.
     *
     * @return A map of header names to values
     */
    fun headers(): Map<String, String> {
        return response.headers.toMultimap().mapValues { it.value.joinToString(", ") }
    }

    /**
     * Provides a string representation for debugging purposes.
     *
     * @return A string representation of this adapter
     */
    override fun toString(): String {
        return "OkHttpResponseAdapter(code=$code, message='${message()}', successful=${isSuccessful()})"
    }

    /**
     * Compares this adapter with another object for equality.
     * Two adapters are equal if they wrap the same OkHttp Response.
     *
     * @param other The other object to compare with
     * @return true if the objects are equal, false otherwise
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OkHttpResponseAdapter) return false
        return response == other.response
    }

    /**
     * Returns the hash code for this adapter.
     * Uses the underlying OkHttp Response's hash code.
     *
     * @return The hash code
     */
    override fun hashCode(): Int {
        return response.hashCode()
    }
}