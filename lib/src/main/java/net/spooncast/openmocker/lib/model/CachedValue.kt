package net.spooncast.openmocker.lib.model

import kotlinx.serialization.Serializable

@Serializable
internal data class CachedValue(
    val response: CachedResponse,
    val mock: CachedResponse? = null
) {
    val code: Int
        get() = mock?.code ?: response.code

    val body: String
        get() = mock?.body ?: response.body

    val duration: Long
        get() = mock?.duration ?: response.duration

    val isCodeMocked: Boolean
        get() = mock?.code != null && mock.code != response.code

    val isBodyMocked: Boolean
        get() = mock?.body != null && mock.body != response.body

    val isDurationMocked: Boolean
        get() = mock?.duration != null && mock.duration != 0L && mock.duration != response.duration
}

@Serializable
internal data class CachedResponse(
    val code: Int,
    val body: String,
    val duration: Long = 0L
)