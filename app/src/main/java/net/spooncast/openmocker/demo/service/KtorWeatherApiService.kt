package net.spooncast.openmocker.demo.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import net.spooncast.openmocker.demo.model.RespWeather
import javax.inject.Inject

class KtorWeatherApiService @Inject constructor(
    private val client: HttpClient
) {
    suspend fun getCurrentWeather(): RespWeather {
        return client.get("https://api.openweathermap.org/data/2.5/weather") {
            parameter("lat", "44.34")
            parameter("lon", "10.99")
            parameter("appid", "73c620f0db6d470239900fc66d7e67a4")
        }.body()
    }
}