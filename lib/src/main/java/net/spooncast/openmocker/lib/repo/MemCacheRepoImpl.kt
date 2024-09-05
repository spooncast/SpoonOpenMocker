package net.spooncast.openmocker.lib.repo

import androidx.compose.runtime.mutableStateMapOf
import net.spooncast.openmocker.lib.ext.pretty
import net.spooncast.openmocker.lib.model.CachedKey
import net.spooncast.openmocker.lib.model.CachedResponse
import net.spooncast.openmocker.lib.model.CachedValue
import okhttp3.Request
import okhttp3.Response
import kotlin.concurrent.Volatile

class MemCacheRepoImpl private constructor(): CacheRepo {

    private val _cachedMap = mutableStateMapOf<CachedKey, CachedValue>()
    override val cachedMap: Map<CachedKey, CachedValue> get() = _cachedMap

    override fun cache(request: Request, response: Response) {
        val key = CachedKey(request.method, request.url.encodedPath)

        val body = runCatching { response.peekBody(MAX_BYTE_COUNT).string().pretty() }
            .getOrDefault("")
        val cachedResponse = CachedResponse(response.code, body)

        _cachedMap[key] = CachedValue(response = cachedResponse)
    }

    override fun clearCache() {
        _cachedMap.clear()
    }

    override fun getMock(request: Request): CachedResponse? {
        val key = CachedKey(request.method, request.url.encodedPath)
        val value = _cachedMap[key] ?: return null
        return value.mock
    }

    override fun mock(key: CachedKey, response: CachedResponse): Boolean {
        val value = _cachedMap[key] ?: return false
        _cachedMap[key] = value.copy(mock = response)
        return true
    }

    override fun unMock(key: CachedKey): Boolean {
        val value = _cachedMap[key] ?: return false
        _cachedMap[key] = value.copy(mock = null)
        return true
    }

    companion object {
        private const val MAX_BYTE_COUNT = 2048L

        @Volatile
        private var instance: MemCacheRepoImpl? = null

        fun getInstance(): MemCacheRepoImpl {
            return instance ?: synchronized(this) {
                instance ?: MemCacheRepoImpl().also { instance = it }
            }
        }
    }
}