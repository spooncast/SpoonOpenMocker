package net.spooncast.openmocker.ktor

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestData
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
/**
 * Internal AttributeKey definitions for request context management.
 * These keys store context information throughout the request lifecycle.
 */
internal object AttributeKeys {
    /** Store MockResponse when a request should be mocked */
    val MOCK_RESPONSE_KEY = AttributeKey<MockResponse>("OpenMocker.MockResponse")

    /** Store MockKey for request identification */
    val MOCK_KEY = AttributeKey<MockKey>("OpenMocker.MockKey")

    /** Store original HttpRequestBuilder for context */
    val ORIGINAL_REQUEST = AttributeKey<HttpRequestBuilder>("OpenMocker.OriginalRequest")

    /** Store RequestContext for tracking request state */
    val REQUEST_CONTEXT = AttributeKey<RequestContext>("OpenMocker.RequestContext")

    /** Track whether a request should bypass caching */
    val BYPASS_CACHE = AttributeKey<Boolean>("OpenMocker.BypassCache")
}



val OpenMocker: ClientPlugin<OpenMockerConfig> = createClientPlugin(
    name = "OpenMocker",
    createConfiguration = ::OpenMockerConfig
) {

    val config = pluginConfig
    config.validate()

    // Create the mocker engine that will be used for this client instance
    val mockerEngine = KtorMockerEngine(config.repository)

    // Initialize metrics tracking if enabled
    val metrics = if (config.metricsEnabled) MockingMetrics() else null

    // Logger function that respects configuration
    fun log(level: OpenMockerConfig.LogLevel, message: String) {
        if (config.enableLogging && level.ordinal >= config.logLevel.ordinal) {
            val prefix = when (level) {
                OpenMockerConfig.LogLevel.DEBUG -> "[DEBUG]"
                OpenMockerConfig.LogLevel.INFO -> "[INFO]"
                OpenMockerConfig.LogLevel.WARN -> "[WARN]"
                OpenMockerConfig.LogLevel.ERROR -> "[ERROR]"
            }
            println("$prefix OpenMocker: $message")
        }
    }

    onRequest { request, _ ->
        // Skip if plugin is disabled
        if (!config.isEnabled) {
            log(OpenMockerConfig.LogLevel.DEBUG, "Plugin disabled, skipping request")
            return@onRequest
        }

        val startTime = System.currentTimeMillis()
        val method = request.method.value
        val path = extractPathFromUrl(request.url.toString())
        val mockKey = MockKey(method, path)

        log(OpenMockerConfig.LogLevel.DEBUG, "Processing request: $method $path")

        try {
            // Check if we should return a mock response
            val mockResponse = mockerEngine.shouldMock(method, path)

            // Create request context for tracking
            val requestContext = if (mockResponse != null) {
                RequestContext(mockKey, mockResponse, startTime, true)
            } else {
                RequestContext(mockKey, null, startTime, false)
            }

            // Store context in request attributes
            request.attributes.put(AttributeKeys.REQUEST_CONTEXT, requestContext)
            request.attributes.put(AttributeKeys.MOCK_KEY, mockKey)

            if (mockResponse != null) {
                log(OpenMockerConfig.LogLevel.INFO, "Mock found for $method $path, code: ${mockResponse.code}")

                // Record metrics
                metrics?.recordMockedRequest()
                metrics?.recordCacheHit()

                // Store mock response in attributes for Phase 3 integration
                request.attributes.put(AttributeKeys.MOCK_RESPONSE_KEY, mockResponse)

                // Apply artificial delay if configured
                if (mockResponse.delay > 0) {
                    log(OpenMockerConfig.LogLevel.DEBUG, "Applying mock delay: ${mockResponse.delay}ms")
                    KtorUtils.applyMockDelay(mockResponse)
                }

                // Mark that we don't need to cache this response since it's mocked
                request.attributes.put(AttributeKeys.BYPASS_CACHE, true)

                log(OpenMockerConfig.LogLevel.DEBUG, "Mock response prepared, ready for Phase 3 integration")
            } else {
                log(OpenMockerConfig.LogLevel.DEBUG, "No mock found for $method $path, proceeding with real request")

                // Record metrics
                metrics?.recordRealRequest()
                metrics?.recordCacheMiss()
            }

        } catch (e: Exception) {
            log(OpenMockerConfig.LogLevel.ERROR, "Error in onRequest: ${e.message}")

            // Create fallback context to ensure request can proceed
            val fallbackContext = RequestContext(mockKey, null, startTime, false)
            request.attributes.put(AttributeKeys.REQUEST_CONTEXT, fallbackContext)
            request.attributes.put(AttributeKeys.MOCK_KEY, mockKey)
        }
    }

    onResponse { response ->
        // Skip if plugin is disabled
        if (!config.isEnabled) {
            return@onResponse
        }

        // Get request context for metrics and logging
        val requestContext = response.call.request.attributes.getOrNull(AttributeKeys.REQUEST_CONTEXT)
        val bypassCache = response.call.request.attributes.getOrNull(AttributeKeys.BYPASS_CACHE) ?: false

        try {
            // Update metrics with response time if context available
            requestContext?.let { context ->
                val responseTime = context.getElapsedTime()
                if (context.isMocked) {
                    metrics?.recordMockedRequest(responseTime)
                    log(OpenMockerConfig.LogLevel.INFO, "Mock response served for ${context.mockKey.method} ${context.mockKey.path} in ${responseTime}ms")
                } else {
                    metrics?.recordRealRequest(responseTime)
                    log(OpenMockerConfig.LogLevel.DEBUG, "Real response for ${context.mockKey.method} ${context.mockKey.path} in ${responseTime}ms")
                }
            }

            // Skip caching if this was a mocked response
            if (bypassCache) {
                log(OpenMockerConfig.LogLevel.DEBUG, "Skipping cache for mocked response")
                return@onResponse
            }

            // Only cache successful responses or if interceptAll is enabled
            if (config.interceptAll || response.status.isSuccessful()) {
                val method = response.call.request.method.value
                val path = extractPathFromUrl(response.call.request.url.toString())
                val code = response.status.value

                log(OpenMockerConfig.LogLevel.DEBUG, "Caching response: $method $path -> $code")

                // Read response body safely without consuming the original stream
                val body = response.readBodySafely()

                mockerEngine.cacheResponse(method, path, code, body)

                log(OpenMockerConfig.LogLevel.INFO, "Response cached for $method $path")
            }

        } catch (e: Exception) {
            log(OpenMockerConfig.LogLevel.ERROR, "Error in onResponse: ${e.message}")
        }
    }
}

