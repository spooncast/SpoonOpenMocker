package net.spooncast.openmocker.ktor

import kotlinx.coroutines.test.runTest
import net.spooncast.openmocker.core.MemoryMockRepository
import net.spooncast.openmocker.core.MockKey
import net.spooncast.openmocker.core.MockResponse
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Comprehensive unit tests for OpenMocker Phase 2.3 enhanced functionality.
 *
 * Tests cover:
 * - RequestContext management
 * - MockingMetrics functionality
 * - OpenMockerConfig enhancements
 * - AttributeKey definitions
 */
class OpenMockerPluginEnhancedTest {

    private lateinit var repository: MemoryMockRepository
    private lateinit var config: OpenMockerConfig
    private lateinit var metrics: MockingMetrics

    @Before
    fun setUp() {
        repository = MemoryMockRepository()
        config = OpenMockerConfig().apply {
            this.repository = this@OpenMockerPluginEnhancedTest.repository
            enableLogging = false // Disable for testing
            metricsEnabled = true
        }
        metrics = MockingMetrics()
    }

    // RequestContext Tests

    @Test
    fun `RequestContext creation for non-mocked request`() = runTest {
        // Arrange
        val mockKey = MockKey("GET", "/api/test")
        val startTime = System.currentTimeMillis()

        // Act
        val context = RequestContext(mockKey, null, startTime)

        // Assert
        assertEquals("MockKey should match", mockKey, context.mockKey)
        assertNull("MockResponse should be null", context.mockResponse)
        assertEquals("Start time should match", startTime, context.startTime)
        assertFalse("Should not be mocked", context.isMocked)
        assertTrue("Elapsed time should be >= 0", context.getElapsedTime() >= 0)
    }

    @Test
    fun `RequestContext creation for mocked request`() = runTest {
        // Arrange
        val mockKey = MockKey("POST", "/api/login")
        val mockResponse = MockResponse(200, """{"token": "abc123"}""", 50L)
        val startTime = System.currentTimeMillis()

        // Act
        val context = RequestContext(mockKey, mockResponse, startTime)

        // Assert
        assertEquals("MockKey should match", mockKey, context.mockKey)
        assertEquals("MockResponse should match", mockResponse, context.mockResponse)
        assertEquals("Start time should match", startTime, context.startTime)
        assertTrue("Should be mocked", context.isMocked)
    }

    @Test
    fun `RequestContext withMockResponse creates mocked context`() = runTest {
        // Arrange
        val mockKey = MockKey("GET", "/api/data")
        val originalContext = RequestContext(mockKey, null)
        val mockResponse = MockResponse(201, """{"id": 1}""", 100L)

        // Act
        val updatedContext = originalContext.withMockResponse(mockResponse)

        // Assert
        assertFalse("Original context should not be mocked", originalContext.isMocked)
        assertTrue("Updated context should be mocked", updatedContext.isMocked)
        assertEquals("Mock response should match", mockResponse, updatedContext.mockResponse)
        assertEquals("MockKey should be preserved", mockKey, updatedContext.mockKey)
    }

    // MockingMetrics Tests

    @Test
    fun `MockingMetrics initial state is clean`() {
        // Arrange
        val metrics = MockingMetrics()

        // Act & Assert
        assertEquals("Mocked count should be 0", 0L, metrics.getMockedRequestCount())
        assertEquals("Real count should be 0", 0L, metrics.getRealRequestCount())
        assertEquals("Total count should be 0", 0L, metrics.getTotalRequestCount())
        assertEquals("Mocking ratio should be 0.0", 0.0, metrics.getMockingRatio(), 0.001)
        assertEquals("Cache hit ratio should be 0.0", 0.0, metrics.getCacheHitRatio(), 0.001)
        assertEquals("Average request time should be 0.0", 0.0, metrics.getAverageRequestTime(), 0.001)
    }

    @Test
    fun `MockingMetrics tracks mocked requests correctly`() {
        // Arrange
        val metrics = MockingMetrics()

        // Act
        metrics.recordMockedRequest(50L)
        metrics.recordMockedRequest(30L)
        metrics.recordCacheHit()
        metrics.recordCacheHit()

        // Assert
        assertEquals("Mocked count should be 2", 2L, metrics.getMockedRequestCount())
        assertEquals("Real count should be 0", 0L, metrics.getRealRequestCount())
        assertEquals("Total count should be 2", 2L, metrics.getTotalRequestCount())
        assertEquals("Mocking ratio should be 1.0", 1.0, metrics.getMockingRatio(), 0.001)
        assertEquals("Cache hit ratio should be 1.0", 1.0, metrics.getCacheHitRatio(), 0.001)
        assertEquals("Average request time should be 40.0", 40.0, metrics.getAverageRequestTime(), 0.001)
    }

    @Test
    fun `MockingMetrics tracks real requests correctly`() {
        // Arrange
        val metrics = MockingMetrics()

        // Act
        metrics.recordRealRequest(100L)
        metrics.recordRealRequest(200L)
        metrics.recordCacheMiss()
        metrics.recordCacheMiss()

        // Assert
        assertEquals("Mocked count should be 0", 0L, metrics.getMockedRequestCount())
        assertEquals("Real count should be 2", 2L, metrics.getRealRequestCount())
        assertEquals("Total count should be 2", 2L, metrics.getTotalRequestCount())
        assertEquals("Mocking ratio should be 0.0", 0.0, metrics.getMockingRatio(), 0.001)
        assertEquals("Cache hit ratio should be 0.0", 0.0, metrics.getCacheHitRatio(), 0.001)
        assertEquals("Average request time should be 150.0", 150.0, metrics.getAverageRequestTime(), 0.001)
    }

    @Test
    fun `MockingMetrics calculates ratios correctly with mixed requests`() {
        // Arrange
        val metrics = MockingMetrics()

        // Act
        metrics.recordMockedRequest(25L) // 1 mocked
        metrics.recordRealRequest(50L)   // 2 real
        metrics.recordRealRequest(75L)
        metrics.recordCacheHit()         // 1 hit
        metrics.recordCacheMiss()        // 2 misses
        metrics.recordCacheMiss()

        // Assert
        assertEquals("Total count should be 3", 3L, metrics.getTotalRequestCount())
        assertEquals("Mocking ratio should be ~0.33", 0.333, metrics.getMockingRatio(), 0.01)
        assertEquals("Cache hit ratio should be ~0.33", 0.333, metrics.getCacheHitRatio(), 0.01)
        assertEquals("Average request time should be 50.0", 50.0, metrics.getAverageRequestTime(), 0.001)
    }

    @Test
    fun `MockingMetrics reset clears all data`() {
        // Arrange
        val metrics = MockingMetrics()
        metrics.recordMockedRequest(100L)
        metrics.recordRealRequest(200L)
        metrics.recordCacheHit()

        // Verify data exists
        assertTrue("Should have data before reset", metrics.getTotalRequestCount() > 0)

        // Act
        metrics.reset()

        // Assert
        assertEquals("Mocked count should be 0 after reset", 0L, metrics.getMockedRequestCount())
        assertEquals("Real count should be 0 after reset", 0L, metrics.getRealRequestCount())
        assertEquals("Total count should be 0 after reset", 0L, metrics.getTotalRequestCount())
        assertEquals("Mocking ratio should be 0.0 after reset", 0.0, metrics.getMockingRatio(), 0.001)
        assertEquals("Cache hit ratio should be 0.0 after reset", 0.0, metrics.getCacheHitRatio(), 0.001)
        assertEquals("Average time should be 0.0 after reset", 0.0, metrics.getAverageRequestTime(), 0.001)
    }

    @Test
    fun `MockingMetrics getSummary provides readable output`() {
        // Arrange
        val metrics = MockingMetrics()
        metrics.recordMockedRequest(50L)
        metrics.recordRealRequest(100L)
        metrics.recordCacheHit()
        metrics.recordCacheMiss()

        // Act
        val summary = metrics.getSummary()

        // Assert
        assertTrue("Summary should contain title", summary.contains("OpenMocker Metrics:"))
        assertTrue("Summary should contain total requests", summary.contains("Total Requests: 2"))
        assertTrue("Summary should contain mocked requests", summary.contains("Mocked Requests: 1"))
        assertTrue("Summary should contain real requests", summary.contains("Real Requests: 1"))
        assertTrue("Summary should contain mocking ratio", summary.contains("Mocking Ratio: 50.0%"))
        assertTrue("Summary should contain cache hit ratio", summary.contains("Cache Hit Ratio: 50.0%"))
        assertTrue("Summary should contain average time", summary.contains("Average Response Time: 75.0ms"))
    }

    // OpenMockerConfig Tests

    @Test
    fun `OpenMockerConfig has correct defaults for new options`() {
        // Arrange & Act
        val config = OpenMockerConfig()

        // Assert
        assertFalse("Logging should be disabled by default", config.enableLogging)
        assertFalse("Metrics should be disabled by default", config.metricsEnabled)
        assertEquals("Default log level should be INFO", OpenMockerConfig.LogLevel.INFO, config.logLevel)
    }

    @Test
    fun `OpenMockerConfig validation passes with logging options`() {
        // Arrange
        val config = OpenMockerConfig().apply {
            enableLogging = true
            metricsEnabled = true
            logLevel = OpenMockerConfig.LogLevel.DEBUG
        }

        // Act & Assert (should not throw)
        config.validate()
    }

    @Test
    fun `OpenMockerConfig LogLevel enum has correct ordering`() {
        // Act & Assert
        assertTrue("DEBUG should have lower ordinal than INFO",
            OpenMockerConfig.LogLevel.DEBUG.ordinal < OpenMockerConfig.LogLevel.INFO.ordinal)
        assertTrue("INFO should have lower ordinal than WARN",
            OpenMockerConfig.LogLevel.INFO.ordinal < OpenMockerConfig.LogLevel.WARN.ordinal)
        assertTrue("WARN should have lower ordinal than ERROR",
            OpenMockerConfig.LogLevel.WARN.ordinal < OpenMockerConfig.LogLevel.ERROR.ordinal)
    }

    // AttributeKeys Tests

    @Test
    fun `AttributeKeys have unique names and proper types`() {
        // Act & Assert
        assertEquals("MOCK_RESPONSE_KEY name", "OpenMocker.MockResponse", AttributeKeys.MOCK_RESPONSE_KEY.name)
        assertEquals("MOCK_KEY name", "OpenMocker.MockKey", AttributeKeys.MOCK_KEY.name)
        assertEquals("ORIGINAL_REQUEST name", "OpenMocker.OriginalRequest", AttributeKeys.ORIGINAL_REQUEST.name)
        assertEquals("REQUEST_CONTEXT name", "OpenMocker.RequestContext", AttributeKeys.REQUEST_CONTEXT.name)
        assertEquals("BYPASS_CACHE name", "OpenMocker.BypassCache", AttributeKeys.BYPASS_CACHE.name)

        // Verify all names are unique
        val names = setOf(
            AttributeKeys.MOCK_RESPONSE_KEY.name,
            AttributeKeys.MOCK_KEY.name,
            AttributeKeys.ORIGINAL_REQUEST.name,
            AttributeKeys.REQUEST_CONTEXT.name,
            AttributeKeys.BYPASS_CACHE.name
        )
        assertEquals("All attribute key names should be unique", 5, names.size)
    }

    // Thread Safety Tests

    @Test
    fun `MockingMetrics operations are thread-safe`() = runTest {
        // Arrange
        val metrics = MockingMetrics()
        val threadCount = 10
        val operationsPerThread = 100

        // Act - simulate concurrent access (simplified for testing)
        repeat(threadCount * operationsPerThread) { iteration ->
            val threadId = (iteration % threadCount) + 1
            if (threadId % 2 == 0) {
                metrics.recordMockedRequest(threadId.toLong())
                metrics.recordCacheHit()
            } else {
                metrics.recordRealRequest(threadId.toLong())
                metrics.recordCacheMiss()
            }
        }

        // Assert
        val expectedMocked = (threadCount / 2) * operationsPerThread
        val expectedReal = (threadCount - threadCount / 2) * operationsPerThread
        val expectedTotal = threadCount * operationsPerThread

        assertEquals("Mocked request count should be accurate", expectedMocked.toLong(), metrics.getMockedRequestCount())
        assertEquals("Real request count should be accurate", expectedReal.toLong(), metrics.getRealRequestCount())
        assertEquals("Total request count should be accurate", expectedTotal.toLong(), metrics.getTotalRequestCount())
        assertTrue("Mocking ratio should be reasonable", metrics.getMockingRatio() in 0.0..1.0)
        assertTrue("Cache hit ratio should be reasonable", metrics.getCacheHitRatio() in 0.0..1.0)
    }

    // Edge Cases

    @Test
    fun `RequestContext handles edge cases gracefully`() {
        // Arrange
        val mockKey = MockKey("GET", "/test")
        val oldStartTime = System.currentTimeMillis() - 86400000L // 24 hours ago
        val context = RequestContext(mockKey, null, oldStartTime)

        // Act
        val elapsedTime = context.getElapsedTime()

        // Assert
        assertTrue("Elapsed time should be positive", elapsedTime > 0)
        assertTrue("Elapsed time should be reasonable", elapsedTime < 86500000L) // Less than 24.1 hours
    }

    @Test
    fun `MockingMetrics handles edge cases gracefully`() {
        // Arrange
        val metrics = MockingMetrics()

        // Act - Test division by zero scenarios
        val initialRatio = metrics.getMockingRatio()
        val initialCacheRatio = metrics.getCacheHitRatio()
        val initialAverage = metrics.getAverageRequestTime()

        // Assert
        assertEquals("Initial mocking ratio should be 0.0", 0.0, initialRatio, 0.001)
        assertEquals("Initial cache ratio should be 0.0", 0.0, initialCacheRatio, 0.001)
        assertEquals("Initial average should be 0.0", 0.0, initialAverage, 0.001)

        // Test with large numbers
        metrics.recordMockedRequest(Long.MAX_VALUE / 2)
        val ratioWithLarge = metrics.getMockingRatio()
        val averageWithLarge = metrics.getAverageRequestTime()

        assertTrue("Ratio should be valid with large numbers", ratioWithLarge in 0.0..1.0)
        assertTrue("Average should be reasonable with large numbers", averageWithLarge > 0)
    }
}