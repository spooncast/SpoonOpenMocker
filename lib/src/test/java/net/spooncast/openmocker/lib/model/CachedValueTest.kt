package net.spooncast.openmocker.lib.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CachedValueTest {

    private val response = CachedResponse(code = 200, body = "original", duration = 100L)

    @Nested
    @DisplayName("mock 이 없을 때")
    inner class WithoutMock {
        private val value = CachedValue(response = response)

        @Test
        fun `code 는 response 값을 반환한다`() {
            assertEquals(200, value.code)
        }

        @Test
        fun `body 는 response 값을 반환한다`() {
            assertEquals("original", value.body)
        }

        @Test
        fun `duration 은 response 값을 반환한다`() {
            assertEquals(100L, value.duration)
        }

        @Test
        fun `모든 isMocked 플래그는 false 다`() {
            assertFalse(value.isCodeMocked)
            assertFalse(value.isBodyMocked)
            assertFalse(value.isDurationMocked)
        }
    }

    @Nested
    @DisplayName("mock 이 있을 때")
    inner class WithMock {

        @Test
        fun `code body duration 은 mock 값을 우선한다`() {
            val value = CachedValue(
                response = response,
                mock = CachedResponse(code = 500, body = "mocked", duration = 3000L)
            )
            assertEquals(500, value.code)
            assertEquals("mocked", value.body)
            assertEquals(3000L, value.duration)
        }

        @Test
        fun `code 가 response 와 다르면 isCodeMocked 는 true 다`() {
            val value = CachedValue(response, mock = CachedResponse(code = 404, body = "original", duration = 100L))
            assertTrue(value.isCodeMocked)
        }

        @Test
        fun `code 가 response 와 같으면 isCodeMocked 는 false 다`() {
            val value = CachedValue(response, mock = CachedResponse(code = 200, body = "x", duration = 0L))
            assertFalse(value.isCodeMocked)
        }

        @Test
        fun `body 가 response 와 다르면 isBodyMocked 는 true 다`() {
            val value = CachedValue(response, mock = CachedResponse(code = 200, body = "changed", duration = 100L))
            assertTrue(value.isBodyMocked)
        }

        @Test
        fun `body 가 response 와 같으면 isBodyMocked 는 false 다`() {
            val value = CachedValue(response, mock = CachedResponse(code = 200, body = "original", duration = 100L))
            assertFalse(value.isBodyMocked)
        }

        @Test
        fun `duration 이 response 와 다르고 0이 아니면 isDurationMocked 는 true 다`() {
            val value = CachedValue(response, mock = CachedResponse(code = 200, body = "original", duration = 5000L))
            assertTrue(value.isDurationMocked)
        }

        @Test
        fun `duration 이 0이면 isDurationMocked 는 false 다`() {
            val value = CachedValue(response, mock = CachedResponse(code = 200, body = "original", duration = 0L))
            assertFalse(value.isDurationMocked)
        }

        @Test
        fun `duration 이 response 와 같으면 isDurationMocked 는 false 다`() {
            val value = CachedValue(response, mock = CachedResponse(code = 200, body = "original", duration = 100L))
            assertFalse(value.isDurationMocked)
        }
    }
}
