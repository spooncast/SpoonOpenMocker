package net.spooncast.openmocker.demo.repo

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.spooncast.openmocker.demo.model.RespWeather
import net.spooncast.openmocker.demo.service.OkHttpWeatherApiService
import javax.inject.Inject

class OkHttpWeatherRepoImpl @Inject constructor(
    private val okHttpWeatherApiService: OkHttpWeatherApiService,
    private val ioDispatcher: CoroutineDispatcher
): WeatherRepo {

    override suspend fun get(): RespWeather {
        return withContext(ioDispatcher) {
            okHttpWeatherApiService.getCurrentWeather()
        }
    }
}