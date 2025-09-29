package net.spooncast.openmocker.lib.client.ktor

import io.ktor.client.request.*
import io.ktor.client.statement.*
import net.spooncast.openmocker.lib.core.MockingEngine
import net.spooncast.openmocker.lib.core.adapter.KtorAdapter
import net.spooncast.openmocker.lib.repo.MemCacheRepoImpl

/**
 * Configuration class for OpenMocker Ktor Plugin
 *
 * This class provides configuration options for the OpenMocker Ktor plugin integration
 * and manages the lazy initialization of the MockingEngine with proper thread safety.
 *
 * Key features:
 * - Simple configuration with enabled/disabled state
 * - Thread-safe lazy MockingEngine initialization
 * - Integration with shared MemCacheRepoImpl for cross-client cache
 * - KtorAdapter integration for Ktor-specific type conversion
 */
class OpenMockerPluginConfig {

    /**
     * Enables or disables the OpenMocker functionality
     * Default: true (enabled)
     */
    var enabled: Boolean = true

    /**
     * Lazily initialized MockingEngine instance
     *
     * This property creates a MockingEngine instance on first access using:
     * - Shared MemCacheRepoImpl.getInstance() for cross-client cache sharing
     * - KtorAdapter for Ktor-specific HTTP client type conversion
     *
     * The lazy delegate ensures thread-safe initialization - only one instance
     * will be created even if accessed concurrently from multiple threads.
     *
     * Internal visibility prevents external code from manipulating the engine directly
     * while allowing plugin components to access it.
     */
    internal val mockingEngine: MockingEngine<HttpRequestData, HttpResponse> by lazy {
        val cacheRepo = MemCacheRepoImpl.getInstance()
        val adapter = KtorAdapter()
        MockingEngine(cacheRepo, adapter)
    }
}