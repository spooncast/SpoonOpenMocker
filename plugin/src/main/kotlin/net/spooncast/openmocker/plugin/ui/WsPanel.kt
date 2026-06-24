package net.spooncast.openmocker.plugin.ui

import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.InlineBanner
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import net.spooncast.openmocker.plugin.net.ControlClient
import net.spooncast.openmocker.plugin.net.Sink
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.SwingUtilities

class WsPanel(private val client: ControlClient) : JPanel(BorderLayout()) {

    private var sinks: List<Sink> = emptyList()
    private val sinkCombo = JComboBox<String>()
    private val presetsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 2))
    private val payloadArea = JBTextArea(8, 40).apply { lineWrap = true; wrapStyleWord = true }
    private val injectButton = JButton("보내기").apply { isEnabled = false }
    private val statusLabel = JBLabel("")
    private val refreshButton = JButton("새로고침")

    init {
        border = JBUI.Borders.empty(8)
        add(buildHeader(), BorderLayout.NORTH)
        add(buildCenterPanel(), BorderLayout.CENTER)
        add(buildSouthPanel(), BorderLayout.SOUTH)
        wireActions()
        loadSinks()
    }

    // 기능 설명을 닫기 가능한 InlineBanner 로 최상단(대상 셀렉터 위)에 둔다.
    // 닫기는 세션 한정 — 배너를 제거할 뿐, 툴윈도우를 다시 열면 재등장한다.
    // 박스 아래 8px 간격은 BorderLayout vgap 으로 줘 배너 자체 스타일은 건드리지 않는다.
    private fun buildHeader(): JPanel = JPanel(BorderLayout(0, 8)).apply {
        val banner = InlineBanner(
            "실제 서버 없이, 선택한 대상으로 가짜 메시지를 보내 앱이 메시지를 받는 상황을 재현합니다.",
            EditorNotificationPanel.Status.Info,
        ).showCloseButton(true)
        banner.setCloseAction {
            remove(banner)
            revalidate()
            repaint()
        }
        add(banner, BorderLayout.NORTH)
        add(buildToolbar(), BorderLayout.CENTER)
    }

    // 대상 셀렉터는 좌측에, 액션 버튼(새로고침)은 우측 끝에 배치한다.
    private fun buildToolbar(): JPanel = JPanel(BorderLayout()).apply {
        val left = JPanel(FlowLayout(FlowLayout.LEFT, 4, 2)).apply {
            add(JBLabel("대상:"))
            add(sinkCombo)
        }
        val right = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 2)).apply {
            add(refreshButton)
        }
        add(left, BorderLayout.WEST)
        add(right, BorderLayout.EAST)
    }

    private fun buildCenterPanel(): JPanel = JPanel(BorderLayout()).apply {
        add(presetsPanel, BorderLayout.NORTH)
        add(JBScrollPane(payloadArea), BorderLayout.CENTER)
    }

    // 상태 라벨은 좌측에, Inject 버튼은 우측 끝에 배치한다.
    private fun buildSouthPanel(): JPanel = JPanel(BorderLayout()).apply {
        val left = JPanel(FlowLayout(FlowLayout.LEFT, 4, 4)).apply {
            add(statusLabel)
        }
        val right = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 4)).apply {
            add(injectButton)
        }
        add(left, BorderLayout.WEST)
        add(right, BorderLayout.EAST)
    }

    private fun wireActions() {
        refreshButton.addActionListener { loadSinks() }

        sinkCombo.addActionListener {
            val idx = sinkCombo.selectedIndex
            if (idx >= 0 && idx < sinks.size) updatePresets(sinks[idx])
        }

        injectButton.addActionListener {
            val idx = sinkCombo.selectedIndex.takeIf { it >= 0 } ?: return@addActionListener
            val sink = sinks.getOrNull(idx) ?: return@addActionListener
            val payload = payloadArea.text
            Thread {
                val result = client.inject(sink.id, payload)
                SwingUtilities.invokeLater {
                    statusLabel.text = if (result.isSuccess) "● 주입 성공" else "✗ 실패: ${result.exceptionOrNull()?.message}"
                }
            }.apply { isDaemon = true; start() }
        }
    }

    private fun loadSinks() {
        statusLabel.text = "로드 중…"
        Thread {
            val result = client.getSinks()
            SwingUtilities.invokeLater {
                if (result.isSuccess) {
                    sinks = result.getOrThrow()
                    sinkCombo.removeAllItems()
                    sinks.forEach { sinkCombo.addItem(it.name) }
                    if (sinks.isNotEmpty()) {
                        sinkCombo.selectedIndex = 0
                        updatePresets(sinks[0])
                        injectButton.isEnabled = true
                    } else {
                        presetsPanel.removeAll()
                        presetsPanel.revalidate()
                        presetsPanel.repaint()
                        injectButton.isEnabled = false
                    }
                    statusLabel.text = if (sinks.isEmpty()) "등록된 대상 없음" else ""
                } else {
                    statusLabel.text = "✗ 대상 로드 실패: ${result.exceptionOrNull()?.message}"
                }
            }
        }.apply { isDaemon = true; start() }
    }

    private fun updatePresets(sink: Sink) {
        presetsPanel.removeAll()
        if (sink.presets.isNotEmpty()) presetsPanel.add(JBLabel("예시 메시지:"))
        sink.presets.forEach { preset ->
            presetsPanel.add(JButton(preset.name).apply {
                addActionListener { payloadArea.text = preset.payload }
            })
        }
        presetsPanel.revalidate()
        presetsPanel.repaint()
    }
}
