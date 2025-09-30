package net.spooncast.openmocker.lib.model

internal data class HttpReq(
    val method: String,
    val path: String,
    val url: String,
    val headers: Map<String, List<String>> = emptyMap()
)