package net.spooncast.openmocker.plugin.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [MockerSettings] 의 기본값과 직렬화 round-trip 을 플랫폼 없이 검증한다.
 *
 * `@Service` 싱글턴 획득([MockerSettings.getInstance])은 application 컨텍스트를 요구하므로, 영속화
 * 계약(`getState`/`loadState`)만 떼어 인스턴스를 직접 만들어 본다. 이 두 함수가 [State] 4 필드를
 * 손실 없이 보존하는지가 "재시작 후에도 설정 유지"의 핵심이다.
 */
class MockerSettingsTest {

    @Test
    fun `defaults match documented values`() {
        val state = MockerSettings.State()

        assertEquals(MockerSettings.DEFAULT_PORT, state.port)
        assertEquals("", state.lastDeviceSerial)
        assertEquals(MockerSettings.DEFAULT_POLL_INTERVAL_MS, state.pollIntervalMs)
        assertTrue(state.autoForward)
    }

    @Test
    fun `getState returns current state`() {
        val settings = MockerSettings()

        assertEquals(MockerSettings.State(), settings.state)
    }

    @Test
    fun `loadState round-trips all fields`() {
        val source = MockerSettings()
        source.loadState(
            MockerSettings.State(
                port = 9000,
                lastDeviceSerial = "emulator-5554",
                pollIntervalMs = 500,
                autoForward = false,
            )
        )

        // 새 인스턴스에 직전 상태를 주입했을 때 4 필드가 그대로 복원되는지(저장→로드 round-trip).
        val restored = MockerSettings()
        restored.loadState(source.state)

        assertEquals(9000, restored.state.port)
        assertEquals("emulator-5554", restored.state.lastDeviceSerial)
        assertEquals(500, restored.state.pollIntervalMs)
        assertEquals(false, restored.state.autoForward)
    }
}
