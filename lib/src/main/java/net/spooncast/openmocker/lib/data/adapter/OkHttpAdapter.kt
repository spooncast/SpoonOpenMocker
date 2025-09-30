package net.spooncast.openmocker.lib.data.adapter

import net.spooncast.openmocker.lib.model.CachedResponse
import net.spooncast.openmocker.lib.model.HttpReq
import net.spooncast.openmocker.lib.model.HttpResp
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

internal class OkHttpAdapter : HttpClientAdapter<Request, Response> {
    override fun extractRequestData(clientRequest: Request): HttpReq {
        return HttpReq(
            method = clientRequest.method,
            path = clientRequest.url.encodedPath,
            url = clientRequest.url.toString(),
            headers = clientRequest.headers.toMultimap()
        )
    }

    override fun extractResponseData(clientResponse: Response): HttpResp {
        val body = try {
            clientResponse.peekBody(Long.MAX_VALUE).string()
        } catch (e: Exception) {
            ""
        }

        return HttpResp(
            code = clientResponse.code,
            body = body,
            headers = clientResponse.headers.toMultimap(),
            isSuccessful = clientResponse.isSuccessful
        )
    }

    override fun createMockResponse(originalRequest: Request, mockResponse: CachedResponse): Response {
        return Response.Builder()
            .protocol(Protocol.HTTP_2)
            .request(originalRequest)
            .code(mockResponse.code)
            .message(MOCKER_MESSAGE)
            .body(mockResponse.body.toResponseBody())
            .build()
    }

    companion object {
        const val MOCKER_MESSAGE = "OpenMocker enabled"
    }
}