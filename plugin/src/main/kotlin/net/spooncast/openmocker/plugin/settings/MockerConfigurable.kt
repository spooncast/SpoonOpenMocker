package net.spooncast.openmocker.plugin.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel

/**
 * Settings/Preferences > Tools > OpenMocker 화면. [MockerSettings] 값을 노출·편집한다.
 *
 * UI 는 플랫폼 번들 Kotlin UI DSL 만 쓴다(외부 의존성 0 원칙). 각 입력은 [MockerSettings.state] 의
 * 필드에 직접 바인딩되므로, DSL 이 reset/isModified/apply 를 자동 처리한다. 값의 유효 범위만
 * [apply] 에서 추가 검증해 잘못된 포트·과도하게 짧은 폴링 주기를 막는다.
 *
 * `lastDeviceSerial` 은 사용자가 직접 입력하는 값이 아니라 기기 드롭다운이 갱신하는 값이라 이 화면엔
 * 노출하지 않는다.
 */
class MockerConfigurable : BoundConfigurable("OpenMocker") {

    private val state get() = MockerSettings.getInstance().state

    override fun createPanel(): DialogPanel = panel {
        row("Control server port:") {
            intTextField(range = MIN_PORT..MAX_PORT)
                .bindIntText(state::port)
                .comment("기기 loopback 에서 OpenMocker 제어 서버가 바인딩된 포트 (기본 ${MockerSettings.DEFAULT_PORT}).")
        }
        row("Poll interval (ms):") {
            intTextField(range = MIN_POLL_INTERVAL_MS..MAX_POLL_INTERVAL_MS)
                .bindIntText(state::pollIntervalMs)
                .comment("REST 기록 테이블을 새로고침하는 주기 (기본 ${MockerSettings.DEFAULT_POLL_INTERVAL_MS}ms).")
        }
        row {
            cell(JBCheckBox("기기 선택 시 자동으로 adb forward 설정"))
                .bindSelected(state::autoForward)
        }
    }

    /** DSL 바인딩을 state 에 반영하기 전에 값의 유효 범위를 검증한다. */
    @Throws(ConfigurationException::class)
    override fun apply() {
        super.apply()
        val current = state
        if (current.port !in MIN_PORT..MAX_PORT) {
            throw ConfigurationException("포트는 $MIN_PORT~$MAX_PORT 범위여야 합니다.")
        }
        if (current.pollIntervalMs < MIN_POLL_INTERVAL_MS) {
            throw ConfigurationException("폴링 주기는 최소 ${MIN_POLL_INTERVAL_MS}ms 이상이어야 합니다.")
        }
    }

    private companion object {
        const val MIN_PORT = 1
        const val MAX_PORT = 65535
        const val MIN_POLL_INTERVAL_MS = 250
        const val MAX_POLL_INTERVAL_MS = 60_000
    }
}
