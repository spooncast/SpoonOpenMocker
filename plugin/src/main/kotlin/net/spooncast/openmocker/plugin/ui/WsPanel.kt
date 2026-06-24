package net.spooncast.openmocker.plugin.ui

import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.InlineBanner
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import net.spooncast.openmocker.plugin.net.ControlClient
import net.spooncast.openmocker.plugin.net.ReceivedMessage
import net.spooncast.openmocker.plugin.net.Sink
import net.spooncast.openmocker.plugin.settings.MockerSettings
import net.spooncast.openmocker.plugin.util.JsonFormatter
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities

class WsPanel(private val client: ControlClient) : JPanel(BorderLayout()) {

    private var sinks: List<Sink> = emptyList()

    // 폴링(백그라운드 스레드)이 EDT 의 sinkCombo 를 건드리지 않도록, 현재 선택 sink id 를 별도로 들고 있는다.
    @Volatile
    private var selectedSinkId: String? = null

    private val sinkCombo = JComboBox<String>()
    private val presetsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 2))

    // 수신 메시지 표 — REST 탭의 JBTable 골격을 따른다(단일 컬럼 "수신 메시지", payload 미리보기).
    private val receivedModel = ReceivedTableModel()
    private val receivedTable = JBTable(receivedModel).apply {
        selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        setShowGrid(false)
        tableHeader.reorderingAllowed = false
    }

    // 폴링이 항목을 갱신하며 selection 을 복원할 때는, 그 프로그램적 선택이 payloadArea 를 덮지 않게 막는다.
    private var suppressReceivedSelection = false

    private val payloadArea = JBTextArea(8, 40).apply {
        lineWrap = true; wrapStyleWord = true
        margin = JBUI.insets(6)  // 입력란 안쪽 여백 — Status Code 필드의 기본 LaF inset 과 맞춤
    }
    private val injectButton = JButton("보내기").apply { isEnabled = false }
    private val statusLabel = JBLabel("")
    private val refreshButton = JButton("새로고침")

    private var scheduler: ScheduledExecutorService? = null

    init {
        border = JBUI.Borders.empty(8)
        add(buildHeader(), BorderLayout.NORTH)
        add(buildCenterSplit(), BorderLayout.CENTER)
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

    // 상단=수신 메시지 표, 하단="편집" 박스. REST 탭과 동일하게 사용자가 경계를 드래그해 비율을 조정한다.
    private fun buildCenterSplit(): OnePixelSplitter = OnePixelSplitter(true, 0.4f).apply {
        firstComponent = JBScrollPane(receivedTable)
        secondComponent = buildEditPanel()
    }

    // REST 탭의 "편집" 박스 골격: 예시 버튼(NORTH) + payload 입력란(CENTER) + 상태·보내기(SOUTH).
    private fun buildEditPanel(): JPanel = JPanel(BorderLayout()).apply {
        border = BorderFactory.createTitledBorder("편집")
        add(presetsPanel, BorderLayout.NORTH)
        add(JBScrollPane(payloadArea), BorderLayout.CENTER)
        add(buildEditFooter(), BorderLayout.SOUTH)
    }

    // 상태 라벨은 좌측에, Inject 버튼은 우측 끝에(REST 의 저장/해제 버튼 위치와 동일).
    private fun buildEditFooter(): JPanel = JPanel(BorderLayout()).apply {
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
        refreshButton.addActionListener {
            loadSinks()
            refreshReceivedAsync()
        }

        sinkCombo.addActionListener {
            val idx = sinkCombo.selectedIndex
            if (idx >= 0 && idx < sinks.size) {
                selectedSinkId = sinks[idx].id
                updatePresets(sinks[idx])
                receivedModel.update(emptyList())  // 대상이 바뀌면 이전 sink 의 수신 목록을 비우고
                refreshReceivedAsync()             // 새 sink 의 목록을 즉시 가져온다
            }
        }

        // 수신 항목을 사용자가 선택하면 payload 를 입력란에 복사한다(preset 클릭과 동일 효과).
        // 폴링이 selection 을 복원할 때(suppressReceivedSelection)는 복사하지 않는다.
        receivedTable.selectionModel.addListSelectionListener { e ->
            if (e.valueIsAdjusting || suppressReceivedSelection) return@addListSelectionListener
            val row = receivedTable.selectedRow
            if (row < 0) return@addListSelectionListener
            payloadArea.text = JsonFormatter.pretty(receivedModel.getMessageAt(row).payload)
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
                        selectedSinkId = sinks[0].id
                        updatePresets(sinks[0])
                        injectButton.isEnabled = true
                    } else {
                        selectedSinkId = null
                        presetsPanel.removeAll()
                        presetsPanel.revalidate()
                        presetsPanel.repaint()
                        receivedModel.update(emptyList())
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
                addActionListener { payloadArea.text = JsonFormatter.pretty(preset.payload) }
            })
        }
        presetsPanel.revalidate()
        presetsPanel.repaint()
    }

    /** 현재 선택된 sink 의 수신 메시지를 (호출 스레드에서) 가져와 EDT 에서 표를 갱신한다. 폴링 스케줄러가 직접 호출한다. */
    private fun refreshReceived() {
        val id = selectedSinkId ?: return
        val result = client.getReceived(id)
        if (result.isSuccess) {
            val items = result.getOrThrow()
            SwingUtilities.invokeLater { updateReceivedList(items) }
        }
    }

    /** EDT 트리거(대상 변경·새로고침)용 — [refreshReceived] 를 데몬 스레드로 감싸 호출한다. */
    private fun refreshReceivedAsync() {
        Thread(::refreshReceived).apply { isDaemon = true; start() }
    }

    /**
     * 수신 표를 새 항목으로 교체한다. 내용(seq 순서)이 같으면 깜빡임·selection churn 을 피해 건너뛴다.
     * 교체 시에는 기존 선택을 seq 로 복원하되, 그 복원이 payloadArea 를 덮지 않게 한다.
     */
    private fun updateReceivedList(items: List<ReceivedMessage>) {
        if (receivedModel.seqs() == items.map { it.seq }) return

        val selectedSeq = receivedTable.selectedRow
            .takeIf { it >= 0 }
            ?.let { receivedModel.getMessageAt(it).seq }
        suppressReceivedSelection = true
        try {
            receivedModel.update(items)
            val newRow = items.indexOfFirst { it.seq == selectedSeq }
            if (newRow >= 0) receivedTable.setRowSelectionInterval(newRow, newRow)
        } finally {
            suppressReceivedSelection = false
        }
    }

    /** ToolWindow 가시 상태에 맞춰 세션이 호출한다(RestPanel 과 동일 생명주기). */
    fun startPolling() {
        // 세션 연결(adb forward 직후)에 호출된다. init 시점의 loadSinks 는 forward 전이라 실패할 수 있어,
        // 아직 sink 를 못 받았으면 여기서(forward 가 선 상태에서) 다시 로드한다. 콤보가 채워지면 그 리스너가
        // 첫 수신 목록 조회까지 켠다. 이미 받았으면(사용자 선택 보존) 재로딩하지 않는다.
        if (sinks.isEmpty()) loadSinks()
        if (scheduler != null) return
        val intervalMs = MockerSettings.getInstance().state.pollIntervalMs.toLong()
        val executor = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "openmocker-ws-poll").apply { isDaemon = true }
        }
        scheduler = executor
        executor.scheduleAtFixedRate(::refreshReceived, 0L, intervalMs, TimeUnit.MILLISECONDS)
    }

    fun stopPolling() {
        scheduler?.shutdownNow()
        scheduler = null
    }
}
