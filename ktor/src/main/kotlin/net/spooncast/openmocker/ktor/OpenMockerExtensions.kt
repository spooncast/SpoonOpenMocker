package net.spooncast.openmocker.ktor

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

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

/**
 * Convenience function to get metrics from an installed OpenMocker plugin.
 *
 * Note: This is a placeholder for Phase 3 when full plugin integration allows
 * accessing plugin instance data. For now, metrics are internal to plugin scope.
 *
 * @param client The HttpClient with OpenMocker installed
 * @return MockingMetrics instance if available, null otherwise
 */
suspend fun HttpClient.getOpenMockerMetrics(): MockingMetrics? {
    // Phase 3: Implement metrics access through plugin instance
    // This requires accessing the plugin's internal metrics instance
    return null
}