package net.spooncast.apimocker.demo.usecase

import net.spooncast.apimocker.demo.model.RespWeather

interface GetWeather {
    suspend fun get(): Result<RespWeather>
}