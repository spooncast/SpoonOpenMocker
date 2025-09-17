package net.spooncast.openmocker.core.model

import org.junit.Test
import org.junit.Assert.*

/**
 * MockKey [HTTP 요청 식별 키 클래스] - BDD 스타일 종합 테스트
 * 100% 코드 커버리지와 모든 엣지 케이스를 다루는 포괄적 테스트 스위트
 */
class MockKeyTest {

    // ================================
    // MockKey 생성 테스트
    // ================================

        @Test
        fun `GIVEN valid method and path WHEN creating MockKey THEN creation succeeds`() {
            // Given
            val method = "GET"
            val path = "/api/users"

            // When
            val mockKey = MockKey(method, path)

            // Then
            assertEquals("GET", mockKey.method)
            assertEquals("/api/users", mockKey.path)
        }

        @Test
        fun `GIVEN various valid HTTP methods WHEN creating MockKey THEN all methods succeed`() {
            // Given
            val validMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS")
            val path = "/api/test"

            // When & Then
            validMethods.forEach { method ->
                val mockKey = MockKey(method, path)
                assertEquals(method, mockKey.method)
                assertEquals(path, mockKey.path)
            }
        }

        @Test
        fun `GIVEN complex path patterns WHEN creating MockKey THEN creation succeeds`() {
            // Given
            val complexPaths = listOf(
                "/api/v1/users/123",
                "/api/users/123/posts/456",
                "/search?q=test&sort=date",
                "/api/users?page=1&limit=10",
                "/api/users/{id}/posts/{postId}",
                "/api/v2/deeply/nested/resource/path"
            )

            // When & Then
            complexPaths.forEach { path ->
                val mockKey = MockKey("GET", path)
                assertEquals("GET", mockKey.method)
                assertEquals(path, mockKey.path)
            }
        }
    // ================================
    // MockKey 검증 테스트
    // ================================

        @Test(expected = IllegalArgumentException::class)
        fun `GIVEN empty method WHEN creating MockKey THEN throws IllegalArgumentException`() {
            MockKey("", "/api/users")
        }

        @Test(expected = IllegalArgumentException::class)
        fun `GIVEN whitespace-only method WHEN creating MockKey THEN throws IllegalArgumentException`() {
            MockKey("   ", "/api/users")
        }

        @Test(expected = IllegalArgumentException::class)
        fun `GIVEN empty path WHEN creating MockKey THEN throws IllegalArgumentException`() {
            MockKey("GET", "")
        }

        @Test(expected = IllegalArgumentException::class)
        fun `GIVEN whitespace-only path WHEN creating MockKey THEN throws IllegalArgumentException`() {
            MockKey("GET", "   ")
        }

        @Test(expected = IllegalArgumentException::class)
        fun `GIVEN lowercase method WHEN creating MockKey THEN throws IllegalArgumentException`() {
            MockKey("get", "/api/users")
        }

        @Test(expected = IllegalArgumentException::class)
        fun `GIVEN mixed case method WHEN creating MockKey THEN throws IllegalArgumentException`() {
            MockKey("Get", "/api/users")
        }

        @Test(expected = IllegalArgumentException::class)
        fun `GIVEN path not starting with slash WHEN creating MockKey THEN throws IllegalArgumentException`() {
            MockKey("GET", "api/users")
        }

        @Test(expected = IllegalArgumentException::class)
        fun `GIVEN method with numbers WHEN creating MockKey THEN throws IllegalArgumentException`() {
            MockKey("GET123", "/api/users")
        }

        @Test(expected = IllegalArgumentException::class)
        fun `GIVEN method with special characters WHEN creating MockKey THEN throws IllegalArgumentException`() {
            MockKey("GET-POST", "/api/users")
        }

        @Test(expected = IllegalArgumentException::class)
        fun `GIVEN method with spaces WHEN creating MockKey THEN throws IllegalArgumentException`() {
            MockKey("GET POST", "/api/users")
        }
    // ================================
    // MockKey 동등성 테스트
    // ================================

        @Test
        fun `GIVEN identical MockKeys WHEN comparing equality THEN returns true`() {
            // Given
            val key1 = MockKey("GET", "/api/users")
            val key2 = MockKey("GET", "/api/users")

            // When & Then
            assertEquals(key1, key2)
            assertEquals(key2, key1) // 대칭성
            assertTrue(key1 == key2)
        }

        @Test
        fun `GIVEN MockKeys with different methods WHEN comparing equality THEN returns false`() {
            // Given
            val key1 = MockKey("GET", "/api/users")
            val key2 = MockKey("POST", "/api/users")

            // When & Then
            assertNotEquals(key1, key2)
            assertNotEquals(key2, key1)
            assertFalse(key1 == key2)
        }

        @Test
        fun `GIVEN MockKeys with different paths WHEN comparing equality THEN returns false`() {
            // Given
            val key1 = MockKey("GET", "/api/users")
            val key2 = MockKey("GET", "/api/posts")

            // When & Then
            assertNotEquals(key1, key2)
            assertNotEquals(key2, key1)
            assertFalse(key1 == key2)
        }

        @Test
        fun `GIVEN completely different MockKeys WHEN comparing equality THEN returns false`() {
            // Given
            val key1 = MockKey("GET", "/api/users")
            val key2 = MockKey("POST", "/api/posts")

            // When & Then
            assertNotEquals(key1, key2)
            assertNotEquals(key2, key1)
            assertFalse(key1 == key2)
        }

        @Test
        fun `GIVEN MockKey and null WHEN comparing equality THEN returns false`() {
            // Given
            val key = MockKey("GET", "/api/users")

            // When & Then
            assertNotEquals(key, null)
            assertFalse(key.equals(null))
        }

        @Test
        fun `GIVEN MockKey and different type object WHEN comparing equality THEN returns false`() {
            // Given
            val key = MockKey("GET", "/api/users")
            val differentObject = "GET /api/users"

            // When & Then
            assertNotEquals(key, differentObject)
            assertFalse(key.equals(differentObject))
        }

        @Test
        fun `GIVEN MockKey WHEN comparing with itself THEN returns true`() {
            // Given
            val key = MockKey("GET", "/api/users")

            // When & Then
            assertEquals(key, key)
            assertTrue(key == key)
        }
    // ================================
    // MockKey hashCode 테스트
    // ================================

        @Test
        fun `GIVEN identical MockKeys WHEN comparing hashCode THEN returns same hashCode`() {
            // Given
            val key1 = MockKey("GET", "/api/users")
            val key2 = MockKey("GET", "/api/users")

            // When & Then
            assertEquals(key1.hashCode(), key2.hashCode())
        }

        @Test
        fun `GIVEN different MockKeys WHEN comparing hashCode THEN likely returns different hashCode`() {
            // Given
            val key1 = MockKey("GET", "/api/users")
            val key2 = MockKey("POST", "/api/users")
            val key3 = MockKey("GET", "/api/posts")

            // When & Then
            assertNotEquals(key1.hashCode(), key2.hashCode())
            assertNotEquals(key1.hashCode(), key3.hashCode())
            assertNotEquals(key2.hashCode(), key3.hashCode())
        }

        @Test
        fun `GIVEN MockKey WHEN calling hashCode multiple times THEN returns consistent value`() {
            // Given
            val key = MockKey("GET", "/api/users")

            // When
            val hashCode1 = key.hashCode()
            val hashCode2 = key.hashCode()
            val hashCode3 = key.hashCode()

            // Then
            assertEquals(hashCode1, hashCode2)
            assertEquals(hashCode1, hashCode3)
            assertEquals(hashCode2, hashCode3)
        }
    // ================================
    // MockKey toString 테스트
    // ================================

        @Test
        fun `GIVEN MockKey WHEN calling toString THEN returns correct formatted string`() {
            // Given
            val mockKey = MockKey("POST", "/api/posts")

            // When
            val result = mockKey.toString()

            // Then
            assertEquals("POST /api/posts", result)
        }

        @Test
        fun `GIVEN various MockKeys WHEN calling toString THEN returns correct format for each`() {
            // Given
            val testCases = listOf(
                Pair(MockKey("GET", "/"), "GET /"),
                Pair(MockKey("POST", "/api/v1/users"), "POST /api/v1/users"),
                Pair(MockKey("DELETE", "/api/users/123"), "DELETE /api/users/123"),
                Pair(MockKey("PATCH", "/api/users?filter=active"), "PATCH /api/users?filter=active")
            )

            // When & Then
            testCases.forEach { (mockKey, expected) ->
                assertEquals(expected, mockKey.toString())
            }
        }

        @Test
        fun `GIVEN MockKey with long path WHEN calling toString THEN returns string with full path`() {
            // Given
            val longPath = "/api/v2/organizations/123/projects/456/issues/789/comments/abc"
            val mockKey = MockKey("GET", longPath)

            // When
            val result = mockKey.toString()

            // Then
            assertEquals("GET $longPath", result)
            assertTrue(result.contains(longPath))
        }
    // ================================
    // MockKey 경계값 테스트
    // ================================

        @Test
        fun `GIVEN minimum length path WHEN creating MockKey THEN creation succeeds`() {
            // Given
            val minPath = "/"

            // When
            val mockKey = MockKey("GET", minPath)

            // Then
            assertEquals("GET", mockKey.method)
            assertEquals("/", mockKey.path)
        }

        @Test
        fun `GIVEN single character method WHEN creating MockKey THEN creation succeeds`() {
            // Given
            val singleCharMethod = "A"

            // When
            val mockKey = MockKey(singleCharMethod, "/api")

            // Then
            assertEquals("A", mockKey.method)
            assertEquals("/api", mockKey.path)
        }

        @Test
        fun `GIVEN very long method WHEN creating MockKey THEN creation succeeds`() {
            // Given
            val longMethod = "VERYLONGMETHODNAME"

            // When
            val mockKey = MockKey(longMethod, "/api")

            // Then
            assertEquals(longMethod, mockKey.method)
            assertEquals("/api", mockKey.path)
        }
}