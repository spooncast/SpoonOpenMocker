package net.spooncast.apimocker.lib.repo

import androidx.compose.runtime.mutableStateMapOf
import net.spooncast.apimocker.lib.model.MockerKey
import net.spooncast.apimocker.lib.model.MockerResponse
import net.spooncast.apimocker.lib.model.MockerValue
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockerRepoImpl @Inject constructor(): MockerRepo {

    private val _cachedMap = mutableStateMapOf<MockerKey, MockerValue>()
    override val cachedMap: Map<MockerKey, MockerValue> get() = _cachedMap

    override fun cache(request: Request, response: Response) {
        val key = MockerKey(request.method, request.url.encodedPath)
        val mockerRes = MockerResponse(response.code, response.body.toString())
        _cachedMap[key] = MockerValue(response = mockerRes)
    }

    override fun clearCache() {
        _cachedMap.clear()
    }

    override fun getMock(request: Request): MockerResponse? {
        val key = MockerKey(request.method, request.url.encodedPath)
        val value = _cachedMap[key] ?: return null
        return value.mocked
    }

    override fun mock(key: MockerKey, response: MockerResponse): Boolean {
        val value = _cachedMap[key] ?: return false
        _cachedMap[key] = value.copy(mocked = response)
        return true
    }

    override fun unMock(key: MockerKey): Boolean {
        val value = _cachedMap[key] ?: return false
        _cachedMap[key] = value.copy(mocked = null)
        return true
    }
}