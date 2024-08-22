package net.spooncast.openmocker.demo.usecase

import net.spooncast.openmocker.demo.model.RespWeather
import net.spooncast.openmocker.demo.repo.WeatherRepo
import javax.inject.Inject

class GetWeatherImpl @Inject constructor(
    private val weatherRepo: WeatherRepo
): GetWeather {

    override suspend fun get(): Result<RespWeather> {
        return try {
            val result = weatherRepo.get()
            Result.success(result)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}