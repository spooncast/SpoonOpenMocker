package net.spooncast.openmocker.lib.data.adapter

import net.spooncast.openmocker.lib.model.CachedResponse
import net.spooncast.openmocker.lib.model.HttpReq
import net.spooncast.openmocker.lib.model.HttpResp

internal interface HttpClientAdapter<TRequest, TResponse> {
    fun extractRequestData(clientRequest: TRequest): HttpReq
    fun extractResponseData(clientResponse: TResponse): HttpResp
    fun createMockResponse(originalRequest: TRequest, mockResponse: CachedResponse): TResponse
}