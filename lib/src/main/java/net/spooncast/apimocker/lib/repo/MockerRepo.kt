package net.spooncast.apimocker.lib.repo

import net.spooncast.apimocker.lib.model.MockerKey
import net.spooncast.apimocker.lib.model.MockerResponse
import net.spooncast.apimocker.lib.model.MockerValue
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