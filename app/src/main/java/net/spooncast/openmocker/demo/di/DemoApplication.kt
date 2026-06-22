package net.spooncast.openmocker.demo.di

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import net.spooncast.openmocker.demo.BuildConfig
import net.spooncast.openmocker.demo.DemoEventSink
import net.spooncast.openmocker.lib.OpenMocker

@HiltAndroidApp
class DemoApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        // debug 빌드에서만 임베디드 제어 서버를 띄우고 데모 sink 를 등록한다.
        // 이 레포만으로 M0 제어 서버를 curl E2E 검증할 수 있게 하는 배선이다.
        if (BuildConfig.DEBUG) {
            OpenMocker.startControlServer()
            OpenMocker.registerSink(DemoEventSink())
        }
    }
}
