package net.spooncast.apimocker.demo.service

import net.spooncast.apimocker.demo.model.RespWeather
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: String = "44.34",
        @Query("lon") lon: String = "10.99",
        @Query("appid") appId: String = ""
    ): RespWeather
}