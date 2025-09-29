package net.spooncast.openmocker.demo.repo

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.spooncast.openmocker.demo.model.RespWeather
import net.spooncast.openmocker.demo.service.KtorWeatherApiService
import javax.inject.Inject

class KtorWeatherRepoImpl @Inject constructor(
    private val ktorWeatherApiService: KtorWeatherApiService,
    private val ioDispatcher: CoroutineDispatcher
): WeatherRepo {

    override suspend fun get(): RespWeather {
        return withContext(ioDispatcher) {
            ktorWeatherApiService.getCurrentWeather()
        }
    }
}