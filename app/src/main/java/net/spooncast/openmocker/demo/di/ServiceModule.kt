package net.spooncast.openmocker.demo.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import net.spooncast.openmocker.demo.service.KtorWeatherApiService
import net.spooncast.openmocker.demo.service.OkHttpWeatherApiService
import net.spooncast.openmocker.lib.OpenMocker
import net.spooncast.openmocker.lib.client.ktor.OpenMockerPlugin
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideOkHttpWeatherApiService(): OkHttpWeatherApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(OpenMocker.getInterceptor())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        return retrofit.create(OkHttpWeatherApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideKtorWeatherApiService(client: HttpClient): KtorWeatherApiService {
        return KtorWeatherApiService(client)
    }

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(Android) {
            install(Logging) {
                logger = Logger.ANDROID
                level = LogLevel.ALL
            }

            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = false
                    prettyPrint = true
                    coerceInputValues = true
                })
            }

            install(OpenMockerPlugin) {
                enabled = true
            }
        }
    }
}