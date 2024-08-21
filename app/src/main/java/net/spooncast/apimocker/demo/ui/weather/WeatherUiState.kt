package net.spooncast.apimocker.demo.ui.weather

import net.spooncast.apimocker.demo.model.RespWeather

sealed interface WeatherUiState {

    data class Success(
        val weather: RespWeather
    ): WeatherUiState

    data object Loading: WeatherUiState

    data class Error(
        val throwable: Throwable
    ): WeatherUiState
}