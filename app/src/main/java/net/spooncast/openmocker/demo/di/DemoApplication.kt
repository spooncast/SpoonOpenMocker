package net.spooncast.openmocker.demo.di

import android.app.Application
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import net.spooncast.openmocker.demo.BuildConfig
import net.spooncast.openmocker.demo.DemoEventInjector
import net.spooncast.openmocker.demo.repo.DemoChatSocketClient
import net.spooncast.openmocker.lib.OpenMocker

@HiltAndroidApp
class DemoApplication: Application() {

    /**
     * Hilt 그래프 밖(Application.onCreate)에서 싱글톤 [DemoChatSocketClient] 를 꺼내기 위한 진입점.
     * injector(주입 측)와 ViewModel(구독 측)이 같은 인스턴스를 공유해야 inject 가 화면에 도달한다.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DemoEntryPoint {
        fun chatSocketClient(): DemoChatSocketClient
    }

    override fun onCreate() {
        super.onCreate()

        // debug 빌드에서만 임베디드 제어 서버를 띄우고, 주입을 실시간 스트림으로 잇는 injector 를 등록한다.
        // 플러그인/curl 의 POST /inject/demo → DemoEventInjector → DemoChatSocketClient → Realtime 화면.
        if (BuildConfig.DEBUG) {
            OpenMocker.control.start()

            val client = EntryPointAccessors
                .fromApplication(this, DemoEntryPoint::class.java)
                .chatSocketClient()
            OpenMocker.control.registerInjector(DemoEventInjector(client))
        }
    }
}
