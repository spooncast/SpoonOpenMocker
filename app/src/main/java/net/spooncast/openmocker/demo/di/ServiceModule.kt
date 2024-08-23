package net.spooncast.openmocker.demo.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.spooncast.openmocker.demo.service.WeatherApiService
import net.spooncast.openmocker.lib.OpenMocker
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideWeatherApiService(): WeatherApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(OpenMocker.getInterceptor())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        return retrofit.create(WeatherApiService::class.java)
    }
}