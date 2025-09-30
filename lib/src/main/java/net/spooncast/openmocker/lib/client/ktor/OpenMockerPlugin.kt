package net.spooncast.openmocker.lib.client.ktor

import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestData
import io.ktor.client.statement.request
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.delay
import kotlinx.coroutines.job

@OptIn(InternalAPI::class)
val OpenMockerPlugin = createClientPlugin("OpenMocker", ::OpenMockerPluginConfig) {
    val config = this@createClientPlugin.pluginConfig
    if (!config.enabled) return@createClientPlugin

    on(Send) { requestBuilder ->
        val requestData = requestBuilder.build()

        val mockData = config.mockingEngine.getMockData(requestData)

        if (mockData != null) {
            if (mockData.duration > 0) {
                delay(mockData.duration)
            }

            return@on config.mockingEngine.createMockResponse(requestData, mockData).call
        }

        val originalCall = proceed(requestBuilder)

        return@on originalCall
    }

    onResponse { response ->
        val request = HttpRequestData(
            url = response.request.url,
            method = response.request.method,
            headers = response.request.headers,
            body = response.request.content,
            executionContext = response.request.coroutineContext.job,
            attributes = response.request.attributes
        )

        config.mockingEngine.cacheResponse(request, response)
    }
}