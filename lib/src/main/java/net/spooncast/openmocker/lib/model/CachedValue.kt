package net.spooncast.openmocker.lib.model

import kotlinx.serialization.Serializable

@Serializable
data class CachedValue(
    val response: CachedResponse,
    val mock: CachedResponse? = null
)

@Serializable
data class CachedResponse(
    val code: Int,
    val body: String,
    val duration: Long = 0L
)