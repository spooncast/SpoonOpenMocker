package net.spooncast.openmocker.plugin.ui

import net.spooncast.openmocker.plugin.net.ReceivedMessage
import javax.swing.table.AbstractTableModel

/**
 * WebSocket 수신 메시지 표 모델. REST 탭의 [RecordedTableModel] 과 같은 골격이되, 컬럼은 단일("수신 메시지")이다.
 * 셀 값은 payload 를 한 줄로 접은 미리보기로, 행 선택 시 원문([getMessageAt])을 입력란에 복사한다.
 */
class ReceivedTableModel : AbstractTableModel() {

    private val columns = arrayOf("수신 메시지")
    private var rows: List<ReceivedMessage> = emptyList()

    fun update(newRows: List<ReceivedMessage>) {
        rows = newRows
        fireTableDataChanged()
    }

    fun getMessageAt(row: Int): ReceivedMessage = rows[row]

    /** 현재 행들의 seq 스냅샷 — 폴링 시 내용 변화 여부 판단·선택 복원에 쓴다. */
    fun seqs(): List<Long> = rows.map { it.seq }

    override fun getRowCount(): Int = rows.size

    override fun getColumnCount(): Int = columns.size

    override fun getColumnName(column: Int): String = columns[column]

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any =
        rows[rowIndex].payload.replace(WHITESPACE, " ").trim()

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
