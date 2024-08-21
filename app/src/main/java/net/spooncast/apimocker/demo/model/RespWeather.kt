package net.spooncast.apimocker.demo.model

import com.google.gson.annotations.SerializedName

data class RespWeather(
    val coord: RespWeatherCoord,
    val weather: List<RespWeatherSum>,
    val base: String,
    val main: RespWeatherMain,
    val visibility: Int,
    val wind: RespWeatherWind,
    val rain: RespWeatherRain,
    val clouds: RespWeatherClouds,
    val dt: Int,
    val sys: RespWeatherSys,
    val timezone: Int,
    val id: Int,
    val name: String,
    val cod: Int
)

data class RespWeatherCoord(
    val lon: Double,
    val lat: Double
)

data class RespWeatherSum(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

data class RespWeatherMain(
    val temp: Double,
    @SerializedName("feels_like")
    val feelsLike: Double,
    @SerializedName("temp_min")
    val tempMin: Double,
    @SerializedName("temp_max")
    val tempMax: Double,
    val pressure: Int,
    val humidity: Int,
    @SerializedName("sea_level")
    val seaLevel: Int,
    @SerializedName("grnd_level")
    val grndLevel: Int
)

data class RespWeatherWind(
    val speed: Double,
    val deg: Int,
    val gust: Double
)

data class RespWeatherRain(
    @SerializedName("1h")
    val oneHour: Double
)

data class RespWeatherClouds(
    val all: Int
)

data class RespWeatherSys(
    val type: Int,
    val id: Int,
    val country: String,
    val sunrise: Int,
    val sunset: Int
)