package net.spooncast.openmocker.plugin.ui

import net.spooncast.openmocker.plugin.net.MockData
import net.spooncast.openmocker.plugin.net.RecordedEntry
import net.spooncast.openmocker.plugin.net.ResponseData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecordedTableModelTest {

    private lateinit var model: RecordedTableModel

    @Before
    fun setUp() {
        model = RecordedTableModel()
    }

    @Test
    fun `초기 상태는 rowCount가 0`() {
        assertEquals(0, model.rowCount)
    }

    @Test
    fun `update 후 rowCount는 entries 수와 일치`() {
        model.update(listOf(entry("GET", "/a"), entry("POST", "/b")))
        assertEquals(2, model.rowCount)
    }

    @Test
    fun `update 재호출 시 이전 데이터를 교체`() {
        model.update(listOf(entry("GET", "/a"), entry("POST", "/b")))
        model.update(listOf(entry("DELETE", "/c")))
        assertEquals(1, model.rowCount)
        assertEquals("DELETE", model.getValueAt(0, 0))
    }

    @Test
    fun `Method 컬럼 값이 entry method 와 일치`() {
        model.update(listOf(entry("PUT", "/x")))
        assertEquals("PUT", model.getValueAt(0, 0))
    }

    @Test
    fun `Path 컬럼 값이 entry path 와 일치`() {
        model.update(listOf(entry("GET", "/weather")))
        assertEquals("/weather", model.getValueAt(0, 1))
    }

    @Test
    fun `mock 이 null 이면 Mocked 컬럼이 false`() {
        model.update(listOf(entry("GET", "/a", mock = null)))
        assertFalse(model.getValueAt(0, 2) as Boolean)
    }

    @Test
    fun `mock 이 있으면 Mocked 컬럼이 true`() {
        val withMock = entry("GET", "/a", mock = MockData(code = 500, body = "err", duration = 0L))
        model.update(listOf(withMock))
        assertTrue(model.getValueAt(0, 2) as Boolean)
    }

    @Test
    fun `getEntryAt 은 해당 인덱스의 RecordedEntry 를 반환`() {
        val e = entry("GET", "/check")
        model.update(listOf(e))
        assertEquals(e, model.getEntryAt(0))
    }

    @Test
    fun `컬럼 수는 3`() {
        assertEquals(3, model.columnCount)
    }

    @Test
    fun `컬럼 이름이 Method Path Mocked 순`() {
        assertEquals("Method", model.getColumnName(0))
        assertEquals("Path", model.getColumnName(1))
        assertEquals("Mocked", model.getColumnName(2))
    }

    private fun entry(method: String, path: String, mock: MockData? = null) =
        RecordedEntry(
            method = method,
            path = path,
            response = ResponseData(code = 200, body = "ok"),
            mock = mock,
        )
}
