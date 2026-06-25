package net.spooncast.openmocker.plugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * 플러그인 제어에 필요한 사용자 설정을 IDE 재시작 후에도 유지하는 application-level 영속화 서비스.
 *
 * [ControlClient][net.spooncast.openmocker.plugin.net.ControlClient] ·
 * [AdbService][net.spooncast.openmocker.plugin.adb.AdbService] 와 같은 원칙(외부 런타임 의존성 0)을
 * 따른다. 영속화는 IntelliJ Platform 표준 [PersistentStateComponent] 로만 처리하며, 직렬화 대상은
 * 공개 var 만 가진 단순 [State] 데이터 클래스다. `@Service` 로 등록되므로 plugin.xml 에 별도 선언이
 * 필요 없다.
 *
 * 포트·폴링 주기·자동 forward 는 프로젝트와 무관한 전역 설정이라 app-level(`openmocker.xml`)에 둔다.
 * 마지막 선택 기기 serial 도 같은 저장소에 보관해, 다음 세션에서 동일 기기를 기본 선택할 수 있게 한다
 * (드롭다운 연동·forward 트리거는 후속 task 의 책임이고, 여기서는 값만 보관한다).
 */
@Service(Service.Level.APP)
@State(name = "OpenMockerSettings", storages = [Storage("openmocker.xml")])
class MockerSettings : PersistentStateComponent<MockerSettings.State> {

    /**
     * 직렬화되는 설정 값. XML 직렬화기가 채울 수 있도록 모든 필드를 기본값을 가진 공개 var 로 둔다.
     *
     * - [port]: 기기 loopback 의 제어 서버 포트(기본 [DEFAULT_PORT]). adb forward 의 대상과 일치.
     * - [lastDeviceSerial]: 마지막으로 선택한 adb 기기 serial. 미선택 상태는 빈 문자열.
     * - [pollIntervalMs]: REST 기록 테이블 폴링 주기(ms, 기본 [DEFAULT_POLL_INTERVAL_MS]).
     * - [autoForward]: 기기 선택 시 자동으로 adb forward 를 설정할지 여부.
     * - [sdkPath]: Android SDK 루트 경로 override. 빈 문자열이면 미지정(env·표준 위치 자동 탐색).
     *   지정 시 [AdbService][net.spooncast.openmocker.plugin.adb.AdbService] 가 그 아래
     *   `platform-tools/adb` 를 최우선으로 찾는다(자동 탐색이 실패하는 비표준 설치 탈출구).
     */
    data class State(
        var port: Int = DEFAULT_PORT,
        var lastDeviceSerial: String = "",
        var pollIntervalMs: Int = DEFAULT_POLL_INTERVAL_MS,
        var autoForward: Boolean = true,
        var sdkPath: String = "",
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, this.state)
    }

    companion object {
        /** 제어 서버 기본 포트. lib 측 `ControlServer.DEFAULT_PORT` 와 일치시킨다. */
        const val DEFAULT_PORT = 8099

        /** REST 기록 폴링 기본 주기(ms). */
        const val DEFAULT_POLL_INTERVAL_MS = 1500

        /** application-level 싱글턴 인스턴스를 돌려준다. */
        fun getInstance(): MockerSettings =
            ApplicationManager.getApplication().getService(MockerSettings::class.java)
    }
}
