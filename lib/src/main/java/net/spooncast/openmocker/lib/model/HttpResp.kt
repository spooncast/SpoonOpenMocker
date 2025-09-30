package net.spooncast.openmocker.lib.model

internal data class HttpResp(
    val code: Int,
    val body: String,
    val headers: Map<String, List<String>> = emptyMap(),
    val isSuccessful: Boolean = code in 200..299
)