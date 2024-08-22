package net.spooncast.openmocker.lib.model

data class OpenMockerValue(
    val response: OpenMockerResponse,
    val mocked: OpenMockerResponse? = null
)

data class OpenMockerResponse(
    val code: Int,
    val body: String
)