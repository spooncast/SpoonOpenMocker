package net.spooncast.openmocker.lib.repo

import androidx.compose.runtime.mutableStateMapOf
import net.spooncast.openmocker.lib.model.OpenMockerKey
import net.spooncast.openmocker.lib.model.OpenMockerResponse
import net.spooncast.openmocker.lib.model.OpenMockerValue
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenMockerRepoImpl @Inject constructor(): OpenMockerRepo {

    private val _cachedMap = mutableStateMapOf<OpenMockerKey, OpenMockerValue>()
    override val cachedMap: Map<OpenMockerKey, OpenMockerValue> get() = _cachedMap

    override fun cache(request: Request, response: Response) {
        val key = OpenMockerKey(request.method, request.url.encodedPath)
        val mockerRes = OpenMockerResponse(response.code, response.body.toString())
        _cachedMap[key] = OpenMockerValue(response = mockerRes)
    }

    override fun clearCache() {
        _cachedMap.clear()
    }

    override fun getMock(request: Request): OpenMockerResponse? {
        val key = OpenMockerKey(request.method, request.url.encodedPath)
        val value = _cachedMap[key] ?: return null
        return value.mocked
    }

    override fun mock(key: OpenMockerKey, response: OpenMockerResponse): Boolean {
        val value = _cachedMap[key] ?: return false
        _cachedMap[key] = value.copy(mocked = response)
        return true
    }

    override fun unMock(key: OpenMockerKey): Boolean {
        val value = _cachedMap[key] ?: return false
        _cachedMap[key] = value.copy(mocked = null)
        return true
    }
}