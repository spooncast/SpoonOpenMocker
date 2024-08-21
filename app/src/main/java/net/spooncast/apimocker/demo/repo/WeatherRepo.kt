package net.spooncast.apimocker.demo.repo

import net.spooncast.apimocker.demo.model.RespWeather

interface WeatherRepo {
    suspend fun get(): RespWeather
}