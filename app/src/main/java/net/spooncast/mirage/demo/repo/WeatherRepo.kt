package net.spooncast.mirage.demo.repo

import net.spooncast.mirage.demo.model.RespWeather

interface WeatherRepo {
    suspend fun get(): RespWeather
}