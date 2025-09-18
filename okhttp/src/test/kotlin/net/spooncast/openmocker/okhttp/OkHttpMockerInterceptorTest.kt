package net.spooncast.openmocker.okhttp

import kotlinx.coroutines.test.runTest
import net.spooncast.openmocker.core.MemoryMockRepository
import net.spooncast.openmocker.core.MockKey
import net.spooncast.openmocker.core.MockResponse
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse as MockWebServerResponse
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.system.measureTimeMillis

class OkHttpMockerInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var interceptor: OkHttpMockerInterceptor
    private lateinit var repository: MemoryMockRepository
    private lateinit var okHttpClient: OkHttpClient

    @Before
    fun setUp() {
        // Set up MockWebServer
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // Create interceptor with memory repository
        repository = MemoryMockRepository()
        interceptor = OkHttpMockerInterceptor.Builder()
            .repository(repository)
            .build()

        // Create OkHttp client with our interceptor
        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // Builder tests
    // BDD: Given default configuration, When Builder.build is called, Then create interceptor with default repository
    @Test
    fun `Builder creates interceptor with default repository`() {
        // Act
        val result = OkHttpMockerInterceptor.Builder().build()

        // Assert
        assertNotNull(result)
        assertNotNull(result.getEngine())
    }

    // BDD: Given custom repository, When Builder.build is called, Then create interceptor with custom repository
    @Test
    fun `Builder creates interceptor with custom repository`() {
        // Arrange
        val customRepo = MemoryMockRepository()

        // Act
        val result = OkHttpMockerInterceptor.Builder()
            .repository(customRepo)
            .build()

        // Assert
        assertNotNull(result)
        assertNotNull(result.getEngine())
    }

    // BDD: Given companion object methods, When create methods are called, Then create interceptors correctly
    @Test
    fun `Companion create methods work correctly`() {
        // Act
        val defaultInterceptor = OkHttpMockerInterceptor.create()
        val customInterceptor = OkHttpMockerInterceptor.create(MemoryMockRepository())
        val builderInterceptor = OkHttpMockerInterceptor.builder().build()

        // Assert
        assertNotNull(defaultInterceptor)
        assertNotNull(customInterceptor)
        assertNotNull(builderInterceptor)
    }

    // Intercept method tests - No mock scenario
    // BDD: Given no existing mock, When intercept is called, Then proceed with real request
    @Test
    fun `intercept proceeds with real request when no mock exists`() {
        // Arrange
        val expectedResponseBody = """{"real": "response"}"""
        mockWebServer.enqueue(MockWebServerResponse()
            .setResponseCode(200)
            .setBody(expectedResponseBody)
            .setHeader("Content-Type", "application/json"))

        val request = Request.Builder()
            .url(mockWebServer.url("/api/users"))
            .get()
            .build()

        // Act
        val response = okHttpClient.newCall(request).execute()

        // Assert
        assertEquals(200, response.code)
        assertEquals(expectedResponseBody, response.body?.string())
        assertEquals(1, mockWebServer.requestCount) // Real request was made
    }

    // BDD: Given real HTTP request, When intercept is called, Then cache response for future mocking
    @Test
    fun `intercept caches real response for future mocking`() = runTest {
        // Arrange
        val expectedResponseBody = """{"cached": "response"}"""
        mockWebServer.enqueue(MockWebServerResponse()
            .setResponseCode(201)
            .setBody(expectedResponseBody))

        val request = Request.Builder()
            .url(mockWebServer.url("/api/create"))
            .post("{}".toRequestBody())
            .build()

        // Act
        val response = okHttpClient.newCall(request).execute()

        // Assert
        assertEquals(201, response.code)
        assertEquals(expectedResponseBody, response.body?.string())

        // Verify response was cached
        val cachedResponses = interceptor.getAllCachedResponses()
        assertEquals(1, cachedResponses.size)

        val cachedKey = MockKey("POST", "/api/create")
        assertTrue(cachedResponses.containsKey(cachedKey))

        val cachedResponse = cachedResponses[cachedKey]
        assertEquals(201, cachedResponse?.code)
        assertEquals(expectedResponseBody, cachedResponse?.body)
    }

    // Intercept method tests - Mock scenario
    // BDD: Given existing mock, When intercept is called, Then return mock response instead of real request
    @Test
    fun `intercept returns mock response when mock exists`() = runTest {
        // Arrange
        val mockKey = MockKey("GET", "/api/mocked")
        val mockResponse = MockResponse(418, """{"mocked": true}""", 0L)

        // Add mock to repository
        val engine = interceptor.getEngine()
        engine.mock(mockKey, mockResponse)

        val request = Request.Builder()
            .url(mockWebServer.url("/api/mocked"))
            .get()
            .build()

        // Act
        val response = okHttpClient.newCall(request).execute()

        // Assert
        assertEquals(418, response.code)
        assertEquals("""{"mocked": true}""", response.body?.string())
        assertEquals("Unknown", response.message) // Our implementation creates standard HTTP message
        assertEquals(0, mockWebServer.requestCount) // No real request was made
    }

    // BDD: Given mock with delay, When intercept is called, Then apply specified delay duration
    @Test
    fun `intercept applies mock delay correctly`() = runTest {
        // Arrange
        val mockKey = MockKey("GET", "/api/delayed")
        val mockResponse = MockResponse(200, """{"delayed": true}""", 100L)

        val engine = interceptor.getEngine()
        engine.mock(mockKey, mockResponse)

        val request = Request.Builder()
            .url(mockWebServer.url("/api/delayed"))
            .get()
            .build()

        // Act
        val elapsedTime = measureTimeMillis {
            val response = okHttpClient.newCall(request).execute()
            assertEquals(200, response.code)
            assertEquals("""{"delayed": true}""", response.body?.string())
        }

        // Assert
        assertTrue("Request should take at least 100ms due to mock delay, but took ${elapsedTime}ms",
            elapsedTime >= 90) // Allow some tolerance
        assertEquals(0, mockWebServer.requestCount) // No real request was made
    }

    // BDD: Given various HTTP methods, When intercept is called, Then handle all methods correctly
    @Test
    fun `intercept handles different HTTP methods correctly`() = runTest {
        // Test data for different HTTP methods
        val testCases = listOf(
            Triple("GET", "/api/get", { Request.Builder().url(mockWebServer.url("/api/get")).get() }),
            Triple("POST", "/api/post", { Request.Builder().url(mockWebServer.url("/api/post")).post("{}".toRequestBody()) }),
            Triple("PUT", "/api/put", { Request.Builder().url(mockWebServer.url("/api/put")).put("{}".toRequestBody()) }),
            Triple("DELETE", "/api/delete", { Request.Builder().url(mockWebServer.url("/api/delete")).delete() })
        )

        val engine = interceptor.getEngine()

        for ((method, path, requestBuilder) in testCases) {
            // Arrange
            val mockKey = MockKey(method, path)
            val mockResponse = MockResponse(200, """{"method": "$method"}""")
            engine.mock(mockKey, mockResponse)

            // Act
            val request = requestBuilder().build()
            val response = okHttpClient.newCall(request).execute()

            // Assert
            assertEquals("Status code should be 200 for $method", 200, response.code)
            assertEquals("Body should contain method for $method",
                """{"method": "$method"}""", response.body?.string())
        }

        // Verify no real requests were made
        assertEquals(0, mockWebServer.requestCount)
    }

    // Management method tests
    // BDD: When getEngine is called, Then provide access to underlying MockerEngine
    @Test
    fun `getEngine provides access to underlying engine`() {
        // Act
        val engine = interceptor.getEngine()

        // Assert
        assertNotNull(engine)
        assertTrue(engine is OkHttpMockerEngine)
    }

    // BDD: Given existing mocks, When getAllMocks is called, Then return all current mocks
    @Test
    fun `getAllMocks returns current mocks`() = runTest {
        // Arrange
        val mockKey = MockKey("GET", "/api/test")
        val mockResponse = MockResponse(200, "test response")
        val engine = interceptor.getEngine()
        engine.mock(mockKey, mockResponse)

        // Act
        val allMocks = interceptor.getAllMocks()

        // Assert
        assertEquals(1, allMocks.size)
        assertTrue(allMocks.containsKey(mockKey))
        assertEquals(mockResponse, allMocks[mockKey])
    }

    // BDD: Given cached responses from real requests, When getAllCachedResponses is called, Then return cached data
    @Test
    fun `getAllCachedResponses returns cached responses`() = runTest {
        // Arrange - Make a real request to cache response
        mockWebServer.enqueue(MockWebServerResponse()
            .setResponseCode(200)
            .setBody("cached response"))

        val request = Request.Builder()
            .url(mockWebServer.url("/api/cache"))
            .get()
            .build()

        okHttpClient.newCall(request).execute() // This should cache the response

        // Act
        val cachedResponses = interceptor.getAllCachedResponses()

        // Assert
        assertEquals(1, cachedResponses.size)
        val cachedKey = MockKey("GET", "/api/cache")
        assertTrue(cachedResponses.containsKey(cachedKey))
        assertEquals("cached response", cachedResponses[cachedKey]?.body)
    }

    // BDD: Given mocks and cached responses, When clearAll is called, Then remove all data
    @Test
    fun `clearAll removes all mocks and cached responses`() = runTest {
        // Arrange - Add mock and make real request
        val mockKey = MockKey("GET", "/api/mock")
        val mockResponse = MockResponse(200, "mock response")
        val engine = interceptor.getEngine()
        engine.mock(mockKey, mockResponse)

        mockWebServer.enqueue(MockWebServerResponse()
            .setResponseCode(200)
            .setBody("real response"))

        val request = Request.Builder()
            .url(mockWebServer.url("/api/real"))
            .get()
            .build()

        okHttpClient.newCall(request).execute()

        // Verify we have data before clearing
        assertEquals(1, interceptor.getAllMocks().size)
        assertEquals(1, interceptor.getAllCachedResponses().size)

        // Act
        interceptor.clearAll()

        // Assert
        assertEquals(0, interceptor.getAllMocks().size)
        assertEquals(0, interceptor.getAllCachedResponses().size)
    }

    // Integration test
    // BDD: 통합 테스트 - 캐싱, 모킹, 클리어의 완전한 워크플로우가 정상 동작해야 한다
    @Test
    fun `complete workflow - cache, mock, and clear`() = runTest {
        // Step 1: Make real request (should be cached)
        mockWebServer.enqueue(MockWebServerResponse()
            .setResponseCode(200)
            .setBody("""{"step": 1}"""))

        val request = Request.Builder()
            .url(mockWebServer.url("/api/workflow"))
            .get()
            .build()

        val realResponse = okHttpClient.newCall(request).execute()
        assertEquals(200, realResponse.code)
        assertEquals("""{"step": 1}""", realResponse.body?.string())
        assertEquals(1, interceptor.getAllCachedResponses().size)

        // Step 2: Add a mock for the same endpoint
        val mockKey = MockKey("GET", "/api/workflow")
        val mockResponse = MockResponse(201, """{"step": 2, "mocked": true}""")
        val engine = interceptor.getEngine()
        engine.mock(mockKey, mockResponse)

        // Step 3: Make same request again (should return mock)
        val mockedResponseResult = okHttpClient.newCall(request).execute()
        assertEquals(201, mockedResponseResult.code)
        assertEquals("""{"step": 2, "mocked": true}""", mockedResponseResult.body?.string())
        assertEquals(1, mockWebServer.requestCount) // Still only 1 real request

        // Step 4: Remove mock
        engine.unmock(mockKey)

        // Step 5: Make request again (should make real request)
        mockWebServer.enqueue(MockWebServerResponse()
            .setResponseCode(200)
            .setBody("""{"step": 3}"""))

        val finalResponse = okHttpClient.newCall(request).execute()
        assertEquals(200, finalResponse.code)
        assertEquals("""{"step": 3}""", finalResponse.body?.string())
        assertEquals(2, mockWebServer.requestCount) // Now 2 real requests

        // Step 6: Clear everything
        interceptor.clearAll()
        assertEquals(0, interceptor.getAllMocks().size)
        assertEquals(0, interceptor.getAllCachedResponses().size)
    }

    // Error handling tests
    // BDD: Given request with query parameters, When intercept is called, Then handle correctly by matching path only
    @Test
    fun `intercept handles request with query parameters correctly`() = runTest {
        // Arrange
        val mockKey = MockKey("GET", "/api/search") // Path without query params
        val mockResponse = MockResponse(200, """{"results": []}""")

        val engine = interceptor.getEngine()
        engine.mock(mockKey, mockResponse)

        // Act
        val request = Request.Builder()
            .url(mockWebServer.url("/api/search?q=test&limit=10"))
            .get()
            .build()

        val response = okHttpClient.newCall(request).execute()

        // Assert
        assertEquals(200, response.code)
        assertEquals("""{"results": []}""", response.body?.string())
        assertEquals(0, mockWebServer.requestCount) // Mock was used
    }

    // BDD: Given response with empty body, When intercept is called, Then handle gracefully without errors
    @Test
    fun `intercept handles empty response body gracefully`() {
        // Arrange
        mockWebServer.enqueue(MockWebServerResponse()
            .setResponseCode(204)
            .setBody("")) // Empty body

        val request = Request.Builder()
            .url(mockWebServer.url("/api/empty"))
            .delete()
            .build()

        // Act
        val response = okHttpClient.newCall(request).execute()

        // Assert
        assertEquals(204, response.code)
        assertEquals("", response.body?.string())
        assertEquals(1, mockWebServer.requestCount)
    }
}