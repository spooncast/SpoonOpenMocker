package net.spooncast.openmocker.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Adapter that wraps legacy CacheRepo to implement the new MockRepository interface.
 *
 * This adapter provides backward compatibility while migrating to the new Core module architecture.
 * It bridges the gap between the existing Android-specific CacheRepo implementation and the
 * platform-independent MockRepository interface.
 *
 * @param cacheRepo The legacy cache repository implementation to wrap
 */
class CacheRepoAdapter(
    private val cacheRepo: Any // Using Any to avoid direct dependency on lib module
) : MockRepository {

    override suspend fun getMock(key: MockKey): MockResponse? = withContext(Dispatchers.IO) {
        try {
            // Use reflection to call legacy CacheRepo.getMock method
            val method = cacheRepo.javaClass.getMethod("getMock", String::class.java, String::class.java)
            val cachedResponse = method.invoke(cacheRepo, key.method, key.path)

            cachedResponse?.let { response ->
                // Convert legacy CachedResponse to new MockResponse using reflection
                val codeField = response.javaClass.getDeclaredField("code")
                val bodyField = response.javaClass.getDeclaredField("body")
                val durationField = response.javaClass.getDeclaredField("duration")

                codeField.isAccessible = true
                bodyField.isAccessible = true
                durationField.isAccessible = true

                val code = codeField.get(response) as Int
                val body = bodyField.get(response) as String
                val duration = durationField.get(response) as Long

                MockResponse(code = code, body = body, delay = duration)
            }
        } catch (e: Exception) {
            // Log error in production code
            null
        }
    }

    override suspend fun saveMock(key: MockKey, response: MockResponse): Unit = withContext(Dispatchers.IO) {
        try {
            // Create legacy CachedKey using reflection
            val cachedKeyClass = Class.forName("net.spooncast.openmocker.lib.model.CachedKey")
            val cachedKeyConstructor = cachedKeyClass.getDeclaredConstructor(String::class.java, String::class.java)
            val cachedKey = cachedKeyConstructor.newInstance(key.method, key.path)

            // Create legacy CachedResponse using reflection
            val cachedResponseClass = Class.forName("net.spooncast.openmocker.lib.model.CachedResponse")
            val cachedResponseConstructor = cachedResponseClass.getDeclaredConstructor(
                Int::class.java,
                String::class.java,
                Long::class.java
            )
            val cachedResponse = cachedResponseConstructor.newInstance(response.code, response.body, response.delay)

            // Call legacy CacheRepo.mock method
            val mockMethod = cacheRepo.javaClass.getMethod("mock", cachedKeyClass, cachedResponseClass)
            mockMethod.invoke(cacheRepo, cachedKey, cachedResponse)
        } catch (e: Exception) {
            // Log error in production code
        }
    }

    override suspend fun removeMock(key: MockKey): Boolean = withContext(Dispatchers.IO) {
        try {
            // Create legacy CachedKey using reflection
            val cachedKeyClass = Class.forName("net.spooncast.openmocker.lib.model.CachedKey")
            val cachedKeyConstructor = cachedKeyClass.getDeclaredConstructor(String::class.java, String::class.java)
            val cachedKey = cachedKeyConstructor.newInstance(key.method, key.path)

            // Call legacy CacheRepo.unMock method
            val unMockMethod = cacheRepo.javaClass.getMethod("unMock", cachedKeyClass)
            unMockMethod.invoke(cacheRepo, cachedKey) as Boolean
        } catch (e: Exception) {
            // Log error in production code
            false
        }
    }

    override suspend fun getAllMocks(): Map<MockKey, MockResponse> = withContext(Dispatchers.IO) {
        try {
            // Get cachedMap property using reflection
            val cachedMapField = cacheRepo.javaClass.getDeclaredField("cachedMap")
            cachedMapField.isAccessible = true
            val cachedMap = cachedMapField.get(cacheRepo) as Map<*, *>

            val mocks = mutableMapOf<MockKey, MockResponse>()

            for ((key, value) in cachedMap) {
                if (key != null && value != null) {
                    // Extract key information
                    val methodField = key.javaClass.getDeclaredField("method")
                    val pathField = key.javaClass.getDeclaredField("path")
                    methodField.isAccessible = true
                    pathField.isAccessible = true

                    val method = methodField.get(key) as String
                    val path = pathField.get(key) as String

                    // Extract mock information if exists
                    val mockField = value.javaClass.getDeclaredField("mock")
                    mockField.isAccessible = true
                    val mock = mockField.get(value)

                    mock?.let { mockResponse ->
                        val codeField = mockResponse.javaClass.getDeclaredField("code")
                        val bodyField = mockResponse.javaClass.getDeclaredField("body")
                        val durationField = mockResponse.javaClass.getDeclaredField("duration")

                        codeField.isAccessible = true
                        bodyField.isAccessible = true
                        durationField.isAccessible = true

                        val code = codeField.get(mockResponse) as Int
                        val body = bodyField.get(mockResponse) as String
                        val duration = durationField.get(mockResponse) as Long

                        mocks[MockKey(method, path)] = MockResponse(code, body, duration)
                    }
                }
            }

            mocks
        } catch (e: Exception) {
            // Log error in production code
            emptyMap()
        }
    }

    override suspend fun clearAll(): Unit = withContext(Dispatchers.IO) {
        try {
            val clearCacheMethod = cacheRepo.javaClass.getMethod("clearCache")
            clearCacheMethod.invoke(cacheRepo)
        } catch (e: Exception) {
            // Log error in production code
        }
    }

    override suspend fun cacheRealResponse(key: MockKey, response: MockResponse): Unit = withContext(Dispatchers.IO) {
        try {
            val cacheMethod = cacheRepo.javaClass.getMethod(
                "cache",
                String::class.java,
                String::class.java,
                Int::class.java,
                String::class.java
            )
            cacheMethod.invoke(cacheRepo, key.method, key.path, response.code, response.body)
        } catch (e: Exception) {
            // Log error in production code
        }
    }

    override suspend fun getCachedResponse(key: MockKey): MockResponse? = withContext(Dispatchers.IO) {
        try {
            // Get cachedMap property using reflection
            val cachedMapField = cacheRepo.javaClass.getDeclaredField("cachedMap")
            cachedMapField.isAccessible = true
            val cachedMap = cachedMapField.get(cacheRepo) as Map<*, *>

            // Find matching cached response
            for ((cachedKey, cachedValue) in cachedMap) {
                if (cachedKey != null && cachedValue != null) {
                    val methodField = cachedKey.javaClass.getDeclaredField("method")
                    val pathField = cachedKey.javaClass.getDeclaredField("path")
                    methodField.isAccessible = true
                    pathField.isAccessible = true

                    val method = methodField.get(cachedKey) as String
                    val path = pathField.get(cachedKey) as String

                    if (method == key.method && path == key.path) {
                        // Extract original response information
                        val responseField = cachedValue.javaClass.getDeclaredField("response")
                        responseField.isAccessible = true
                        val response = responseField.get(cachedValue)

                        val codeField = response.javaClass.getDeclaredField("code")
                        val bodyField = response.javaClass.getDeclaredField("body")
                        val durationField = response.javaClass.getDeclaredField("duration")

                        codeField.isAccessible = true
                        bodyField.isAccessible = true
                        durationField.isAccessible = true

                        val code = codeField.get(response) as Int
                        val body = bodyField.get(response) as String
                        val duration = durationField.get(response) as Long

                        return@withContext MockResponse(code, body, duration)
                    }
                }
            }

            null
        } catch (e: Exception) {
            // Log error in production code
            null
        }
    }

    override suspend fun getAllCachedResponses(): Map<MockKey, MockResponse> = withContext(Dispatchers.IO) {
        try {
            // Get cachedMap property using reflection
            val cachedMapField = cacheRepo.javaClass.getDeclaredField("cachedMap")
            cachedMapField.isAccessible = true
            val cachedMap = cachedMapField.get(cacheRepo) as Map<*, *>

            val cached = mutableMapOf<MockKey, MockResponse>()

            for ((key, value) in cachedMap) {
                if (key != null && value != null) {
                    // Extract key information
                    val methodField = key.javaClass.getDeclaredField("method")
                    val pathField = key.javaClass.getDeclaredField("path")
                    methodField.isAccessible = true
                    pathField.isAccessible = true

                    val method = methodField.get(key) as String
                    val path = pathField.get(key) as String

                    // Extract cached response information
                    val responseField = value.javaClass.getDeclaredField("response")
                    responseField.isAccessible = true
                    val response = responseField.get(value)

                    val codeField = response.javaClass.getDeclaredField("code")
                    val bodyField = response.javaClass.getDeclaredField("body")
                    val durationField = response.javaClass.getDeclaredField("duration")

                    codeField.isAccessible = true
                    bodyField.isAccessible = true
                    durationField.isAccessible = true

                    val code = codeField.get(response) as Int
                    val body = bodyField.get(response) as String
                    val duration = durationField.get(response) as Long

                    cached[MockKey(method, path)] = MockResponse(code, body, duration)
                }
            }

            cached
        } catch (e: Exception) {
            // Log error in production code
            emptyMap()
        }
    }
}