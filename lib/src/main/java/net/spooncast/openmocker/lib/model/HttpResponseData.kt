package net.spooncast.openmocker.lib.model

/**
 * Client-agnostic HTTP response data model
 *
 * This class abstracts HTTP response information from specific client implementations
 * (OkHttp, Ktor) to enable unified processing in the mocking engine.
 */
internal data class HttpResponseData(
    /**
     * HTTP status code (200, 404, 500, etc.)
     */
    val code: Int,

    /**
     * Response body as string
     */
    val body: String,

    /**
     * HTTP headers as a map where each header name maps to a list of values
     */
    val headers: Map<String, List<String>> = emptyMap(),

    /**
     * Whether the response indicates success (2xx status codes)
     */
    val isSuccessful: Boolean = code in 200..299
) {

    /**
     * Converts this response data to a CachedResponse for repository operations
     */
    fun toCachedResponse(duration: Long = 0L): CachedResponse {
        return CachedResponse(
            code = code,
            body = body,
            duration = duration
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

    /**
     * Gets the content type from headers
     */
    fun getContentType(): String? {
        return getHeader("Content-Type")
    }

    /**
     * Checks if the response has JSON content type
     */
    fun isJsonResponse(): Boolean {
        return getContentType()?.contains("application/json", ignoreCase = true) == true
    }
}