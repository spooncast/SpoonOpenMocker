package net.spooncast.openmocker.demo.repo

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 실제 서버 없이 동작하는 [ChatSocketClient] 데모 구현.
 *
 * 운영이라면 이 자리에서 OkHttp/Ktor `WebSocket` 을 열어 수신 프레임을 [incoming] 으로 forward 한다.
 * 데모에서는 OpenMocker 의 injector 가 [emit] 으로 payload 를 밀어넣어, 플러그인 Inject 가 곧
 * "수신 메시지"가 되도록 한다 — seam([ChatSocketClient])은 운영과 동일하다.
 *
 * `@Singleton` 이라 injector(주입 측)와 ViewModel(구독 측)이 같은 인스턴스를 공유한다.
 *
 * 수신 이력 버퍼(플러그인 폴링용)는 [net.spooncast.openmocker.lib.control.BufferedEventInjector]
 * (= [net.spooncast.openmocker.demo.DemoEventInjector] 의 베이스)가 보관하므로, 이 클라이언트는
 * [incoming] 으로 흘려보내는 일만 한다.
 */
@Singleton
class DemoChatSocketClient @Inject constructor() : ChatSocketClient {

    private val _incoming = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 64,
    )
    override val incoming: SharedFlow<String> = _incoming.asSharedFlow()

    private val _connected = MutableStateFlow(false)
    override val connected: StateFlow<Boolean> = _connected.asStateFlow()

    override fun connect() {
        _connected.value = true
    }

    override fun disconnect() {
        _connected.value = false
    }

    /**
     * 수신 메시지 한 건을 [incoming] 으로 흘려보낸다(운영의 `onMessage` 에 해당).
     * 백그라운드 스레드(제어 서버)에서도 호출되므로 non-suspend `tryEmit` 을 쓴다.
     */
    fun emit(payload: String) {
        _incoming.tryEmit(payload)
    }
}
