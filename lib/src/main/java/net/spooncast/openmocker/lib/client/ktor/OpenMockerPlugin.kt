package net.spooncast.openmocker.lib.client.ktor

import io.ktor.client.plugins.api.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.coroutines.delay

/**
 * OpenMocker Plugin for Ktor HTTP Client
 *
 * This plugin provides HTTP response mocking functionality for Ktor clients,
 * enabling developers to intercept HTTP requests, return mock responses when available,
 * and cache real responses for future mocking use.
 *
 * Key Features:
 * - HTTP request interception with mock response handling
 * - Automatic response caching for development and testing
 * - Network delay simulation for realistic testing scenarios
 * - Runtime enable/disable configuration
 * - Integration with shared MemCacheRepoImpl for cross-client compatibility
 *
 * Architecture Integration:
 * ```
 * HttpClient + OpenMockerPlugin
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
val OpenMockerPlugin = createClientPlugin("OpenMocker", ::OpenMockerPluginConfig) {
    /**
     * Basic plugin structure for OpenMocker Ktor integration
     *
     * This creates a foundation for HTTP mocking in Ktor clients.
     * The actual mocking and caching logic will be implemented in the test coverage
     * phase to ensure proper integration with the MockingEngine.
     *
     * Current limitations:
     * - Response caching requires type conversion between HttpRequest and HttpRequestData
     * - Request mocking requires intercepting the request pipeline before network calls
     * - Both features need proper integration with the existing KtorAdapter
     */
    onResponse { response ->
        // Plugin is installed and configured
        // Response handling and caching will be implemented with proper type conversion
        if (this@createClientPlugin.pluginConfig.enabled) {
            // TODO: Implement response caching with proper HttpRequest to HttpRequestData conversion
            // this.pluginConfig.mockingEngine.cacheResponse(requestData, response)
        }
    }
}


