package net.spooncast.mirage.lib.repo

import net.spooncast.mirage.lib.model.MockerKey
import net.spooncast.mirage.lib.model.MockerResponse
import net.spooncast.mirage.lib.model.MockerValue
import okhttp3.Request
import okhttp3.Response

interface MockerRepo {
    val cachedMap: Map<MockerKey, MockerValue>

    fun cache(request: Request, response: Response)
    fun clearCache()

    fun getMock(request: Request): MockerResponse?

    fun mock(key: MockerKey, response: MockerResponse): Boolean
    fun unMock(key: MockerKey): Boolean
}