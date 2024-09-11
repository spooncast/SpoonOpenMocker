package net.spooncast.openmocker.lib.repo

import net.spooncast.openmocker.lib.model.CachedKey
import net.spooncast.openmocker.lib.model.CachedResponse
import net.spooncast.openmocker.lib.model.CachedValue
import okhttp3.Request
import okhttp3.Response

internal interface CacheRepo {
    val cachedMap: Map<CachedKey, CachedValue>

    fun cache(request: Request, response: Response)
    fun clearCache()
    fun getMock(request: Request): CachedResponse?
    fun mock(key: CachedKey, response: CachedResponse): Boolean
    fun unMock(key: CachedKey): Boolean
}