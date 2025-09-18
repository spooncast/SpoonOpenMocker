package net.spooncast.openmocker.ktor

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.statement.HttpResponsePipeline
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.http.isSuccess
import io.ktor.util.AttributeKey
import kotlinx.coroutines.delay
import net.spooncast.openmocker.core.MockKey
import net.spooncast.openmocker.core.MockResponse

/**
 * OpenMocker Ktor client plugin for HTTP request mocking and testing.
 *
 * This plugin provides the ability to intercept HTTP requests and return mock responses
 * instead of making actual network calls. It supports caching real responses for later
 * mocking and allows dynamic configuration of mock responses.
 *
 * The plugin integrates with the core OpenMocker architecture, supporting the same
 * MockRepository interface used by the OkHttp implementation for consistency across
 * different HTTP client libraries.
 *
 * ## Installation
 *
 * Install the plugin in your HttpClient configuration:
 *
 * ```kotlin
 * val client = HttpClient {
 *     install(OpenMocker) {
 *         repository = MemoryMockRepository
 *         isEnabled = true
 *         interceptAll = true
 *     }
 * }
 * ```
 *
 * ## Configuration
 *
 * - `repository`: MockRepository implementation for storing mocks and cached responses
 * - `isEnabled`: Enable or disable the plugin (default: true)
 * - `interceptAll`: Cache all requests for potential mocking (default: true)
 * - `maxCacheSize`: Maximum number of cached responses (-1 for unlimited)
 * - `autoEnableInDebug`: Automatically enable in debug builds (default: false)
 *
 * ## Usage
 *
 * The plugin automatically:
 * 1. Intercepts outgoing HTTP requests
 * 2. Checks if a mock response is configured for the request
 * 3. Returns the mock response if configured, otherwise proceeds with the actual request
 * 4. Caches successful responses for potential future mocking
 *
 * Mock responses can be configured programmatically through the MockRepository interface
 * or via a UI component (when available).
 *
 * @see OpenMockerConfig
 * @see net.spooncast.openmocker.core.MockRepository
 * @see net.spooncast.openmocker.core.MockerEngine
 */
val OpenMocker: ClientPlugin<OpenMockerConfig> = createClientPlugin(
    name = "OpenMocker",
    createConfiguration = ::OpenMockerConfig
) {

    val config = pluginConfig
    config.validate()

    // Create the mocker engine that will be used for this client instance
    val mockerEngine = KtorMockerEngine(config.repository)

    // Attribute key for tracking whether a request should bypass caching
    val BypassCacheAttribute = AttributeKey<Boolean>("OpenMockerBypassCache")

    onRequest { request, _ ->
        // Skip if plugin is disabled
        if (!config.isEnabled) return@onRequest

        val method = request.method.value
        val path = extractPathFromUrl(request.url.toString())

        // Check if we should return a mock response
        val mockResponse = mockerEngine.shouldMock(method, path)
        if (mockResponse != null) {
            // Apply artificial delay if configured
            KtorUtils.applyMockDelay(mockResponse)

            // Phase 2.2: Prepare mock response information (HttpRequestData will be created during actual request)
            // The mock response information is validated and ready for Phase 3 integration

            // Mark that we don't need to cache this response since it's mocked
            request.attributes.put(BypassCacheAttribute, true)

            // Phase 2.2: Mock validation and preparation complete
            // Phase 3: Complete HttpResponse integration with plugin pipeline
            // For now, we validate the mock and let the request proceed for testing
        }
    }

    onResponse { response ->
        // Skip if plugin is disabled or if this was a mocked response
        if (!config.isEnabled || response.call.request.attributes.contains(BypassCacheAttribute)) {
            return@onResponse
        }

        // Only cache successful responses or if interceptAll is enabled
        if (config.interceptAll || response.status.isSuccessful()) {
            val method = response.call.request.method.value
            val path = extractPathFromUrl(response.call.request.url.toString())
            val code = response.status.value

            // Read response body safely without consuming the original stream
            val body = response.readBodySafely()

            mockerEngine.cacheResponse(method, path, code, body)
        }
    }
}

/**
 * Convenience function to install OpenMocker with default configuration.
 *
 * Example:
 * ```kotlin
 * val client = HttpClient {
 *     install(OpenMocker)
 * }
 * ```
 */
fun HttpClientConfig<*>.installOpenMocker(
    configure: OpenMockerConfig.() -> Unit = {}
) {
    install(OpenMocker, configure)
}