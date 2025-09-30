package net.spooncast.openmocker.lib.client.ktor

import io.ktor.client.request.HttpRequestData
import io.ktor.client.statement.HttpResponse
import net.spooncast.openmocker.lib.data.adapter.KtorAdapter
import net.spooncast.openmocker.lib.data.repo.MemCacheRepoImpl
import net.spooncast.openmocker.lib.data.MockingEngine

class OpenMockerPluginConfig {
    var enabled: Boolean = true

    internal val mockingEngine: MockingEngine<HttpRequestData, HttpResponse> by lazy {
        val cacheRepo = MemCacheRepoImpl.getInstance()
        val adapter = KtorAdapter()
        MockingEngine(cacheRepo, adapter)
    }
}