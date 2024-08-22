package net.spooncast.openmocker.lib.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.spooncast.openmocker.lib.repo.OpenMockerRepo
import net.spooncast.openmocker.lib.repo.OpenMockerRepoImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepoModule {
    @Singleton
    @Binds
    abstract fun bindOpenMockerRepo(impl: OpenMockerRepoImpl): OpenMockerRepo
}