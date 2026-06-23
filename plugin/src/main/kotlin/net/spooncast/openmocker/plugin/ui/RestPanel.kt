package net.spooncast.openmocker.plugin.ui

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import net.spooncast.openmocker.plugin.net.ControlClient
import net.spooncast.openmocker.plugin.net.MockRequest
import net.spooncast.openmocker.plugin.settings.MockerSettings
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities

class RestPanel(private val client: ControlClient) : JPanel(BorderLayout()) {

    private val tableModel = RecordedTableModel()
    private val table = JBTable(tableModel).apply {
        selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        setShowGrid(false)
        columnModel.getColumn(2).maxWidth = 70
        columnModel.getColumn(2).minWidth = 70
    }

    private val codeField = JBTextField(6)
    private val bodyArea = JBTextArea(5, 40).apply { lineWrap = true; wrapStyleWord = true }

    private val saveButton = JButton("저장").apply { isEnabled = false }
    private val clearMockButton = JButton("Mock 해제").apply { isEnabled = false }
    private val clearAllButton = JButton("전체 Clear")
    private val refreshButton = JButton("새로고침")

    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "openmocker-poll").apply { isDaemon = true }
    }

    init {
        add(buildToolbar(), BorderLayout.NORTH)
        add(JBScrollPane(table), BorderLayout.CENTER)
        add(buildEditPanel(), BorderLayout.SOUTH)

        wireActions()
        startPolling()
    }

    private fun buildToolbar(): JPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 2)).apply {
        add(refreshButton)
        add(clearAllButton)
    }

    private fun buildEditPanel(): JPanel = JPanel(GridBagLayout()).apply {
        border = BorderFactory.createTitledBorder("편집")
        val gbc = GridBagConstraints().apply {
            insets = Insets(2, 4, 2, 4)
            fill = GridBagConstraints.HORIZONTAL
        }

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0
        add(JBLabel("Status Code"), gbc)
        gbc.gridx = 1; gbc.weightx = 0.2
        add(codeField, gbc)

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0; gbc.fill = GridBagConstraints.NONE
        add(JBLabel("Body"), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.BOTH
        add(JBScrollPane(bodyArea), gbc)

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE
        gbc.anchor = GridBagConstraints.EAST; gbc.weightx = 0.0
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
            add(clearMockButton)
            add(saveButton)
        }
        add(buttonPanel, gbc)
    }

    private fun wireActions() {
        table.selectionModel.addListSelectionListener { e ->
            if (e.valueIsAdjusting) return@addListSelectionListener
            val row = table.selectedRow
            if (row < 0) {
                saveButton.isEnabled = false
                clearMockButton.isEnabled = false
                return@addListSelectionListener
            }
            val entry = tableModel.getEntryAt(row)
            codeField.text = (entry.mock?.code ?: entry.response.code).toString()
            bodyArea.text = entry.mock?.body ?: entry.response.body
            saveButton.isEnabled = true
            clearMockButton.isEnabled = entry.mock != null
        }

        saveButton.addActionListener {
            val row = table.selectedRow.takeIf { it >= 0 } ?: return@addActionListener
            val entry = tableModel.getEntryAt(row)
            val code = codeField.text.trim().toIntOrNull() ?: return@addActionListener
            val body = bodyArea.text
            client.upsertMock(MockRequest(method = entry.method, path = entry.path, code = code, body = body))
            refresh()
        }

        clearMockButton.addActionListener {
            val row = table.selectedRow.takeIf { it >= 0 } ?: return@addActionListener
            val entry = tableModel.getEntryAt(row)
            client.clearMock(method = entry.method, path = entry.path)
            refresh()
        }

        clearAllButton.addActionListener {
            client.clearAll()
            refresh()
        }

        refreshButton.addActionListener { refresh() }
    }

    private fun startPolling() {
        val intervalMs = MockerSettings.getInstance().state.pollIntervalMs.toLong()
        scheduler.scheduleAtFixedRate(::refresh, 0L, intervalMs, TimeUnit.MILLISECONDS)
    }

    private fun refresh() {
        val result = client.getRecorded()
        if (result.isSuccess) {
            SwingUtilities.invokeLater {
                tableModel.update(result.getOrThrow())
            }
        }
    }

    fun stopPolling() {
        scheduler.shutdownNow()
    }
}
