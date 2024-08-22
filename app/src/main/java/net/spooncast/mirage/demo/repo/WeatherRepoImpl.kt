package net.spooncast.mirage.demo.repo

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.spooncast.mirage.demo.model.RespWeather
import net.spooncast.mirage.demo.service.WeatherApiService
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