package net.spooncast.openmocker.lib.model

data class CachedValue(
    val response: CachedResponse,
    val mock: CachedResponse? = null
)

data class CachedResponse(
    val code: Int,
    val body: String
)