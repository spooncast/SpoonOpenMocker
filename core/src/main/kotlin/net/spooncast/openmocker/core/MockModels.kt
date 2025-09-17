package net.spooncast.openmocker.core

import kotlinx.serialization.Serializable

/**
 * Unique identifier for HTTP requests used in mocking.
 *
 * This class represents the key components that uniquely identify
 * an HTTP request for mocking purposes.
 *
 * @property method HTTP method (GET, POST, PUT, DELETE, etc.)
 * @property path URL path without query parameters or fragment
 */
@Serializable
data class MockKey(
    val method: String,
    val path: String
)

/**
 * Mock response configuration for HTTP requests.
 *
 * This class defines the mock response that should be returned
 * when a matching HTTP request is intercepted.
 *
 * @property code HTTP status code to return (200, 404, 500, etc.)
 * @property body Response body content as string
 * @property delay Artificial delay in milliseconds before returning response (default: 0)
 */
@Serializable
data class MockResponse(
    val code: Int,
    val body: String,
    val delay: Long = 0L
)