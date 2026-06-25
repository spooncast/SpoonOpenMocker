package net.spooncast.openmocker.plugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import net.spooncast.openmocker.plugin.adb.AdbService
import net.spooncast.openmocker.plugin.net.ControlClient
import net.spooncast.openmocker.plugin.session.ConnectionState
import net.spooncast.openmocker.plugin.session.MockerSession
import net.spooncast.openmocker.plugin.settings.MockerSettings
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.SwingUtilities

class MockerToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val session = project.getService(MockerSession::class.java)
        val settings = MockerSettings.getInstance().state
        val client = ControlClient(port = settings.port)

        val restPanel = RestPanel(client)
        val wsPanel = WsPanel(client)
        val statusBar = StatusBar(project)

        session.registerPollingCallbacks(
            start = {
                SwingUtilities.invokeLater {
                    restPanel.startPolling()
                    wsPanel.startPolling()
                }
            },
            stop = {
                SwingUtilities.invokeLater {
                    restPanel.stopPolling()
                    wsPanel.stopPolling()
                }
            },
        )

        val sessionListener: (ConnectionState, String?) -> Unit = { state, error ->
            statusBar.update(state, error)
        }
        session.addListener(sessionListener)

        // 기기 드롭다운
        val deviceCombo = JComboBox<String>()
        var devices: List<AdbService.AdbDevice> = emptyList()
        // true 동안 deviceCombo 의 프로그램적 갱신(removeAllItems/selectedIndex)이 리스너를 발화시키지
        // 않게 한다 — 같은 기기로 selectDevice/startSession 이 중복 실행되는 것을 막는다.
        var suppressDeviceEvent = false

        fun runAsync(block: () -> Unit) {
            Thread(block).apply { isDaemon = true; start() }
        }

        fun loadDevices() {
            Thread {
                val result = session.loadDevices()
                val onlineDevices = result.getOrNull()?.filter { it.isOnline }.orEmpty()
                // 기기명은 EDT 밖에서 미리 조회한다. "<기기명> (<serial>)" 형식, 실패 시 serial 만.
                val labels = onlineDevices.associate { device ->
                    val name = session.deviceName(device.serial).getOrNull()?.takeIf { it.isNotBlank() }
                    device.serial to (name?.let { "$it (${device.serial})" } ?: device.serial)
                }
                SwingUtilities.invokeLater {
                    suppressDeviceEvent = true
                    deviceCombo.removeAllItems()
                    var serialToStart: String? = null
                    if (result.isSuccess) {
                        devices = onlineDevices
                        devices.forEach { deviceCombo.addItem(labels[it.serial]) }
                        val lastSerial = MockerSettings.getInstance().state.lastDeviceSerial
                        val idx = devices.indexOfFirst { it.serial == lastSerial }
                        when {
                            idx >= 0 -> {
                                deviceCombo.selectedIndex = idx
                                serialToStart = devices[idx].serial
                            }
                            devices.isNotEmpty() -> {
                                deviceCombo.selectedIndex = 0
                                serialToStart = devices[0].serial
                            }
                            else -> session.selectDevice(null)
                        }
                    } else {
                        devices = emptyList()
                        session.selectDevice(null)
                    }
                    suppressDeviceEvent = false
                    // 리스너를 억제했으므로 선택된 기기의 세션 시작을 직접(백그라운드로) 트리거한다.
                    serialToStart?.let { serial ->
                        session.selectDevice(serial)
                        runAsync { session.startSession(settings.port) }
                    }
                }
            }.apply { isDaemon = true; start() }
        }

        deviceCombo.addActionListener {
            if (suppressDeviceEvent) return@addActionListener
            val idx = deviceCombo.selectedIndex
            if (idx >= 0 && idx < devices.size) {
                val serial = devices[idx].serial
                session.selectDevice(serial)
                // adb.forward 는 블로킹이므로 EDT 밖에서 — 기기 전환 시 IDE 프리징 방지.
                runAsync { session.startSession(settings.port) }
            }
        }

        val refreshButton = JButton("새로고침").apply {
            addActionListener { loadDevices() }
        }

        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT, 4, 2)).apply {
            border = JBUI.Borders.empty(8)
            add(JBLabel("기기:"))
            add(deviceCombo)
            add(refreshButton)
            add(statusBar)
        }

        val tabs = JTabbedPane().apply {
            addTab("REST", restPanel)
            addTab("Event Injection", wsPanel)
        }

        val mainPanel = JPanel(BorderLayout()).apply {
            add(toolbar, BorderLayout.NORTH)
            add(tabs, BorderLayout.CENTER)
        }

        val content = toolWindow.contentManager.factory.createContent(mainPanel, "", false)
        toolWindow.contentManager.addContent(content)

        toolWindow.contentManager.addContentManagerListener(object :
            com.intellij.ui.content.ContentManagerListener {
            override fun contentRemoved(event: com.intellij.ui.content.ContentManagerEvent) {
                session.stopSession(settings.port, removeForward = true)
                session.removeListener(sessionListener)
            }
        })

        // ToolWindow 가시성으로 폴링 start/stop. startSession 은 블로킹 adb.forward 를 포함하므로
        // EDT 밖에서 실행한다. stop(removeForward=false) 은 콜백·상태 전이뿐이라 그대로 둔다.
        toolWindow.component.addPropertyChangeListener("showing") { evt ->
            if (evt.newValue == true) {
                runAsync { session.startSession(settings.port) }
            } else {
                session.stopSession(settings.port, removeForward = false)
            }
        }

        loadDevices()
    }
}
