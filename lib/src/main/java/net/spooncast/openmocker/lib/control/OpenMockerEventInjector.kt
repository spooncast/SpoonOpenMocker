package net.spooncast.openmocker.lib.control

/**
 * HTTP 범위 밖의 이벤트(예: WebSocket/WALA)를 앱이 OpenMocker 제어 통로에 연결하기 위한 범용 추상화.
 *
 * 앱이 이 인터페이스를 구현해 [net.spooncast.openmocker.lib.OpenMocker.control] 의
 * `registerInjector` 로 등록하면, 제어 서버가 `POST /inject/{id}` 로 받은 raw payload 를
 * 파싱 없이 [inject] 로 그대로 전달한다.
 *
 * REST 쪽 [net.spooncast.openmocker.lib.client.okhttp.OpenMockerInterceptor] 와 짝을 이루는,
 * "앱이 전송계층마다 직접 끼워넣는 통합 객체"다 — REST 는 가로채고(intercept), 이벤트는 주입한다(inject).
 *
 * 버퍼링(수신 이력 보관)을 직접 만들고 싶지 않으면 [BufferedEventInjector] 를 상속하면 된다.
 */
interface OpenMockerEventInjector {
    /** 레지스트리 키. `POST /inject/{id}` 의 `{id}` 와 매칭된다. URL 경로 세그먼트로 쓰이므로 `[A-Za-z0-9._-]` 만 허용한다. */
    val id: String

    /** 사람이 읽는 표시 이름(플러그인 UI 용). */
    val name: String

    /**
     * raw payload 를 앱에 주입한다. 제어 서버는 본문을 해석하지 않고 통째로 전달하며,
     * 해석은 전적으로 injector 구현(앱)의 책임이다.
     */
    fun inject(payload: String)

    /**
     * injector 가 실제로 수신/기록한 프레임 목록(최신 일부). 플러그인 UI 가 `GET /inject/{id}/recorded` 로
     * 폴링해 "수신 메시지" 리스트로 노출하고, 항목을 골라 수정·재주입하는 데 쓴다.
     *
     * 보관(버퍼링)은 전적으로 injector 구현(앱)의 책임이다 — lib 는 통로일 뿐 수신 이력을 보관하지 않는다.
     * 기록하지 않는 injector 는 기본 구현(빈 목록)을 그대로 쓰면 된다(opt-in). 버퍼링이 필요하면
     * [BufferedEventInjector] 를 상속해 자동으로 처리할 수 있다.
     */
    fun recorded(): List<RecordedMessage> = emptyList()

    /**
     * injector 의 수신 이력 버퍼를 비운다. 플러그인 UI 가 `DELETE /inject/{id}/recorded` 로 호출해
     * REST 의 "전체 Clear" 와 동일하게 수신 목록을 초기화하는 데 쓴다.
     *
     * [recorded] 와 마찬가지로 보관은 injector 구현(앱)의 책임이라, 비우는 동작도 구현이 맡는다.
     * 이력을 기록하지 않는 injector 는 기본 구현(no-op)을 그대로 쓰면 된다(opt-in).
     */
    fun clearRecorded() = Unit
}

/**
 * injector 가 수신/기록한 프레임 한 건. [OpenMockerEventInjector.recorded] 가 반환한다.
 *
 * [sequence] 는 injector 내 단조 증가 일련번호로, 표시 정렬·항목 식별(안정 키)에 쓴다.
 */
data class RecordedMessage(
    val sequence: Long,
    val payload: String,
)
