package net.spooncast.openmocker.lib.control

import java.util.concurrent.atomic.AtomicLong

/**
 * 수신 이력 버퍼링을 lib 이 대신 처리해 주는 [OpenMockerEventInjector] 추상 베이스(opt-in).
 *
 * 직접 인터페이스를 구현하면 앱이 링버퍼·일련번호 채번·thread-safe 접근을 매번 다시 만들어야 한다.
 * 이 베이스를 상속하면 그 보일러플레이트 없이 실제 전달 로직([deliver])만 구현하면 된다 —
 * [inject] 가 payload 를 버퍼에 기록(일련번호 자동 채번)한 뒤 [deliver] 로 앱에 전달하고,
 * [recorded]/[clearRecorded] 는 베이스가 제공한다.
 *
 * 주입은 제어 서버 스레드에서, [recorded] 조회는 폴링 스레드에서 호출되므로 버퍼 접근을 lock 으로 보호한다.
 *
 * @param capacity 보관할 최신 프레임 최대 개수. 초과분은 오래된 것부터 버린다.
 */
abstract class BufferedEventInjector(
    final override val id: String,
    final override val name: String,
    private val capacity: Int = DEFAULT_CAPACITY,
) : OpenMockerEventInjector {

    private val lock = Any()
    private val buffer = ArrayDeque<RecordedMessage>()
    private val sequenceCounter = AtomicLong(0L)

    /**
     * 주입된 raw payload 를 앱의 실시간 스트림으로 실제 전달한다(예: WebSocket `onMessage` 재현).
     * 해석은 구현의 책임이다. 호출 전에 [inject] 가 이미 버퍼에 기록을 마친 상태다.
     */
    protected abstract fun deliver(payload: String)

    final override fun inject(payload: String) {
        record(payload)
        deliver(payload)
    }

    /**
     * payload 를 수신 이력 버퍼에 기록한다(일련번호 자동 채번). [inject] 가 자동으로 호출하지만,
     * 주입 외 경로로 들어온 실제 inbound 프레임도 이력에 남기고 싶으면 구현이 직접 호출할 수 있다.
     */
    protected fun record(payload: String) {
        val frame = RecordedMessage(sequence = sequenceCounter.incrementAndGet(), payload = payload)
        synchronized(lock) {
            buffer.addLast(frame)
            while (buffer.size > capacity) buffer.removeFirst()
        }
    }

    /** 최근 수신 프레임을 최신순(newest-first)으로 반환한다. */
    final override fun recorded(): List<RecordedMessage> = synchronized(lock) {
        buffer.toList().asReversed()
    }

    /** 수신 이력 버퍼를 비운다. 일련번호 카운터는 유지해 이후 프레임과 구분된다. */
    final override fun clearRecorded() = synchronized(lock) {
        buffer.clear()
    }

    companion object {
        private const val DEFAULT_CAPACITY = 50
    }
}
