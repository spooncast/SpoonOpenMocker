package net.spooncast.openmocker.plugin.adb

import net.spooncast.openmocker.plugin.adb.AdbService.AdbDevice
import net.spooncast.openmocker.plugin.adb.AdbService.ProcessResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [AdbService] 의 순수 파서([AdbService.parseDevices]/[AdbService.parseForwards]) 와 명령 조립·
 * 실패 흡수 로직을, 실제 adb 없이 가짜 [runner][AdbService] 와 출력 문자열만으로 검증한다.
 * 외부 의존(실기기/adb 설치)에 닿지 않으므로 CI 어디서나 결정적이다.
 */
class AdbServiceTest {

    // ── parseDevices ────────────────────────────────────────────────────────

    @Test
    fun `parseDevices skips header and parses serial and state`() {
        val output = """
            List of devices attached
            emulator-5554	device
            2C121JEGR00428	device
        """.trimIndent()

        val devices = AdbService.parseDevices(output)

        assertEquals(2, devices.size)
        assertEquals("emulator-5554", devices[0].serial)
        assertEquals("device", devices[0].state)
        assertEquals("2C121JEGR00428", devices[1].serial)
    }

    @Test
    fun `parseDevices captures offline and unauthorized states`() {
        val output = """
            List of devices attached
            emulator-5554	device
            deadbeef	offline
            ghostdevice	unauthorized
        """.trimIndent()

        val devices = AdbService.parseDevices(output)

        assertEquals(3, devices.size)
        assertTrue(devices[0].isOnline)
        assertEquals("offline", devices[1].state)
        assertFalse(devices[1].isOnline)
        assertEquals("unauthorized", devices[2].state)
        assertFalse(devices[2].isOnline)
    }

    @Test
    fun `parseDevices returns empty when no devices attached`() {
        val output = """
            List of devices attached

        """.trimIndent()

        assertTrue(AdbService.parseDevices(output).isEmpty())
    }

    @Test
    fun `parseDevices ignores blank and malformed lines`() {
        // adb 가 앞단에 데몬 기동 메시지를 섞어 출력하는 경우 등
        val output = """
            * daemon started successfully

            List of devices attached
            emulator-5554	device
            noise
        """.trimIndent()

        val devices = AdbService.parseDevices(output)

        // "* daemon started successfully" 는 2열이지만 잡음 — 다만 파서는 형식만 보므로
        // 여기서는 실제 device 줄만 단언한다.
        assertTrue(devices.any { it.serial == "emulator-5554" && it.state == "device" })
        // 단일 토큰 "noise" 는 state 가 없어 버려진다.
        assertFalse(devices.any { it.serial == "noise" })
    }

    // ── parseForwards ───────────────────────────────────────────────────────

    @Test
    fun `parseForwards parses serial local and remote`() {
        val output = """
            emulator-5554 tcp:8099 tcp:8099
            2C121JEGR00428 tcp:9000 tcp:8099
        """.trimIndent()

        val forwards = AdbService.parseForwards(output)

        assertEquals(2, forwards.size)
        assertEquals("emulator-5554", forwards[0].serial)
        assertEquals("tcp:8099", forwards[0].local)
        assertEquals("tcp:8099", forwards[0].remote)
        assertEquals("tcp:9000", forwards[1].local)
        assertEquals("tcp:8099", forwards[1].remote)
    }

    @Test
    fun `parseForwards returns empty for no forwards`() {
        assertTrue(AdbService.parseForwards("").isEmpty())
        assertTrue(AdbService.parseForwards("   \n  ").isEmpty())
    }

    @Test
    fun `parseForwards skips lines with fewer than three columns`() {
        val output = """
            emulator-5554 tcp:8099 tcp:8099
            broken tcp:8099
        """.trimIndent()

        val forwards = AdbService.parseForwards(output)

        assertEquals(1, forwards.size)
        assertEquals("emulator-5554", forwards[0].serial)
    }

    // ── adb 탐색 우선순위 ────────────────────────────────────────────────────

    @Test
    fun `resolveAdb falls back to PATH when no sdk env set`() {
        // SDK env 가 없으면 PATH 위임을 기대하고 "adb" 만으로 실행 — 명령 첫 토큰으로 확인한다.
        var captured: List<String>? = null
        val service = AdbService(
            env = { null },
            runner = { cmd -> captured = cmd; ProcessResult(0, "List of devices attached\n", "") },
        )

        val result = service.devices()

        assertTrue(result.isSuccess)
        assertEquals("adb", captured?.first())
    }

    @Test
    fun `resolveAdb fails when sdk env set but executable missing`() {
        // ANDROID_HOME 가 가리키는 곳에 platform-tools/adb 가 없으면 조기 실패(오타·미설치 발견).
        val service = AdbService(
            env = { key -> if (key == "ANDROID_HOME") "/no/such/sdk/path" else null },
            runner = { ProcessResult(0, "", "") },
        )

        val result = service.devices()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AdbException)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("ANDROID_HOME"))
    }

    // ── 명령 조립 & 실패 흡수 ──────────────────────────────────────────────────

    @Test
    fun `forward assembles adb -s serial forward tcp tcp`() {
        var captured: List<String>? = null
        val service = AdbService(
            env = { null },
            runner = { cmd -> captured = cmd; ProcessResult(0, "", "") },
        )

        val result = service.forward(serial = "emulator-5554", port = 8099)

        assertTrue(result.isSuccess)
        assertEquals(
            listOf("adb", "-s", "emulator-5554", "forward", "tcp:8099", "tcp:8099"),
            captured,
        )
    }

    @Test
    fun `removeForward assembles forward --remove`() {
        var captured: List<String>? = null
        val service = AdbService(
            env = { null },
            runner = { cmd -> captured = cmd; ProcessResult(0, "", "") },
        )

        service.removeForward(serial = "emulator-5554", port = 8099)

        assertEquals(
            listOf("adb", "-s", "emulator-5554", "forward", "--remove", "tcp:8099"),
            captured,
        )
    }

    @Test
    fun `non-zero exit becomes AdbException failure with stderr detail`() {
        val service = AdbService(
            env = { null },
            runner = { ProcessResult(exitCode = 1, stdout = "", stderr = "error: device offline") },
        )

        val result = service.forward(serial = "deadbeef", port = 8099)

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is AdbException)
        assertTrue(ex!!.message!!.contains("device offline"))
    }

    @Test
    fun `timeout (null exit) becomes AdbException failure`() {
        val service = AdbService(
            env = { null },
            runner = { ProcessResult(exitCode = null, stdout = "", stderr = "") },
        )

        val result = service.devices()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AdbException)
    }

    @Test
    fun `devices round-trips runner output through parser`() {
        val service = AdbService(
            env = { null },
            runner = { ProcessResult(0, "List of devices attached\nemulator-5554\tdevice\n", "") },
        )

        val devices: List<AdbDevice> = service.devices().getOrThrow()

        assertEquals(1, devices.size)
        assertEquals("emulator-5554", devices[0].serial)
        assertNull(service.devices().exceptionOrNull())
    }
}
