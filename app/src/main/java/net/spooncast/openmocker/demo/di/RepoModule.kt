package net.spooncast.openmocker.demo.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.spooncast.openmocker.demo.repo.KtorWeatherRepoImpl
import net.spooncast.openmocker.demo.repo.OkHttpWeatherRepoImpl
import net.spooncast.openmocker.demo.repo.WeatherRepo
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepoModule {

    @OkHttpWeatherRepo
    @Binds
    @Singleton
    abstract fun bindOkHttpWeatherRepo(impl: OkHttpWeatherRepoImpl): WeatherRepo

    @KtorWeatherRepo
    @Binds
    @Singleton
    abstract fun bindKtorWeatherRepo(impl: KtorWeatherRepoImpl): WeatherRepo
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OkHttpWeatherRepo

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KtorWeatherRepo
