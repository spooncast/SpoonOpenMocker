package net.spooncast.openmocker.lib.model

/**
 * Client-agnostic HTTP request data model
 *
 * This class abstracts HTTP request information from specific client implementations
 * (OkHttp, Ktor) to enable unified processing in the mocking engine.
 */
internal data class HttpRequestData(
    /**
     * HTTP method (GET, POST, PUT, DELETE, etc.)
     */
    val method: String,

    /**
     * URL path (e.g., "/api/v1/users")
     */
    val path: String,

    /**
     * Full URL string for debugging and logging purposes
     */
    val url: String,

    /**
     * HTTP headers as a map where each header name maps to a list of values
     */
    val headers: Map<String, List<String>> = emptyMap()
) {

    /**
     * Converts this request data to a CachedKey for repository operations
     */
    fun toCachedKey(): CachedKey {
        return CachedKey(
            method = method,
            path = path
        )
    }

    /**
     * Gets the first value of a specific header (case-insensitive)
     */
    fun getHeader(name: String): String? {
        return headers.entries
            .firstOrNull { it.key.equals(name, ignoreCase = true) }
            ?.value
            ?.firstOrNull()
    }

    /**
     * Gets all values of a specific header (case-insensitive)
     */
    fun getHeaders(name: String): List<String> {
        return headers.entries
            .firstOrNull { it.key.equals(name, ignoreCase = true) }
            ?.value
            ?: emptyList()
    }
}