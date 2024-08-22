package net.spooncast.openmocker.lib.repo

import net.spooncast.openmocker.lib.model.OpenMockerKey
import net.spooncast.openmocker.lib.model.OpenMockerResponse
import net.spooncast.openmocker.lib.model.OpenMockerValue
import okhttp3.Request
import okhttp3.Response

interface OpenMockerRepo {
    val cachedMap: Map<OpenMockerKey, OpenMockerValue>

    fun cache(request: Request, response: Response)
    fun clearCache()
    fun getMock(request: Request): OpenMockerResponse?
    fun mock(key: OpenMockerKey, response: OpenMockerResponse): Boolean
    fun unMock(key: OpenMockerKey): Boolean
}