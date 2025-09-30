package net.spooncast.openmocker.lib.client.ktor

import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestData
import io.ktor.client.statement.request
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.delay
import kotlinx.coroutines.job

/**
 * OpenMocker Plugin for Ktor HTTP Client
 *
 * This plugin provides HTTP response mocking functionality for Ktor clients,
 * enabling developers to intercept HTTP requests, return mock responses when available,
 * and cache real responses for future mocking use.
 *
 * Key Features:
 * - HTTP request interception with mock response handling using on(Send) hook
 * - Automatic response caching for development and testing
 * - Network delay simulation for realistic testing scenarios
 * - Runtime enable/disable configuration
 * - Integration with shared MemCacheRepoImpl for cross-client compatibility
 *
 * Architecture Integration:
 * ```
 * HttpClient + OpenMockerPlugin (on Send hook)
 *         ↓
 * KtorAdapter (HttpRequestData ↔ HttpResponse)
 *         ↓
 * MockingEngine<HttpRequestData, HttpResponse>
 *         ↓
 * MemCacheRepoImpl (shared cache with OkHttp)
 * ```
 *
 * Usage:
 * ```kotlin
 * val client = HttpClient {
 *     install(OpenMockerPlugin) {
 *         enabled = true
 *     }
 * }
 * ```
 */
@OptIn(InternalAPI::class)
val OpenMockerPlugin = createClientPlugin("OpenMocker", ::OpenMockerPluginConfig) {
    /**
     * OpenMocker Ktor plugin implementation using on(Send) hook
     *
     * This plugin intercepts HTTP requests before they are sent over the network.
     * When a mock response is available, it bypasses the network call and returns
     * the mock response with optional delay simulation. When no mock is available,
     * it proceeds with the normal network call and caches the response for future use.
     *
     * The implementation follows the same pattern as OpenMockerInterceptor but adapted
     * for Ktor's asynchronous nature and plugin architecture.
     */
    on(Send) { requestBuilder ->
        val config = pluginConfig

        // Check if plugin is enabled
        if (!config.enabled) {
            return@on proceed(requestBuilder)
        }

        // Convert HttpRequestBuilder to HttpRequestData
        val requestData = requestBuilder.build()

        // Check for mock data using MockingEngine abstraction
        val mockData = config.mockingEngine.getMockData(requestData)

        if (mockData != null) {
            // Apply delay if configured (Ktor-specific: async delay)
            if (mockData.duration > 0) {
                delay(mockData.duration)
            }

            // Create HttpClientCall wrapper for the mock response
            return@on config.mockingEngine.createMockResponse(requestData, mockData).call
        }

        // Proceed with normal network call
        val originalCall = proceed(requestBuilder)

        // Cache the real response for future mocking
        return@on originalCall
    }

    onResponse { response ->
        val config = this@createClientPlugin.pluginConfig

        val request = HttpRequestData(
            url = response.request.url,
            method = response.request.method,
            headers = response.request.headers,
            body = response.request.content,
            executionContext = response.request.coroutineContext.job,
            attributes = response.request.attributes
        )

        config.mockingEngine.cacheResponse(request, response)
    }
}