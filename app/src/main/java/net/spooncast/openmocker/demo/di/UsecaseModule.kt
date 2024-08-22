package net.spooncast.openmocker.demo.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.spooncast.openmocker.demo.usecase.GetWeather
import net.spooncast.openmocker.demo.usecase.GetWeatherImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UsecaseModule {

    @Binds
    @Singleton
    abstract fun bindGetWeather(impl: GetWeatherImpl): GetWeather
}