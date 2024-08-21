package net.spooncast.apimocker.demo.repo

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.spooncast.apimocker.demo.model.RespWeather
import net.spooncast.apimocker.demo.service.WeatherApiService
import javax.inject.Inject

class WeatherRepoImpl @Inject constructor(
    private val service: WeatherApiService,
    private val ioDispatcher: CoroutineDispatcher
): WeatherRepo {

    override suspend fun get(): RespWeather {
        return withContext(ioDispatcher) {
            service.getCurrentWeather()
        }
    }
}