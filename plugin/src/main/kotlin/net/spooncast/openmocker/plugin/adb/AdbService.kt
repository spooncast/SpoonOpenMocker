package net.spooncast.openmocker.plugin.adb

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * adb 실행파일을 탐색하고 기기 목록 조회·포트 포워딩을 수행하는 통신 레이어.
 *
 * [ControlClient][net.spooncast.openmocker.plugin.net.ControlClient] 와 같은 원칙 — 외부 런타임
 * 의존성 0, 생성자 주입, 모든 호출을 [Result] 의 성공/실패로 흡수(호출부 try/catch 불필요) — 를
 * 따른다. 실패는 사유를 담은 [AdbException] 으로 구조화한다.
 *
 * 프로세스 실행(부수효과)과 출력 파싱(순수)을 분리한다. 파싱은 [parseDevices]/[parseForwards] 의
 * 순수 함수로 두어 실제 adb 없이 출력 문자열만으로 단위 테스트할 수 있고, 프로세스 실행은 [runner]
 * seam 으로 주입해 기본값은 ProcessBuilder, 테스트는 가짜 러너를 끼울 수 있다. adb 탐색의 env 조회도
 * [env] seam 으로 주입한다.
 *
 * 운영 시 [forward] 는 기기 loopback 의 제어 서버 포트(기본 8099)를 데스크톱 loopback 으로 노출해
 * [ControlClient][net.spooncast.openmocker.plugin.net.ControlClient] 가 접속할 수 있게 한다.
 */
class AdbService(
    private val env: (String) -> String? = System::getenv,
    private val timeout: Long = DEFAULT_TIMEOUT_SECONDS,
    private val runner: (List<String>) -> ProcessResult = { runProcess(it, timeout) },
) {
    /** 프로세스 1회 실행 결과. [exitCode] 가 null 이면 타임아웃으로 강제 종료된 것이다. */
    data class ProcessResult(
        val exitCode: Int?,
        val stdout: String,
        val stderr: String,
    )

    /** `adb devices` 한 항목. [state] 는 device/offline/unauthorized 등 adb 가 보고한 원문. */
    data class AdbDevice(
        val serial: String,
        val state: String,
    ) {
        /** 명령 대상으로 쓸 수 있는, 정상 연결된 기기인지. */
        val isOnline: Boolean get() = state == STATE_DEVICE
    }

    /** `adb forward --list` 한 항목. 예: `emulator-5554 tcp:8099 tcp:8099`. */
    data class AdbForward(
        val serial: String,
        val local: String,
        val remote: String,
    )

    /** `adb devices` — 연결된 기기 목록. */
    fun devices(): Result<List<AdbDevice>> = call {
        val out = exec(listOf(resolveAdb(), "devices"))
        parseDevices(out)
    }

    /** `adb -s <serial> forward tcp:<port> tcp:<port>` — 기기 loopback 포트를 데스크톱으로 노출. */
    fun forward(serial: String, port: Int): Result<Unit> = call {
        exec(listOf(resolveAdb(), "-s", serial, "forward", "tcp:$port", "tcp:$port"))
        Unit
    }

    /** `adb -s <serial> forward --remove tcp:<port>` — 포워딩 해제. */
    fun removeForward(serial: String, port: Int): Result<Unit> = call {
        exec(listOf(resolveAdb(), "-s", serial, "forward", "--remove", "tcp:$port"))
        Unit
    }

    /** `adb -s <serial> forward --list` — 해당 기기에 설정된 포워딩 목록. */
    fun listForwards(serial: String): Result<List<AdbForward>> = call {
        val out = exec(listOf(resolveAdb(), "-s", serial, "forward", "--list"))
        parseForwards(out)
    }

    /**
     * 사람이 읽기 좋은 기기명. 에뮬레이터(`emulator-*`)는 AVD 이름(`emu avd name`)을, 실기기는
     * `getprop ro.product.model`(예: "Pixel 6a")을 돌려준다. UI 드롭다운 표시용이며 명령 대상
     * 식별은 여전히 serial 로 한다. 조회 실패는 [Result.failure].
     */
    fun deviceName(serial: String): Result<String> = call {
        if (serial.startsWith(EMULATOR_SERIAL_PREFIX)) {
            val out = exec(listOf(resolveAdb(), "-s", serial, "emu", "avd", "name"))
            parseEmuAvdName(out) ?: getProp(serial, PROP_MODEL)
        } else {
            getProp(serial, PROP_MODEL)
        }
    }

    /** `adb -s <serial> shell getprop <prop>` 의 값(trim). */
    private fun getProp(serial: String, prop: String): String =
        exec(listOf(resolveAdb(), "-s", serial, "shell", "getprop", prop)).trim()

    /**
     * adb 실행파일 경로를 결정한다. `ANDROID_HOME` → `ANDROID_SDK_ROOT` 의 `platform-tools/adb`
     * 가 실재하면 그 절대경로를, 둘 다 없으면 `PATH` 위임을 기대하고 `adb` 를 반환한다. 후보 SDK 가
     * 지정됐는데 실행파일이 없으면 [AdbException] 으로 실패시킨다(오타·미설치 조기 발견).
     */
    private fun resolveAdb(): String {
        for (key in SDK_ENV_KEYS) {
            val home = env(key)?.takeIf { it.isNotBlank() } ?: continue
            val candidate = File(File(home, "platform-tools"), adbExecutableName())
            if (candidate.isFile) return candidate.absolutePath
            throw AdbException("$key=$home 아래에서 adb 실행파일을 찾지 못했습니다: ${candidate.absolutePath}")
        }
        return adbExecutableName()
    }

    /** 프로세스를 실행하고 비정상 종료/타임아웃을 [AdbException] 으로 변환한 뒤 stdout 을 돌려준다. */
    private fun exec(command: List<String>): String {
        val result = runner(command)
        if (result.exitCode == null) {
            throw AdbException("adb 명령이 ${timeout}s 안에 끝나지 않았습니다: ${command.joinToString(" ")}")
        }
        if (result.exitCode != 0) {
            val detail = result.stderr.ifBlank { result.stdout }.trim()
            throw AdbException("adb 명령 실패(exit ${result.exitCode}): ${command.joinToString(" ")}${if (detail.isEmpty()) "" else " — $detail"}")
        }
        return result.stdout
    }

    /** 블록을 실행하고 던져진 예외를 [Result.failure] 로 흡수한다. */
    private inline fun <T> call(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        }

    companion object {
        const val DEFAULT_TIMEOUT_SECONDS = 10L

        /** 정상 연결 상태 토큰. */
        const val STATE_DEVICE = "device"

        /** 에뮬레이터 serial 접두사(예: `emulator-5554`). */
        private const val EMULATOR_SERIAL_PREFIX = "emulator-"

        /** 기기 모델명 시스템 프로퍼티 키(예: "Pixel 6a"). */
        private const val PROP_MODEL = "ro.product.model"

        /**
         * `adb -s <serial> emu avd name` 출력에서 AVD 이름을 뽑는다. 출력은 보통
         * `"<name>\nOK"` 형태라 빈 줄과 `OK` 종료 토큰을 건너뛴 첫 줄을 쓴다. 없으면 null.
         */
        fun parseEmuAvdName(output: String): String? =
            output.lineSequence()
                .map { it.trim() }
                .firstOrNull { it.isNotEmpty() && it != "OK" }

        /** adb 탐색 시 우선순위대로 조회하는 SDK 루트 env 키. */
        private val SDK_ENV_KEYS = listOf("ANDROID_HOME", "ANDROID_SDK_ROOT")

        private fun adbExecutableName(): String =
            if (System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)) "adb.exe" else "adb"

        /** 실제 프로세스를 ProcessBuilder 로 실행하는 기본 runner. 타임아웃 시 [ProcessResult.exitCode] 가 null. */
        private fun runProcess(command: List<String>, timeout: Long): ProcessResult {
            val process = ProcessBuilder(command)
                .redirectErrorStream(false)
                .start()
            val finished = process.waitFor(timeout, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return ProcessResult(exitCode = null, stdout = "", stderr = "")
            }
            val stdout = process.inputStream.readBytes().toString(Charsets.UTF_8)
            val stderr = process.errorStream.readBytes().toString(Charsets.UTF_8)
            return ProcessResult(exitCode = process.exitValue(), stdout = stdout, stderr = stderr)
        }

        /**
         * `adb devices` 출력을 파싱한다. 첫 헤더 줄("List of devices attached")과 빈 줄·잡음 줄은
         * 건너뛰고, `"<serial>\t<state>"` 형태(공백/탭 구분)만 [AdbDevice] 로 만든다.
         */
        fun parseDevices(output: String): List<AdbDevice> =
            output.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .filterNot { it.startsWith("List of devices attached") }
                .mapNotNull { line ->
                    val cols = line.split(Regex("\\s+"))
                    if (cols.size < 2) return@mapNotNull null
                    AdbDevice(serial = cols[0], state = cols[1])
                }
                .toList()

        /**
         * `adb forward --list` 출력을 파싱한다. 각 줄은 `"<serial> <local> <remote>"` 형태
         * (예: `emulator-5554 tcp:8099 tcp:8099`). 빈 줄·형식 미달 줄은 건너뛴다.
         */
        fun parseForwards(output: String): List<AdbForward> =
            output.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull { line ->
                    val cols = line.split(Regex("\\s+"))
                    if (cols.size < 3) return@mapNotNull null
                    AdbForward(serial = cols[0], local = cols[1], remote = cols[2])
                }
                .toList()
    }
}

/** adb 미탐색·비정상 종료·타임아웃 등 adb 명령 수행 실패의 구조화된 사유. */
class AdbException(message: String) : RuntimeException(message)
