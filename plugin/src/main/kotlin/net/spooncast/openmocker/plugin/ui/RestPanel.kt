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
        emptyText.text = "기록된 요청이 없습니다 — 앱에서 네트워크 요청을 보내면 여기에 표시됩니다."
        // 세 컬럼 모두 사용자가 드래그로 폭 조정 가능. preferredWidth 로 기존 기본 폭만 유지한다.
        columnModel.getColumn(2).preferredWidth = 70
    }

    private val codeField = JBTextField(6)
    private val delayField = JBTextField(6)
    private val bodyArea = JBTextArea(5, 40).apply {
        lineWrap = true; wrapStyleWord = true
        margin = JBUI.insets(6)  // 입력란 안쪽 여백 — Status Code 필드의 기본 LaF inset 과 맞춤
    }

    private val saveButton = JButton("저장").apply { isEnabled = false }
    private val clearMockButton = JButton("Mock 해제").apply { isEnabled = false }
    private val clearAllButton = JButton("전체 Clear")
    private val refreshButton = JButton("새로고침")

    // 편집 패널의 인라인 피드백(성공·실패·안내). 모달 다이얼로그 대신 써서 흐름을 끊지 않는다(WsPanel 과 동일 방식).
    private val statusLabel = JBLabel("왼쪽에서 항목을 선택하세요")

    // 현재 편집 중인 항목 — 폴링이 selection을 초기화해도 유지됨
    private var editingEntry: RecordedEntry? = null

    // true 동안 선택 리스너가 편집 입력란(코드·지연·본문)을 다시 채우지 않는다.
    // 폴링/저장 후 선택을 프로그램적으로 복구할 때, 입력 중인 값이 덮어써지는 것을 막는다.
    private var suppressFieldReload = false

    // WsPanel 과 동일한 생명주기 — start 마다 재생성하고 stop 에서 null 로 비운다. 단일 생성 후 shutdownNow
    // 하면 재표시(stop→start) 때 죽은 executor 에 schedule 해 예외가 나므로, nullable 로 재생성한다.
    private var scheduler: ScheduledExecutorService? = null

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
            "앱이 보낸 HTTP 요청 목록입니다. 항목을 선택하면 응답(상태 코드·본문·지연)을 원하는 값으로 바꿀 수 있습니다.",
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
        gbc.gridx = 2; gbc.weightx = 0.0; gbc.insets = Insets(2, 12, 2, 4)
        add(JBLabel("Delay (ms)"), gbc)
        gbc.gridx = 3; gbc.weightx = 0.2; gbc.insets = Insets(2, 4, 2, 4)
        add(delayField, gbc)

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0; gbc.fill = GridBagConstraints.NONE
        add(JBLabel("Body"), gbc)
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1.0; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH
        add(JBScrollPane(bodyArea), gbc)

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 4; gbc.weighty = 0.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 1.0
        // 상태 라벨(좌) + 액션 버튼(우). statusLabel 은 흐름을 끊지 않는 인라인 피드백 자리.
        val footer = JPanel(BorderLayout()).apply {
            add(JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply { add(statusLabel) }, BorderLayout.WEST)
            add(JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
                add(clearMockButton)
                add(saveButton)
            }, BorderLayout.EAST)
        }
        add(footer, gbc)
    }

    private fun wireActions() {
        table.selectionModel.addListSelectionListener { e ->
            if (e.valueIsAdjusting) return@addListSelectionListener
            val row = table.selectedRow
            if (row < 0) return@addListSelectionListener  // 폴링 리셋 시 editingEntry 유지
            val entry = tableModel.getEntryAt(row)
            editingEntry = entry
            // 갱신된 항목의 mock 여부를 반영 — 프로그램적 선택 복구에서도 버튼 상태는 맞춘다.
            clearMockButton.isEnabled = entry.mock != null
            // 선택 복구(폴링/저장 후)일 때는 입력 중인 값을 보존하기 위해 입력란을 다시 채우지 않는다.
            if (suppressFieldReload) return@addListSelectionListener
            codeField.text = (entry.mock?.code ?: entry.response.code).toString()
            delayField.text = (entry.mock?.duration ?: 0L).toString()
            bodyArea.text = JsonFormatter.pretty(entry.mock?.body ?: entry.response.body)
            saveButton.isEnabled = false  // 방금 불러온 값 = 미저장 변경 없음
            statusLabel.text = ""         // 선택 전 안내 제거
        }

        // body/code/delay 수정 시 편집 중인 항목이 있으면 저장 버튼 활성화
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
        delayField.document.addDocumentListener(editListener)

        // 입력란에서 Enter 로 바로 저장(본문은 줄바꿈이라 제외).
        codeField.addActionListener { if (saveButton.isEnabled) saveButton.doClick() }
        delayField.addActionListener { if (saveButton.isEnabled) saveButton.doClick() }

        saveButton.addActionListener {
            val entry = editingEntry ?: return@addActionListener
            // 입력 읽기·검증은 EDT 에서(필드 접근). 검증 실패는 인라인 상태로 알린다.
            val raw = codeField.text.trim()
            val code = raw.toIntOrNull()
            if (code == null) {
                statusLabel.text = "✗ Status Code 는 숫자여야 합니다: '$raw'"
                return@addActionListener
            }
            val delayRaw = delayField.text.trim().ifEmpty { "0" }
            val duration = delayRaw.toLongOrNull()
            if (duration == null || duration < 0) {
                statusLabel.text = "✗ Delay 는 0 이상의 숫자여야 합니다: '$delayRaw'"
                return@addActionListener
            }
            val body = bodyArea.text
            statusLabel.text = "저장 중…"
            // HTTP 는 백그라운드에서 — EDT 블로킹(프리징) 방지. 결과·후속 refresh 만 처리.
            runAsync {
                val result = client.upsertMock(
                    MockRequest(method = entry.method, path = entry.path, code = code, body = body, duration = duration)
                )
                SwingUtilities.invokeLater {
                    if (result.isFailure) {
                        statusLabel.text = "✗ 저장 실패: ${result.exceptionOrNull()?.message}"
                    } else {
                        statusLabel.text = "● 저장됨"
                        saveButton.isEnabled = false
                    }
                }
                // 성공 시에만 갱신 — 선택을 그대로 복구하고, 리스너가 갱신된(mocked) 항목을 읽어
                // Mock 해제 버튼을 활성화한다. refresh 는 성공 시 statusLabel 을 건드리지 않는다.
                if (result.isSuccess) refresh()
            }
        }

        clearMockButton.addActionListener {
            val entry = editingEntry ?: return@addActionListener
            statusLabel.text = "Mock 해제 중…"
            runAsync {
                val result = client.clearMock(method = entry.method, path = entry.path)
                SwingUtilities.invokeLater {
                    if (result.isFailure) {
                        statusLabel.text = "✗ Mock 해제 실패: ${result.exceptionOrNull()?.message}"
                    } else {
                        statusLabel.text = "● Mock 해제됨"
                        editingEntry = null
                        clearMockButton.isEnabled = false
                        saveButton.isEnabled = false
                    }
                }
                if (result.isSuccess) refresh()
            }
        }

        clearAllButton.addActionListener {
            // 파괴적·비가역 작업 — 확인을 받는다.
            val answer = Messages.showYesNoDialog(
                this,
                "기록된 요청과 설정된 mock 을 모두 삭제합니다. 되돌릴 수 없습니다. 계속할까요?",
                "전체 Clear",
                Messages.getWarningIcon(),
            )
            if (answer != Messages.YES) return@addActionListener
            statusLabel.text = "전체 삭제 중…"
            runAsync {
                val result = client.clearAll()
                SwingUtilities.invokeLater {
                    if (result.isFailure) {
                        statusLabel.text = "✗ 전체 Clear 실패: ${result.exceptionOrNull()?.message}"
                    } else {
                        statusLabel.text = "● 전체 삭제됨"
                        editingEntry = null
                        saveButton.isEnabled = false
                        clearMockButton.isEnabled = false
                    }
                }
                if (result.isSuccess) refresh()
            }
        }

        refreshButton.addActionListener { runAsync { refresh() } }
    }

    private fun runAsync(action: () -> Unit) {
        Thread(action).apply { isDaemon = true; start() }
    }

    fun startPolling() {
        if (scheduler != null) return  // 중복 start(이중 startSession) 시 폴링 루프 중복 생성 방지
        val intervalMs = MockerSettings.getInstance().state.pollIntervalMs.toLong()
        val executor = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "openmocker-poll").apply { isDaemon = true }
        }
        scheduler = executor
        executor.scheduleAtFixedRate(::refresh, 0L, intervalMs, TimeUnit.MILLISECONDS)
    }

    /**
     * 기록 목록을 갱신한다. 폴링 스케줄러와 버튼 핸들러가 모두 백그라운드 스레드에서 호출한다.
     * 실패는 인라인 상태로 알린다(연결 끊김 등이 조용히 묻히지 않도록). 성공 시에는 statusLabel 을
     * 건드리지 않아 직전 액션 피드백("● 저장됨" 등)을 덮지 않는다.
     */
    private fun refresh() {
        val result = client.getRecorded()
        if (result.isSuccess) {
            val entries = result.getOrThrow()
            SwingUtilities.invokeLater {
                tableModel.update(entries)
                restoreSelection()
            }
        } else {
            SwingUtilities.invokeLater {
                statusLabel.text = "✗ 갱신 실패: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    /**
     * 갱신으로 풀린 선택을, 편집 중이던 항목([editingEntry])의 method+path 키로 같은 행에 복구한다.
     * 테이블 갱신과 같은 EDT 사이클 안에서 즉시 일어나므로 사용자에게는 선택이 그대로 유지되는 것처럼
     * 보인다(깜빡임 없음). 인덱스가 아닌 키로 복구하므로 행이 추가·삭제·재정렬돼도 올바른 항목을 다시 고른다.
     * [suppressFieldReload] 로 선택 리스너의 입력란 갱신은 건너뛰어 입력 중인 값을 보존한다.
     */
    private fun restoreSelection() {
        val target = editingEntry ?: return
        val row = tableModel.indexOfEntry(target.method, target.path)
        if (row < 0) return
        suppressFieldReload = true
        try {
            table.selectionModel.setSelectionInterval(row, row)
        } finally {
            suppressFieldReload = false
        }
    }

    fun stopPolling() {
        scheduler?.shutdownNow()
        scheduler = null
    }
}
