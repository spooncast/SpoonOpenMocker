package net.spooncast.openmocker.demo.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RespWeather(
    val coord: RespWeatherCoord,
    val weather: List<RespWeatherSum>,
    val base: String,
    val main: RespWeatherMain,
    val visibility: Int,
    val wind: RespWeatherWind? = null,
    val rain: RespWeatherRain? = null,
    val clouds: RespWeatherClouds? = null,
    val dt: Int,
    val sys: RespWeatherSys? = null,
    val timezone: Int,
    val id: Int,
    val name: String,
    val cod: Int
)

@Serializable
data class RespWeatherCoord(
    val lon: Double,
    val lat: Double
)

@Serializable
data class RespWeatherSum(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

@Serializable
data class RespWeatherMain(
    val temp: Double,
    @SerializedName("feels_like")
    @SerialName("feels_like")
    val feelsLike: Double,
    @SerializedName("temp_min")
    @SerialName("temp_min")
    val tempMin: Double,
    @SerializedName("temp_max")
    @SerialName("temp_max")
    val tempMax: Double,
    val pressure: Int,
    val humidity: Int,
    @SerializedName("sea_level")
    @SerialName("sea_level")
    val seaLevel: Int,
    @SerializedName("grnd_level")
    @SerialName("grnd_level")
    val grndLevel: Int
)

@Serializable
data class RespWeatherWind(
    val speed: Double,
    val deg: Int,
    val gust: Double
)

@Serializable
data class RespWeatherRain(
    @SerializedName("1h")
    @SerialName("1h")
    val oneHour: Double
)

@Serializable
data class RespWeatherClouds(
    val all: Int
)

@Serializable
data class RespWeatherSys(
    val type: Int? = null,
    val id: Int? = null,
    val country: String,
    val sunrise: Int,
    val sunset: Int
)