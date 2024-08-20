package net.spooncast.apimocker.lib.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.spooncast.apimocker.lib.repo.MockerRepo
import net.spooncast.apimocker.lib.repo.MockerRepoImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepoModule {
    @Singleton
    @Binds
    abstract fun bindMockerRepo(impl: MockerRepoImpl): MockerRepo
}