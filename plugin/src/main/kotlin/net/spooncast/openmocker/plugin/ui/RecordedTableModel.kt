package net.spooncast.openmocker.plugin.ui

import net.spooncast.openmocker.plugin.net.RecordedEntry
import javax.swing.table.AbstractTableModel

class RecordedTableModel : AbstractTableModel() {

    private val columns = arrayOf("Method", "Path", "Mocked")
    private var entries: List<RecordedEntry> = emptyList()

    fun update(newEntries: List<RecordedEntry>) {
        entries = newEntries
        fireTableDataChanged()
    }

    fun getEntryAt(row: Int): RecordedEntry = entries[row]

    /** method+path(기록 항목의 고유 키)로 행 인덱스를 찾는다. 없으면 -1. */
    fun indexOfEntry(method: String, path: String): Int =
        entries.indexOfFirst { it.method == method && it.path == path }

    override fun getRowCount(): Int = entries.size

    override fun getColumnCount(): Int = columns.size

    override fun getColumnName(column: Int): String = columns[column]

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val entry = entries[rowIndex]
        return when (columnIndex) {
            0 -> entry.method
            1 -> entry.path
            2 -> entry.mock != null
            else -> ""
        }
    }

    override fun getColumnClass(columnIndex: Int): Class<*> =
        if (columnIndex == 2) Boolean::class.javaObjectType else String::class.java
}
