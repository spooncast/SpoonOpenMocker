package net.spooncast.openmocker.demo.repo

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.spooncast.openmocker.demo.model.RespWeather
import net.spooncast.openmocker.demo.service.WeatherApiService
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