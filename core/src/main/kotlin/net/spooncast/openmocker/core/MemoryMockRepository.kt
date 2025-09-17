package net.spooncast.openmocker.core

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory implementation of MockRepository.
 *
 * This implementation uses ConcurrentHashMap for thread-safe storage of mocks and cached responses.
 * All operations are protected with appropriate synchronization mechanisms to ensure thread safety
 * in multi-threaded environments.
 *
 * Memory management considerations:
 * - Uses efficient concurrent data structures
 * - Provides methods for clearing data to prevent memory leaks
 * - All data is stored in memory and will be lost when the application terminates
 */
class MemoryMockRepository : MockRepository {

    private val mocks = ConcurrentHashMap<MockKey, MockResponse>()
    private val cachedResponses = ConcurrentHashMap<MockKey, MockResponse>()
    private val mutex = Mutex()

    override suspend fun getMock(key: MockKey): MockResponse? {
        return mocks[key]
    }

    override suspend fun saveMock(key: MockKey, response: MockResponse) {
        mocks[key] = response
    }

    override suspend fun removeMock(key: MockKey): Boolean {
        return mocks.remove(key) != null
    }

    override suspend fun getAllMocks(): Map<MockKey, MockResponse> {
        return mocks.toMap()
    }

    override suspend fun clearAll() {
        mutex.withLock {
            mocks.clear()
            cachedResponses.clear()
        }
    }

    override suspend fun cacheRealResponse(key: MockKey, response: MockResponse) {
        cachedResponses[key] = response
    }

    override suspend fun getCachedResponse(key: MockKey): MockResponse? {
        return cachedResponses[key]
    }

    override suspend fun getAllCachedResponses(): Map<MockKey, MockResponse> {
        return cachedResponses.toMap()
    }

    /**
     * Gets the total number of stored mocks.
     * Useful for monitoring memory usage and debugging.
     */
    fun getMocksCount(): Int = mocks.size

    /**
     * Gets the total number of cached responses.
     * Useful for monitoring memory usage and debugging.
     */
    fun getCachedResponsesCount(): Int = cachedResponses.size

    /**
     * Clears only the mocks, keeping cached responses intact.
     */
    suspend fun clearMocks() {
        mocks.clear()
    }

    /**
     * Clears only the cached responses, keeping mocks intact.
     */
    suspend fun clearCache() {
        cachedResponses.clear()
    }
}