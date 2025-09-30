package net.spooncast.openmocker.lib.client.okhttp

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.spooncast.openmocker.lib.data.MockingEngine
import net.spooncast.openmocker.lib.data.adapter.OkHttpAdapter
import net.spooncast.openmocker.lib.data.repo.MemCacheRepoImpl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class OpenMockerInterceptor private constructor(
    private val mockingEngine: MockingEngine<Request, Response>
): Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val mockData = mockingEngine.getMockData(request)

        if (mockData != null) {
            if (mockData.duration > 0) {
                runBlocking { delay(mockData.duration) }
            }

            return mockingEngine.createMockResponse(request, mockData)
        }

        val response = chain.proceed(request)
        mockingEngine.cacheResponse(request, response)

        return response
    }

    class Builder {
        fun build(): OpenMockerInterceptor {
            val cacheRepo = MemCacheRepoImpl.getInstance()
            val adapter = OkHttpAdapter()
            val mockingEngine = MockingEngine(cacheRepo, adapter)
            return OpenMockerInterceptor(mockingEngine)
        }
    }
}