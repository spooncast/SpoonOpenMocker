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
 * These tests verify that Phase 2.1 requirements are met:
 * - Plugin can be compiled without errors
 * - Basic classes can be instantiated
 * - Configuration validation works
 * - Utility functions work correctly
 *
 * Note: Full plugin integration tests will be added in Phase 2.2 when
 * the actual mock response creation is implemented.
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