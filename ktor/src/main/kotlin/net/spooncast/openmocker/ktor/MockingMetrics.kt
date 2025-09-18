package net.spooncast.openmocker.ktor

import java.util.concurrent.atomic.AtomicLong

/**
 * Performance metrics tracking for mocked vs real requests.
 * Provides thread-safe counters and calculations for monitoring mock performance.
 */
class MockingMetrics {
    private val mockedRequestCount = AtomicLong(0)
    private val realRequestCount = AtomicLong(0)
    private val cacheHitCount = AtomicLong(0)
    private val cacheMissCount = AtomicLong(0)
    private val totalRequestTime = AtomicLong(0)

    /**
     * Record a mocked request.
     * @param responseTime Request processing time in milliseconds
     */
    fun recordMockedRequest(responseTime: Long = 0) {
        mockedRequestCount.incrementAndGet()
        totalRequestTime.addAndGet(responseTime)
    }

    /**
     * Record a real (non-mocked) request.
     * @param responseTime Request processing time in milliseconds
     */
    fun recordRealRequest(responseTime: Long = 0) {
        realRequestCount.incrementAndGet()
        totalRequestTime.addAndGet(responseTime)
    }

    /**
     * Record a cache hit (mock found).
     */
    fun recordCacheHit() {
        cacheHitCount.incrementAndGet()
    }

    /**
     * Record a cache miss (no mock found).
     */
    fun recordCacheMiss() {
        cacheMissCount.incrementAndGet()
    }

    /**
     * Get total number of mocked requests.
     */
    fun getMockedRequestCount(): Long = mockedRequestCount.get()

    /**
     * Get total number of real requests.
     */
    fun getRealRequestCount(): Long = realRequestCount.get()

    /**
     * Get total number of requests (mocked + real).
     */
    fun getTotalRequestCount(): Long = mockedRequestCount.get() + realRequestCount.get()

    /**
     * Calculate mocking ratio as percentage.
     * @return Mocking ratio (0.0 to 1.0), or 0.0 if no requests
     */
    fun getMockingRatio(): Double {
        val total = getTotalRequestCount()
        return if (total > 0) mockedRequestCount.get().toDouble() / total else 0.0
    }

    /**
     * Get cache hit ratio.
     * @return Cache hit ratio (0.0 to 1.0), or 0.0 if no cache operations
     */
    fun getCacheHitRatio(): Double {
        val total = cacheHitCount.get() + cacheMissCount.get()
        return if (total > 0) cacheHitCount.get().toDouble() / total else 0.0
    }

    /**
     * Get average request processing time.
     * @return Average time in milliseconds, or 0.0 if no requests
     */
    fun getAverageRequestTime(): Double {
        val total = getTotalRequestCount()
        return if (total > 0) totalRequestTime.get().toDouble() / total else 0.0
    }

    /**
     * Reset all metrics counters.
     */
    fun reset() {
        mockedRequestCount.set(0)
        realRequestCount.set(0)
        cacheHitCount.set(0)
        cacheMissCount.set(0)
        totalRequestTime.set(0)
    }

    /**
     * Get a summary of current metrics.
     * @return Human-readable metrics summary
     */
    fun getSummary(): String {
        return buildString {
            appendLine("OpenMocker Metrics:")
            appendLine("  Total Requests: ${getTotalRequestCount()}")
            appendLine("  Mocked Requests: ${getMockedRequestCount()}")
            appendLine("  Real Requests: ${getRealRequestCount()}")
            appendLine("  Mocking Ratio: ${String.format("%.1f%%", getMockingRatio() * 100)}")
            appendLine("  Cache Hit Ratio: ${String.format("%.1f%%", getCacheHitRatio() * 100)}")
            appendLine("  Average Response Time: ${String.format("%.1fms", getAverageRequestTime())}")
        }
    }
}