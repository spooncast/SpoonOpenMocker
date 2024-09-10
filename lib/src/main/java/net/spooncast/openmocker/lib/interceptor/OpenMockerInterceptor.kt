package net.spooncast.openmocker.lib.interceptor

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.spooncast.openmocker.lib.repo.CacheRepo
import net.spooncast.openmocker.lib.repo.MemCacheRepoImpl
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class OpenMockerInterceptor private constructor(
    private val cacheRepo: CacheRepo
): Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val mock = cacheRepo.getMock(request)

        if (mock != null) {
            runBlocking { delay(mock.duration) }
            
            return Response.Builder()
                .protocol(Protocol.HTTP_2)
                .request(request)
                .code(mock.code)
                .message(MOCKER_MESSAGE)
                .body(mock.body.toResponseBody())
                .build()
        }

        val response = chain.proceed(request)

        // 성공한 요청에 대해서만 caching을 수행한다.
        if (response.isSuccessful) {
            cacheRepo.cache(request, response)
        }

        return response
    }

    class Builder {
        fun build(): OpenMockerInterceptor {
            return OpenMockerInterceptor(MemCacheRepoImpl.getInstance())
        }
    }

    companion object {
        const val MOCKER_MESSAGE = "OpenMocker enabled"
    }
}