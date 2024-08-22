package net.spooncast.mirage.demo.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.spooncast.mirage.demo.usecase.GetWeather
import net.spooncast.mirage.demo.usecase.GetWeatherImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UsecaseModule {

    @Binds
    @Singleton
    abstract fun bindGetWeather(impl: GetWeatherImpl): GetWeather
}