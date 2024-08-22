package net.spooncast.mirage.demo.usecase

import net.spooncast.mirage.demo.model.RespWeather

interface GetWeather {
    suspend fun get(): Result<RespWeather>
}