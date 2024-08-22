package net.spooncast.mirage.lib.interceptor

import net.spooncast.mirage.lib.repo.MockerRepo
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject

class MockerInterceptor @Inject constructor(
    private val mockerRepo: MockerRepo
): Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val mocked = mockerRepo.getMock(request)

        if (mocked != null) {
            return Response.Builder()
                .protocol(Protocol.HTTP_2)
                .request(request)
                .code(mocked.code)
                .message(MOCKER_MESSAGE)
                .body(MOCKER_MESSAGE.toResponseBody(null))
                .build()
        }

        val response = chain.proceed(request)

        // 성공한 요청에 대해서만 caching을 수행한다.
        if (response.isSuccessful) {
            mockerRepo.cache(request, response)
        }

        return response
    }

    companion object {
        const val MOCKER_MESSAGE = "Spoon api mocker enabled"
    }
}