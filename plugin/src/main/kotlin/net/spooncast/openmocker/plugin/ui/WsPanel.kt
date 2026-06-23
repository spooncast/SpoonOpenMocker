package net.spooncast.openmocker.plugin.ui

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
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
    private val injectButton = JButton("Inject").apply { isEnabled = false }
    private val statusLabel = JBLabel("")
    private val refreshButton = JButton("새로고침")

    init {
        add(buildToolbar(), BorderLayout.NORTH)
        add(buildCenterPanel(), BorderLayout.CENTER)
        add(buildSouthPanel(), BorderLayout.SOUTH)
        wireActions()
        loadSinks()
    }

    private fun buildToolbar(): JPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 2)).apply {
        add(refreshButton)
        add(JBLabel("Sink:"))
        add(sinkCombo)
    }

    private fun buildCenterPanel(): JPanel = JPanel(BorderLayout()).apply {
        add(presetsPanel, BorderLayout.NORTH)
        add(JBScrollPane(payloadArea), BorderLayout.CENTER)
    }

    private fun buildSouthPanel(): JPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 4)).apply {
        add(injectButton)
        add(statusLabel)
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
                    statusLabel.text = if (sinks.isEmpty()) "등록된 sink 없음" else ""
                } else {
                    statusLabel.text = "✗ sink 로드 실패: ${result.exceptionOrNull()?.message}"
                }
            }
        }.apply { isDaemon = true; start() }
    }

    private fun updatePresets(sink: Sink) {
        presetsPanel.removeAll()
        sink.presets.forEach { preset ->
            presetsPanel.add(JButton(preset.name).apply {
                addActionListener { payloadArea.text = preset.payload }
            })
        }
        presetsPanel.revalidate()
        presetsPanel.repaint()
    }
}
