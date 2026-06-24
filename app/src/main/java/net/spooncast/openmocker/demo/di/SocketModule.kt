package net.spooncast.openmocker.demo.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.spooncast.openmocker.demo.repo.ChatSocketClient
import net.spooncast.openmocker.demo.repo.DemoChatSocketClient
import javax.inject.Singleton

/**
 * [ChatSocketClient] seam 을 데모 구현([DemoChatSocketClient])에 바인딩한다.
 *
 * 구체 타입([DemoChatSocketClient])은 `@Inject constructor` + `@Singleton` 이라
 * `DemoApplication` 이 `EntryPointAccessors` 로 같은 싱글톤을 꺼내 injector 에 넘길 수 있다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SocketModule {

    @Binds
    @Singleton
    abstract fun bindChatSocketClient(impl: DemoChatSocketClient): ChatSocketClient
}
