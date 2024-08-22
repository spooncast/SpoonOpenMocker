package net.spooncast.openmocker.demo.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.spooncast.openmocker.demo.repo.WeatherRepo
import net.spooncast.openmocker.demo.repo.WeatherRepoImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepoModule {

    @Binds
    @Singleton
    abstract fun bindWeatherRepo(impl: WeatherRepoImpl): WeatherRepo
}