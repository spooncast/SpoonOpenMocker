package net.spooncast.openmocker.plugin.settings

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 실제 IntelliJ Platform Application 컨테이너를 부팅해, 순수 단위 테스트가 닿지 못하는 두 지점을
 * 런타임에서 직접 실행시켜 관찰한다:
 *
 *  1. [MockerSettings] 가 `@Service(APP)` 로 컨테이너에 등록돼 [MockerSettings.getInstance] 로
 *     싱글턴이 해석되고, 그 인스턴스가 상태를 round-trip 으로 보존하는지.
 *  2. [MockerConfigurable] 의 `createComponent()` 가 Kotlin UI DSL 패널을 예외 없이 빌드하는지
 *     (= `applicationConfigurable` 가 Settings 화면을 열었을 때 실제로 일어나는 일).
 *
 * 검증 용도의 통합 테스트다 — GUI 다이얼로그를 헤드리스로 클릭할 수 없어, 동일 코드 경로를 플랫폼
 * 컨테이너 위에서 실행해 관찰을 대신한다.
 */
class MockerSettingsPlatformTest : BasePlatformTestCase() {

    fun testServiceResolvesAsSingletonAndRoundTrips() {
        val settings = MockerSettings.getInstance()
        assertNotNull("MockerSettings 가 @Service 로 해석되어야 한다", settings)
        assertSame("getInstance 는 app-level 싱글턴이어야 한다", settings, MockerSettings.getInstance())

        val original = settings.state.copy()
        try {
            settings.loadState(
                MockerSettings.State(
                    port = 9001,
                    lastDeviceSerial = "platform-dev",
                    pollIntervalMs = 750,
                    autoForward = false,
                )
            )

            val reread = MockerSettings.getInstance().state
            assertEquals(9001, reread.port)
            assertEquals("platform-dev", reread.lastDeviceSerial)
            assertEquals(750, reread.pollIntervalMs)
            assertFalse(reread.autoForward)
        } finally {
            settings.loadState(original) // 다른 테스트로 상태가 새지 않도록 복원
        }
    }

    fun testConfigurableBuildsPanelAndBindsToState() {
        val settings = MockerSettings.getInstance()
        val original = settings.state.copy()
        val configurable = MockerConfigurable()
        try {
            settings.loadState(original.copy(port = 8123, pollIntervalMs = 2000, autoForward = true))

            // createComponent() 가 createPanel() 을 호출 — DSL API 오용이면 여기서 예외.
            val component = configurable.createComponent()
            assertNotNull("Settings 패널이 빌드되어야 한다", component)

            // 패널을 state 로 채운 뒤(reset) 변경이 없으면 isModified=false 여야 바인딩이 성립한 것.
            configurable.reset()
            assertFalse("reset 직후엔 수정 없음이어야 한다", configurable.isModified)
        } finally {
            configurable.disposeUIResources()
            settings.loadState(original)
        }
    }
}
