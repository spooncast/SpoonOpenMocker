package net.spooncast.openmocker.demo.repo

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 실제 서버 없이 동작하는 [ChatSocketClient] 데모 구현.
 *
 * 운영이라면 이 자리에서 OkHttp/Ktor `WebSocket` 을 열어 수신 프레임을 [incoming] 으로 forward 한다.
 * 데모에서는 OpenMocker 의 sink 가 [emit] 으로 payload 를 밀어넣어, 플러그인 Inject 가 곧
 * "수신 메시지"가 되도록 한다 — seam([ChatSocketClient])은 운영과 동일하다.
 *
 * `@Singleton` 이라 sink(주입 측)와 ViewModel(구독 측)이 같은 인스턴스를 공유한다.
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

    // 수신 프레임 히스토리(최근 [RECENT_CAPACITY]건). _incoming(replay=0)은 과거를 되읽을 수 없어,
    // 플러그인의 "수신 메시지" 폴링(GET /inject/demo/received)이 읽어갈 별도 버퍼를 둔다.
    // emit 은 제어 서버 백그라운드 스레드에서, recentReceived 는 GET 핸들러 스레드에서 호출되므로
    // 두 접근 모두 lock 으로 보호한다.
    private val recentLock = Any()
    private val recent = ArrayDeque<ReceivedFrame>()
    private val seqCounter = AtomicLong(0L)

    override fun connect() {
        _connected.value = true
    }

    override fun disconnect() {
        _connected.value = false
    }

    /**
     * 수신 메시지 한 건을 [incoming] 으로 흘려보낸다(운영의 `onMessage` 에 해당).
     * 백그라운드 스레드(제어 서버)에서도 호출되므로 non-suspend `tryEmit` 을 쓴다.
     *
     * 같은 자리에서 수신 히스토리 버퍼에도 적재한다 — [incoming] 동작은 그대로 두고 버퍼만 추가한다.
     */
    fun emit(payload: String) {
        _incoming.tryEmit(payload)
        val frame = ReceivedFrame(seq = seqCounter.incrementAndGet(), payload = payload)
        synchronized(recentLock) {
            recent.addLast(frame)
            while (recent.size > RECENT_CAPACITY) recent.removeFirst()
        }
    }

    /** 최근 수신 프레임을 최신순(newest-first)으로 반환한다. */
    fun recentReceived(): List<ReceivedFrame> = synchronized(recentLock) {
        recent.toList().asReversed()
    }

    companion object {
        private const val RECENT_CAPACITY = 50
    }
}

/** 수신한 프레임 한 건(일련번호 + 원문). 플러그인 "수신 메시지" 노출용. */
data class ReceivedFrame(
    val seq: Long,
    val payload: String,
)
