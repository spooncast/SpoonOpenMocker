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
        val mock = cacheRepo.getMock(request.method, request.url.encodedPath)

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
        cacheRepo.cache(
            method = request.method,
            urlPath = request.url.encodedPath,
            responseCode = response.code,
            responseBody = response.peekBody(Long.MAX_VALUE).string(),
        )

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