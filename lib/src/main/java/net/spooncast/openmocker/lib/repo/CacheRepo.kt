package net.spooncast.openmocker.lib.repo

import net.spooncast.openmocker.lib.model.CachedKey
import net.spooncast.openmocker.lib.model.CachedResponse
import net.spooncast.openmocker.lib.model.CachedValue
import okhttp3.Response

internal interface CacheRepo {
    val cachedMap: Map<CachedKey, CachedValue>

    fun cache(
        method: String,
        urlPath: String,
        responseCode: Int,
        responseBody: String
    )
    fun clearCache()
    fun getMock(method: String, urlPath: String): CachedResponse?
    fun mock(key: CachedKey, response: CachedResponse): Boolean
    fun unMock(key: CachedKey): Boolean
}