package net.spooncast.openmocker.ktor

import kotlinx.coroutines.test.runTest
import net.spooncast.openmocker.core.MemoryMockRepository
import net.spooncast.openmocker.core.MockKey
import net.spooncast.openmocker.core.MockResponse
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class KtorMockerEngineTest {

    private lateinit var repository: MemoryMockRepository
    private lateinit var engine: KtorMockerEngine

    @Before
    fun setUp() {
        // Create engine with real repository for integration testing
        repository = MemoryMockRepository()
        engine = KtorMockerEngine(repository)
    }

    // shouldMock() tests

    // BDD: Given no active mock exists, When shouldMock is called, Then return null
    @Test
    fun `shouldMock returns null when no mock exists`() = runTest {
        // Arrange
        val method = "GET"
        val path = "/api/users"

        // Act
        val result = engine.shouldMock(method, path)

        // Assert
        assertNull("Expected null when no mock exists", result)
    }

    // BDD: Given active mock exists, When shouldMock is called, Then return MockResponse
    @Test
    fun `shouldMock returns mock response when mock exists`() = runTest {
        // Arrange
        val method = "POST"
        val path = "/api/login"
        val expectedResponse = MockResponse(code = 200, body = """{"success": true}""", delay = 100)
        val key = MockKey(method, path)

        // Setup mock in repository
        engine.mock(key, expectedResponse)

        // Act
        val result = engine.shouldMock(method, path)

        // Assert
        assertNotNull("Expected mock response to exist", result)
        assertEquals("Expected matching response code", 200, result!!.code)
        assertEquals("Expected matching response body", """{"success": true}""", result.body)
        assertEquals("Expected matching delay", 100L, result.delay)
    }

    // cacheResponse() tests

    // BDD: Given valid HTTP response info, When cacheResponse is called, Then store in repository
    @Test
    fun `cacheResponse stores response in repository`() = runTest {
        // Arrange
        val method = "GET"
        val path = "/api/users"
        val code = 200
        val body = """[{"id": 1, "name": "John"}]"""

        // Act
        engine.cacheResponse(method, path, code, body)

        // Assert
        val cachedResponses = engine.getAllCachedResponses()
        val key = MockKey(method, path)
        assertTrue("Expected cached response to exist", cachedResponses.containsKey(key))

        val cachedResponse = cachedResponses[key]!!
        assertEquals("Expected matching code", code, cachedResponse.code)
        assertEquals("Expected matching body", body, cachedResponse.body)
        assertEquals("Expected zero delay for cached response", 0L, cachedResponse.delay)
    }

    // mock() tests

    // BDD: Given valid MockKey and MockResponse, When mock is called, Then save successfully and return true
    @Test
    fun `mock saves mock successfully and returns true`() = runTest {
        // Arrange
        val key = MockKey("GET", "/api/test")
        val response = MockResponse(code = 404, body = "Not found", delay = 50)

        // Act
        val result = engine.mock(key, response)

        // Assert
        assertTrue("Expected mock to be saved successfully", result)

        // Verify it's actually stored
        val savedResponse = engine.shouldMock("GET", "/api/test")
        assertEquals("Expected saved response to match", response, savedResponse)
    }

    // BDD: Given invalid MockKey with blank method, When mock is called, Then return false
    @Test
    fun `mock returns false when key method is blank`() = runTest {
        // Arrange
        val invalidKey = MockKey("", "/api/test")
        val response = MockResponse(code = 200, body = "OK")

        // Act
        val result = engine.mock(invalidKey, response)

        // Assert
        assertFalse("Expected mock to fail with blank method", result)
    }

    // BDD: Given invalid MockKey with blank path, When mock is called, Then return false
    @Test
    fun `mock returns false when key path is blank`() = runTest {
        // Arrange
        val invalidKey = MockKey("GET", "")
        val response = MockResponse(code = 200, body = "OK")

        // Act
        val result = engine.mock(invalidKey, response)

        // Assert
        assertFalse("Expected mock to fail with blank path", result)
    }

    // BDD: Given MockResponse with invalid status code, When mock is called, Then return false
    @Test
    fun `mock returns false when response code is invalid`() = runTest {
        // Arrange
        val key = MockKey("GET", "/api/test")
        val invalidResponse = MockResponse(code = 999, body = "Invalid")

        // Act
        val result = engine.mock(key, invalidResponse)

        // Assert
        assertFalse("Expected mock to fail with invalid status code", result)
    }

    // unmock() tests

    // BDD: Given existing stored mock, When unmock is called, Then remove successfully and return true
    @Test
    fun `unmock removes mock successfully and returns true`() = runTest {
        // Arrange
        val key = MockKey("DELETE", "/api/user/1")
        val response = MockResponse(200, "Deleted")

        // Setup mock first
        engine.mock(key, response)

        // Act
        val result = engine.unmock(key)

        // Assert
        assertTrue("Expected mock to be removed successfully", result)

        // Verify it's actually removed
        val removedResponse = engine.shouldMock("DELETE", "/api/user/1")
        assertNull("Expected mock to be removed", removedResponse)
    }

    // BDD: Given no stored mock, When unmock is called, Then return false
    @Test
    fun `unmock returns false when no mock exists`() = runTest {
        // Arrange
        val key = MockKey("GET", "/api/nonexistent")

        // Act
        val result = engine.unmock(key)

        // Assert
        assertFalse("Expected unmock to return false when no mock exists", result)
    }

    // Convenience method tests

    // BDD: Given HTTP method and path, When createMockKey is called, Then create MockKey
    @Test
    fun `createMockKey creates mock key with method and path`() {
        // Arrange
        val method = "PATCH"
        val path = "/api/update"

        // Act
        val result = engine.createMockKey(method, path)

        // Assert
        assertEquals("Expected matching method", method, result.method)
        assertEquals("Expected matching path", path, result.path)
    }

    // BDD: Given real repository, When getAllMocks is called, Then return all mocks from repository
    @Test
    fun `getAllMocks returns all mocks from repository`() = runTest {
        // Arrange
        val key1 = MockKey("GET", "/api/users")
        val response1 = MockResponse(200, "users data")
        val key2 = MockKey("POST", "/api/users")
        val response2 = MockResponse(201, "user created")

        engine.mock(key1, response1)
        engine.mock(key2, response2)

        // Act
        val result = engine.getAllMocks()

        // Assert
        assertEquals("Expected 2 mocks", 2, result.size)
        assertEquals("Expected first mock", response1, result[key1])
        assertEquals("Expected second mock", response2, result[key2])
    }

    // BDD: Given real repository, When getAllCachedResponses is called, Then return all cached responses
    @Test
    fun `getAllCachedResponses returns all cached responses from repository`() = runTest {
        // Arrange
        engine.cacheResponse("GET", "/api/data", 200, "cached data")
        engine.cacheResponse("POST", "/api/submit", 201, "submitted")

        // Act
        val result = engine.getAllCachedResponses()

        // Assert
        assertEquals("Expected 2 cached responses", 2, result.size)
        assertTrue("Expected GET /api/data to be cached",
            result.containsKey(MockKey("GET", "/api/data")))
        assertTrue("Expected POST /api/submit to be cached",
            result.containsKey(MockKey("POST", "/api/submit")))
    }

    // BDD: Given repository with mocks and cached responses, When clearAll is called, Then clear all data
    @Test
    fun `clearAll clears all mocks and cached responses`() = runTest {
        // Arrange
        val key = MockKey("GET", "/api/test")
        val response = MockResponse(200, "test data")
        engine.mock(key, response)
        engine.cacheResponse("GET", "/api/cached", 200, "cached")

        // Act
        engine.clearAll()

        // Assert
        val mocks = engine.getAllMocks()
        val cached = engine.getAllCachedResponses()
        assertTrue("Expected all mocks to be cleared", mocks.isEmpty())
        assertTrue("Expected all cached responses to be cleared", cached.isEmpty())
    }

    // Factory function test

    // BDD: Given factory function, When createKtorMockerEngine is called, Then create engine with default repository
    @Test
    fun `factory function creates engine with default repository`() {
        // Act
        val engine = createKtorMockerEngine()

        // Assert
        assertNotNull("Expected engine to be created", engine)
    }

    // Integration tests

    // BDD: Given complete workflow, When mock operations are performed, Then work end-to-end
    @Test
    fun `complete mock workflow works end to end`() = runTest {
        // Arrange
        val key = MockKey("PUT", "/api/resource/123")
        val response = MockResponse(code = 204, body = "", delay = 25)

        // Act & Assert - Complete workflow

        // 1. Initially no mock
        assertNull("Expected no initial mock", engine.shouldMock("PUT", "/api/resource/123"))

        // 2. Add mock
        assertTrue("Expected mock to be added", engine.mock(key, response))

        // 3. Mock should be found
        val foundMock = engine.shouldMock("PUT", "/api/resource/123")
        assertNotNull("Expected mock to be found", foundMock)
        assertEquals("Expected matching mock", response, foundMock)

        // 4. Remove mock
        assertTrue("Expected mock to be removed", engine.unmock(key))

        // 5. Mock should no longer be found
        assertNull("Expected no mock after removal", engine.shouldMock("PUT", "/api/resource/123"))
    }
}