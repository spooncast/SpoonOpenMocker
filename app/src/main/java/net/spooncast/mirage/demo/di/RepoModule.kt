package net.spooncast.mirage.demo.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.spooncast.mirage.demo.repo.WeatherRepo
import net.spooncast.mirage.demo.repo.WeatherRepoImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepoModule {

    @Binds
    @Singleton
    abstract fun bindWeatherRepo(impl: WeatherRepoImpl): WeatherRepo
}