package net.spooncast.openmocker.lib.repo

import androidx.compose.runtime.mutableStateMapOf
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import net.spooncast.openmocker.lib.model.CachedKey
import net.spooncast.openmocker.lib.model.CachedResponse
import net.spooncast.openmocker.lib.model.CachedValue
import kotlin.concurrent.Volatile

internal class MemCacheRepoImpl private constructor(): CacheRepo {

    private val gson by lazy { GsonBuilder().setPrettyPrinting().create() }

    private val _cachedMap = mutableStateMapOf<CachedKey, CachedValue>()
    override val cachedMap: Map<CachedKey, CachedValue> get() = _cachedMap

    override fun cache(
        method: String,
        urlPath: String,
        responseCode: Int,
        responseBody: String
    ) {
        val key = CachedKey(method, urlPath)

        val body = runCatching {
            val jsonElement = JsonParser.parseString(responseBody)
            gson.toJson(jsonElement)
        }.getOrDefault("")
        val cachedResponse = CachedResponse(responseCode, body)

        _cachedMap[key] = CachedValue(response = cachedResponse)
    }

    override fun clearCache() {
        _cachedMap.clear()
    }

    override fun getMock(method: String, urlPath: String): CachedResponse? {
        val key = CachedKey(method, urlPath)
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
        @Volatile
        private var instance: MemCacheRepoImpl? = null

        fun getInstance(): MemCacheRepoImpl {
            return instance ?: synchronized(this) {
                instance ?: MemCacheRepoImpl().also { instance = it }
            }
        }
    }
}