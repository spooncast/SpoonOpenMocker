package net.spooncast.openmocker.ktor

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.test.runTest
import net.spooncast.openmocker.core.MemoryMockRepository
import org.junit.Assert.*
import org.junit.Test

/**
 * Basic compilation and instantiation tests for OpenMocker Ktor plugin.
 *
 * These tests verify that Phase 2.2 requirements are met:
 * - Plugin can be compiled without errors
 * - Basic classes can be instantiated
 * - Configuration validation works
 * - Utility functions work correctly
 * - KtorMockerEngine implementation is functional
 * - Mock response creation is working
 * - Request/response adapters are functional
 *
 * Note: Full plugin integration tests with actual HTTP clients will be added
 * in Phase 3 when cross-platform testing is implemented.
 */
class OpenMockerPluginCompilationTest {

    // BDD: Given default configuration requirements, When plugin configuration is instantiated, Then use correct default values
    @Test
    fun `plugin configuration can be instantiated with default values`() {
        // Arrange & Act
        val config = OpenMockerConfig()

        // Assert - Check default values
        assertNotNull(config.repository)
        assertTrue("Plugin should be enabled by default", config.isEnabled)
        assertTrue("Intercept all should be true by default", config.interceptAll)
        assertEquals("Max cache size should default to -1", -1, config.maxCacheSize)
        assertFalse("Auto enable in debug should default to false", config.autoEnableInDebug)
    }

    // BDD: Given custom configuration values, When plugin configuration is modified, Then accept and store custom settings
    @Test
    fun `plugin configuration can be customized`() {
        // Arrange
        val customRepo = MemoryMockRepository()
        val config = OpenMockerConfig()

        // Act - Customize configuration
        config.repository = customRepo
        config.isEnabled = false
        config.interceptAll = false
        config.maxCacheSize = 100
        config.autoEnableInDebug = true

        // Assert - Verify customization worked
        assertSame("Custom repository should be set", customRepo, config.repository)
        assertFalse("Plugin should be disabled", config.isEnabled)
        assertFalse("Intercept all should be false", config.interceptAll)
        assertEquals("Max cache size should be 100", 100, config.maxCacheSize)
        assertTrue("Auto enable in debug should be true", config.autoEnableInDebug)
    }

    // BDD: Given default factory function, When KtorMockerEngine is created, Then instantiate with default repository
    @Test
    fun `mocker engine can be created with default repository`() {
        // Act
        val engine = createKtorMockerEngine()

        // Assert
        assertNotNull(engine)
    }

    // BDD: Given custom repository, When KtorMockerEngine is created, Then use provided repository
    @Test
    fun `mocker engine can be created with custom repository`() {
        // Arrange
        val customRepo = MemoryMockRepository()

        // Act
        val engine = KtorMockerEngine(customRepo)

        // Assert
        assertNotNull(engine)
    }

    // BDD: Given configuration validation rules, When validate is called, Then accept valid settings and reject invalid ones
    @Test
    fun `configuration validation works correctly`() {
        // Arrange
        val config = OpenMockerConfig()

        // Act & Assert - Valid configurations should pass
        config.maxCacheSize = 100
        config.validate() // Should not throw

        config.maxCacheSize = -1
        config.validate() // Should not throw (unlimited)

        // Act & Assert - Invalid configuration should fail
        var exceptionThrown = false
        try {
            config.maxCacheSize = 0
            config.validate()
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }

        assertTrue("Expected validation to fail for maxCacheSize = 0", exceptionThrown)
    }

    // BDD: Given MockResponse creation parameters, When KtorUtils functions are called, Then work correctly with phase 2.2 implementation
    @Test
    fun `ktorutils functions work correctly with phase 2_2 implementation`() = runTest {
        // Arrange
        val mockResponse = net.spooncast.openmocker.core.MockResponse(
            code = 200,
            body = """{"test": "data"}""",
            delay = 50L
        )

        // Act - Test delay application (verify function works without error)
        KtorUtils.applyMockDelay(mockResponse)

        // Assert (function completed successfully)
        assertTrue("Expected delay function to complete", true)

        // Act - Test content type detection
        val contentType = KtorUtils.detectContentType(mockResponse.body)

        // Assert
        assertEquals("Expected JSON content type", io.ktor.http.ContentType.Application.Json, contentType)
    }

    // BDD: Given engine functionality requirements, When mocker engine operations are called, Then work correctly in phase 2.2
    @Test
    fun `mocker engine phase 2_2 functionality works correctly`() = runTest {
        // Arrange
        val engine = createKtorMockerEngine()
        val key = net.spooncast.openmocker.core.MockKey("GET", "/test")
        val response = net.spooncast.openmocker.core.MockResponse(200, "test data")

        // Act - Test mock operations
        val mockResult = engine.mock(key, response)
        val shouldMockResult = engine.shouldMock("GET", "/test")
        val unmockResult = engine.unmock(key)

        // Assert
        assertTrue("Expected mock to be saved successfully", mockResult)
        assertNotNull("Expected mock to be found", shouldMockResult)
        assertEquals("Expected matching response", response, shouldMockResult)
        assertTrue("Expected mock to be removed successfully", unmockResult)
    }

    // BDD: Given caching functionality requirements, When cache operations are called, Then work correctly in phase 2.2
    @Test
    fun `cache functionality phase 2_2 works correctly`() = runTest {
        // Arrange
        val engine = createKtorMockerEngine()
        val method = "POST"
        val path = "/api/submit"
        val code = 201
        val body = "Created successfully"

        // Act - Test caching
        engine.cacheResponse(method, path, code, body)
        val cachedResponses = engine.getAllCachedResponses()

        // Assert
        assertEquals("Expected one cached response", 1, cachedResponses.size)
        val cachedResponse = cachedResponses[net.spooncast.openmocker.core.MockKey(method, path)]
        assertNotNull("Expected cached response to exist", cachedResponse)
        assertEquals("Expected matching code", code, cachedResponse!!.code)
        assertEquals("Expected matching body", body, cachedResponse.body)
    }

    // BDD: Given convenience methods requirements, When utility methods are called, Then work correctly in phase 2.2
    @Test
    fun `convenience methods phase 2_2 work correctly`() = runTest {
        // Arrange
        val engine = createKtorMockerEngine()

        // Act - Test convenience methods
        val mockKey = engine.createMockKey("PATCH", "/api/update")
        val initialMocks = engine.getAllMocks()
        val initialCached = engine.getAllCachedResponses()

        // Add some data
        engine.mock(mockKey, net.spooncast.openmocker.core.MockResponse(200, "updated"))
        engine.cacheResponse("GET", "/api/data", 200, "data")

        val mocksAfterAdd = engine.getAllMocks()
        val cachedAfterAdd = engine.getAllCachedResponses()

        // Clear all
        engine.clearAll()
        val mocksAfterClear = engine.getAllMocks()
        val cachedAfterClear = engine.getAllCachedResponses()

        // Assert
        assertEquals("Expected correct mock key method", "PATCH", mockKey.method)
        assertEquals("Expected correct mock key path", "/api/update", mockKey.path)
        assertTrue("Expected initial mocks to be empty", initialMocks.isEmpty())
        assertTrue("Expected initial cached to be empty", initialCached.isEmpty())
        assertEquals("Expected one mock after add", 1, mocksAfterAdd.size)
        assertEquals("Expected one cached after add", 1, cachedAfterAdd.size)
        assertTrue("Expected mocks to be cleared", mocksAfterClear.isEmpty())
        assertTrue("Expected cached to be cleared", cachedAfterClear.isEmpty())
    }

    // BDD: Given URL with path components, When adapter functions are called, Then compile and execute correctly
    @Test
    fun `adapter functions compile correctly`() {
        // Arrange
        val url = "https://api.example.com/test/path?param=value#fragment"

        // Act
        val extractedPath = extractPathFromUrl(url)

        // Assert
        assertEquals("Expected correct path extraction", "/test/path", extractedPath)
    }

    // BDD: Given various URL formats with edge cases, When extractPathFromUrl is called, Then handle all formats correctly
    @Test
    fun `url path extraction handles edge cases`() {
        // Arrange
        val testCases = mapOf(
            "https://api.example.com/" to "/",
            "https://api.example.com" to "/",
            "https://api.example.com/path" to "/path",
            "https://api.example.com/path/" to "/path/",
            "https://api.example.com/path?query=value" to "/path",
            "https://api.example.com/path#fragment" to "/path",
            "https://api.example.com/path?query=value#fragment" to "/path",
            "https://api.example.com/complex/nested/path" to "/complex/nested/path"
        )

        // Act & Assert
        testCases.forEach { (url, expectedPath) ->
            val actualPath = extractPathFromUrl(url)
            assertEquals("For URL '$url'", expectedPath, actualPath)
        }
    }
}