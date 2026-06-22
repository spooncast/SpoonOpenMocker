package net.spooncast.openmocker.demo

import android.util.Log
import net.spooncast.openmocker.lib.control.OpenMockerEventSink
import net.spooncast.openmocker.lib.control.Preset

/**
 * 데모 앱이 제어 서버의 `POST /inject/demo` 통로를 검증하기 위한 예시 sink.
 *
 * 실제 WebSocket/WALA 연결이 없는 데모이므로, 주입된 payload 를 [Log] 로 출력해
 * 제어 서버 → sink 도달 경로만 증명한다. 해석은 하지 않는다.
 */
class DemoEventSink : OpenMockerEventSink {

    override val id: String = "demo"

    override val name: String = "Demo Sink"

    override fun inject(payload: String) {
        Log.d(TAG, "inject payload: $payload")
    }

    override fun presets(): List<Preset> = listOf(
        Preset(name = "hello", payload = """{"event":"hello","message":"from demo sink"}"""),
        Preset(name = "tick", payload = """{"event":"tick","value":1}"""),
    )

    companion object {
        private const val TAG = "DemoEventSink"
    }
}
