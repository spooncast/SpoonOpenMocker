package net.spooncast.openmocker.plugin.session

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import net.spooncast.openmocker.plugin.adb.AdbException
import net.spooncast.openmocker.plugin.adb.AdbService
import net.spooncast.openmocker.plugin.settings.MockerSettings

enum class ConnectionState { IDLE, FORWARDING, CONNECTED, ERROR }

@Service(Service.Level.PROJECT)
class MockerSession(@Suppress("UNUSED_PARAMETER") project: Project) {
    private val adb = AdbService()
    private val settings get() = MockerSettings.getInstance().state

    var connectionState: ConnectionState = ConnectionState.IDLE
        private set
    var errorMessage: String? = null
        private set
    var selectedSerial: String? = null
        private set

    private val listeners = mutableListOf<(ConnectionState, String?) -> Unit>()

    private var restPanelCallback: (() -> Unit)? = null
    private var restPanelStopCallback: (() -> Unit)? = null

    fun addListener(listener: (ConnectionState, String?) -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: (ConnectionState, String?) -> Unit) {
        listeners -= listener
    }

    fun registerPollingCallbacks(start: () -> Unit, stop: () -> Unit) {
        restPanelCallback = start
        restPanelStopCallback = stop
    }

    fun loadDevices(): Result<List<AdbService.AdbDevice>> = adb.devices()

    /** 드롭다운 표시용 사람이 읽기 좋은 기기명. 실패 시 [Result.failure]. */
    fun deviceName(serial: String): Result<String> = adb.deviceName(serial)

    fun selectDevice(serial: String?) {
        selectedSerial = serial
        MockerSettings.getInstance().state.lastDeviceSerial = serial ?: ""
    }

    fun startSession(port: Int = settings.port) {
        val serial = selectedSerial
        if (serial == null) {
            transition(ConnectionState.IDLE, null)
            return
        }
        if (settings.autoForward) {
            transition(ConnectionState.FORWARDING, null)
            val result = adb.forward(serial, port)
            if (result.isFailure) {
                val msg = (result.exceptionOrNull() as? AdbException)?.message
                    ?: result.exceptionOrNull()?.message
                    ?: "포워딩 실패"
                transition(ConnectionState.ERROR, msg)
                return
            }
        }
        transition(ConnectionState.CONNECTED, null)
        restPanelCallback?.invoke()
    }

    fun stopSession(port: Int = settings.port, removeForward: Boolean = false) {
        restPanelStopCallback?.invoke()
        if (removeForward) {
            val serial = selectedSerial
            if (serial != null) adb.removeForward(serial, port)
        }
        transition(ConnectionState.IDLE, null)
    }

    private fun transition(state: ConnectionState, error: String?) {
        connectionState = state
        errorMessage = error
        listeners.forEach { it(state, error) }
    }
}
