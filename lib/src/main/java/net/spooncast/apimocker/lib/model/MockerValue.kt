package net.spooncast.apimocker.lib.model

data class MockerValue(
    val response: MockerResponse,
    val mocked: MockerResponse? = null
)

data class MockerResponse(
    val code: Int,
    val body: String
)