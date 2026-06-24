package net.spooncast.openmocker.plugin.ui

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.openapi.ui.Messages
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.InlineBanner
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import net.spooncast.openmocker.plugin.util.JsonFormatter
import net.spooncast.openmocker.plugin.net.ControlClient
import net.spooncast.openmocker.plugin.net.MockRequest
import net.spooncast.openmocker.plugin.net.RecordedEntry
import net.spooncast.openmocker.plugin.settings.MockerSettings
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import com.intellij.ui.OnePixelSplitter
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class RestPanel(private val client: ControlClient) : JPanel(BorderLayout()) {

    private val tableModel = RecordedTableModel()
    private val table = JBTable(tableModel).apply {
        selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        setShowGrid(false)
        // 세 컬럼 모두 사용자가 드래그로 폭 조정 가능. preferredWidth 로 기존 기본 폭만 유지한다.
        columnModel.getColumn(2).preferredWidth = 70
    }

    private val codeField = JBTextField(6)
    private val bodyArea = JBTextArea(5, 40).apply {
        lineWrap = true; wrapStyleWord = true
        margin = JBUI.insets(6)  // 입력란 안쪽 여백 — Status Code 필드의 기본 LaF inset 과 맞춤
    }

    private val saveButton = JButton("저장").apply { isEnabled = false }
    private val clearMockButton = JButton("Mock 해제").apply { isEnabled = false }
    private val clearAllButton = JButton("전체 Clear")
    private val refreshButton = JButton("새로고침")

    // 현재 편집 중인 항목 — 폴링이 selection을 초기화해도 유지됨
    private var editingEntry: RecordedEntry? = null

    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "openmocker-poll").apply { isDaemon = true }
    }

    init {
        border = JBUI.Borders.empty(8)
        val splitPane = OnePixelSplitter(true, 0.6f).apply {
            firstComponent = JBScrollPane(table)
            secondComponent = buildEditPanel()
        }

        add(buildHeader(), BorderLayout.NORTH)
        add(splitPane, BorderLayout.CENTER)

        wireActions()
    }

    // 기능 설명을 닫기 가능한 InlineBanner 로 최상단(툴바 위)에 둔다.
    // 닫기는 세션 한정 — 배너를 제거할 뿐, 툴윈도우를 다시 열면 재등장한다.
    // 박스 아래 8px 간격은 BorderLayout vgap 으로 줘 배너 자체 스타일은 건드리지 않는다.
    private fun buildHeader(): JPanel = JPanel(BorderLayout(0, 8)).apply {
        val banner = InlineBanner(
            "앱이 보낸 HTTP 요청 목록입니다. 항목을 선택하면 응답(상태 코드·본문)을 원하는 값으로 바꿀 수 있습니다.",
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

    private fun buildToolbar(): JPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 2)).apply {
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
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH
        add(JBScrollPane(bodyArea), gbc)

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weighty = 0.0
        gbc.fill = GridBagConstraints.NONE
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
            if (row < 0) return@addListSelectionListener  // 폴링 리셋 시 editingEntry 유지
            val entry = tableModel.getEntryAt(row)
            editingEntry = entry
            codeField.text = (entry.mock?.code ?: entry.response.code).toString()
            bodyArea.text = JsonFormatter.pretty(entry.mock?.body ?: entry.response.body)
            saveButton.isEnabled = true
            clearMockButton.isEnabled = entry.mock != null
        }

        // body/code 수정 시 편집 중인 항목이 있으면 저장 버튼 활성화
        val editListener = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = onEdit()
            override fun removeUpdate(e: DocumentEvent) = onEdit()
            override fun changedUpdate(e: DocumentEvent) = onEdit()
            private fun onEdit() {
                if (editingEntry != null) saveButton.isEnabled = true
            }
        }
        bodyArea.document.addDocumentListener(editListener)
        codeField.document.addDocumentListener(editListener)

        saveButton.addActionListener {
            val entry = editingEntry ?: return@addActionListener
            val raw = codeField.text.trim()
            val code = raw.toIntOrNull()
            if (code == null) {
                Messages.showErrorDialog(this, "Status Code 는 숫자여야 합니다: '$raw'", "Mock 저장 실패")
                return@addActionListener
            }
            val body = bodyArea.text
            val result = client.upsertMock(MockRequest(method = entry.method, path = entry.path, code = code, body = body))
            if (reportIfFailed(result, "Mock 저장 실패")) return@addActionListener
            refresh()
        }

        clearMockButton.addActionListener {
            val entry = editingEntry ?: return@addActionListener
            val result = client.clearMock(method = entry.method, path = entry.path)
            if (reportIfFailed(result, "Mock 해제 실패")) return@addActionListener
            editingEntry = null
            clearMockButton.isEnabled = false
            refresh()
        }

        clearAllButton.addActionListener {
            val result = client.clearAll()
            if (reportIfFailed(result, "전체 Clear 실패")) return@addActionListener
            editingEntry = null
            saveButton.isEnabled = false
            clearMockButton.isEnabled = false
            refresh()
        }

        refreshButton.addActionListener { refresh() }
    }

    fun startPolling() {
        val intervalMs = MockerSettings.getInstance().state.pollIntervalMs.toLong()
        scheduler.scheduleAtFixedRate(::refresh, 0L, intervalMs, TimeUnit.MILLISECONDS)
    }

    /**
     * [ControlClient] 호출 [Result] 가 실패면 에러 다이얼로그를 띄우고 true 를 반환한다.
     * 제어 서버 미연결(adb forward 없음)·비2xx 응답 등 실패가 조용히 묻히지 않도록 한다.
     */
    private fun reportIfFailed(result: Result<Unit>, title: String): Boolean {
        val cause = result.exceptionOrNull() ?: return false
        Messages.showErrorDialog(this, cause.message ?: "알 수 없는 오류", title)
        return true
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
