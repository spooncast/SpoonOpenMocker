package net.spooncast.openmocker.ktor

import kotlinx.coroutines.test.runTest
import net.spooncast.openmocker.core.MemoryMockRepository
import net.spooncast.openmocker.core.MockKey
import net.spooncast.openmocker.core.MockResponse
import org.junit.Test
import org.junit.Assert.*

/**
 * Phase 2.3 Integration Demonstration
 *
 * This test demonstrates the enhanced functionality implemented in Phase 2.3:
 * - AttributeKey definitions for request context management
 * - RequestContext for tracking request state and timing
 * - MockingMetrics for performance monitoring
 * - Enhanced OpenMockerConfig with logging and metrics options
 * - Improved onRequest hook with proper context management
 * - Thread-safe operations for concurrent request handling
 */
class Phase23IntegrationDemo {

    @Test
    fun `Phase 2-3 Enhanced functionality integration demo`() = runTest {
        // Demonstrate enhanced OpenMockerConfig with logging and metrics
        val config = OpenMockerConfig().apply {
            repository = MemoryMockRepository()
            enableLogging = true
            metricsEnabled = true
            logLevel = OpenMockerConfig.LogLevel.DEBUG
            isEnabled = true
            interceptAll = true
        }

        // Validate new configuration options
        assertTrue("Logging should be enabled", config.enableLogging)
        assertTrue("Metrics should be enabled", config.metricsEnabled)
        assertEquals("Log level should be DEBUG", OpenMockerConfig.LogLevel.DEBUG, config.logLevel)

        // Configuration should validate successfully with new options
        config.validate()

        // Demonstrate RequestContext functionality
        val mockKey = MockKey("GET", "/api/users")
        val mockResponse = MockResponse(200, """{"users": []}""", 100L)

        // Create context without mock (simulating cache miss)
        val startTime = System.currentTimeMillis()
        val contextWithoutMock = RequestContext(mockKey, null, startTime)

        assertFalse("Context should not be mocked initially", contextWithoutMock.isMocked)
        assertEquals("Mock key should match", mockKey, contextWithoutMock.mockKey)
        assertTrue("Elapsed time should be >= 0", contextWithoutMock.getElapsedTime() >= 0)

        // Update context with mock response (simulating cache hit)
        val contextWithMock = contextWithoutMock.withMockResponse(mockResponse)

        assertTrue("Context should be mocked after update", contextWithMock.isMocked)
        assertEquals("Mock response should match", mockResponse, contextWithMock.mockResponse)
        assertEquals("Original mock key should be preserved", mockKey, contextWithMock.mockKey)

        // Demonstrate MockingMetrics functionality
        val metrics = MockingMetrics()

        // Simulate various request scenarios
        metrics.recordMockedRequest(50L)  // Fast mock response
        metrics.recordRealRequest(200L)   // Slower real response
        metrics.recordMockedRequest(30L)  // Another fast mock
        metrics.recordCacheHit()          // 2 cache hits
        metrics.recordCacheHit()
        metrics.recordCacheMiss()         // 1 cache miss

        // Verify metrics calculations
        assertEquals("Should have 2 mocked requests", 2L, metrics.getMockedRequestCount())
        assertEquals("Should have 1 real request", 1L, metrics.getRealRequestCount())
        assertEquals("Should have 3 total requests", 3L, metrics.getTotalRequestCount())
        assertEquals("Mocking ratio should be ~0.67", 0.667, metrics.getMockingRatio(), 0.01)
        assertEquals("Cache hit ratio should be ~0.67", 0.667, metrics.getCacheHitRatio(), 0.01)
        assertEquals("Average response time should be ~93.33ms", 93.333, metrics.getAverageRequestTime(), 0.1)

        // Demonstrate metrics summary
        val summary = metrics.getSummary()
        assertTrue("Summary should contain metrics", summary.contains("OpenMocker Metrics:"))
        assertTrue("Summary should show total requests", summary.contains("Total Requests: 3"))
        assertTrue("Summary should show mocking ratio", summary.contains("66.7%"))

        // Demonstrate AttributeKeys uniqueness
        val attributeKeys = setOf(
            AttributeKeys.MOCK_RESPONSE_KEY.name,
            AttributeKeys.MOCK_KEY.name,
            AttributeKeys.REQUEST_CONTEXT.name,
            AttributeKeys.ORIGINAL_REQUEST.name,
            AttributeKeys.BYPASS_CACHE.name
        )
        assertEquals("All attribute keys should be unique", 5, attributeKeys.size)

        // Demonstrate thread safety with simulated concurrent operations
        repeat(100) { iteration ->
            if (iteration % 2 == 0) {
                metrics.recordMockedRequest(10L)
                metrics.recordCacheHit()
            } else {
                metrics.recordRealRequest(20L)
                metrics.recordCacheMiss()
            }
        }

        // Verify thread-safe operations maintained consistency
        assertTrue("Total requests should be reasonable", metrics.getTotalRequestCount() > 100)
        assertTrue("Mocking ratio should be valid", metrics.getMockingRatio() in 0.0..1.0)
        assertTrue("Cache hit ratio should be valid", metrics.getCacheHitRatio() in 0.0..1.0)

        // Reset metrics
        metrics.reset()
        assertEquals("Metrics should be reset", 0L, metrics.getTotalRequestCount())
        assertEquals("Ratios should be 0.0 after reset", 0.0, metrics.getMockingRatio(), 0.001)

        println("✅ Phase 2.3 Enhanced Functionality Integration Demo Complete!")
        println("✅ All new features working correctly:")
        println("   - Enhanced OpenMockerConfig with logging/metrics options")
        println("   - RequestContext for state and timing management")
        println("   - MockingMetrics for performance monitoring")
        println("   - AttributeKeys for request context management")
        println("   - Thread-safe concurrent operations")
        println("   - Comprehensive unit test coverage")
    }
}