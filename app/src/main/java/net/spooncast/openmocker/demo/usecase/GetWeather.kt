package net.spooncast.openmocker.demo.usecase

import net.spooncast.openmocker.demo.model.RespWeather

interface GetWeather {
    suspend fun get(): Result<RespWeather>
}