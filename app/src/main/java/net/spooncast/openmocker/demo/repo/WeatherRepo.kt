package net.spooncast.openmocker.demo.repo

import net.spooncast.openmocker.demo.model.RespWeather

interface WeatherRepo {
    suspend fun get(): RespWeather
}