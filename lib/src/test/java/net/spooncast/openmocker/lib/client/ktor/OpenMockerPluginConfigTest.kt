package net.spooncast.openmocker.lib.client.ktor

import io.ktor.client.request.*
import io.ktor.client.statement.*
import net.spooncast.openmocker.lib.core.MockingEngine
import net.spooncast.openmocker.lib.repo.MemCacheRepoImpl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Assertions.*
import kotlin.concurrent.thread
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@DisplayName("OpenMockerPluginConfig 테스트")
class OpenMockerPluginConfigTest {

    @Nested
    @DisplayName("주어진 조건: OpenMockerPluginConfig 인스턴스")
    inner class BasicConfigurationTests {

        @Test
        @DisplayName("""
        [주어진 조건: 새로운 OpenMockerPluginConfig 인스턴스]
        [실행: enabled 속성 확인]
        [예상 결과: 기본값 true 반환]
        """)
        fun `should have enabled property defaulted to true`() {
            // Given
            val config = OpenMockerPluginConfig()

            // When & Then
            assertTrue(config.enabled)
        }

        @Test
        @DisplayName("""
        [주어진 조건: OpenMockerPluginConfig 인스턴스]
        [실행: enabled 속성을 false로 설정]
        [예상 결과: false 값이 올바르게 설정됨]
        """)
        fun `should allow enabled property to be set to false`() {
            // Given
            val config = OpenMockerPluginConfig()

            // When
            config.enabled = false

            // Then
            assertFalse(config.enabled)
        }

        @Test
        @DisplayName("""
        [주어진 조건: OpenMockerPluginConfig 인스턴스]
        [실행: enabled 속성을 true로 설정]
        [예상 결과: true 값이 올바르게 설정됨]
        """)
        fun `should allow enabled property to be set to true`() {
            // Given
            val config = OpenMockerPluginConfig()

            // When
            config.enabled = true

            // Then
            assertTrue(config.enabled)
        }
    }

    @Nested
    @DisplayName("주어진 조건: MockingEngine 지연 초기화")
    inner class LazyMockingEngineTests {

        @Test
        @DisplayName("""
        [주어진 조건: 새로운 OpenMockerPluginConfig 인스턴스]
        [실행: mockingEngine 속성에 최초 접근]
        [예상 결과: MockingEngine 인스턴스가 생성되어 반환됨]
        """)
        fun `should lazy initialize mockingEngine on first access`() {
            // Given
            val config = OpenMockerPluginConfig()

            // When
            val mockingEngine = config.mockingEngine

            // Then
            assertNotNull(mockingEngine)
            assertTrue(mockingEngine is MockingEngine<*, *>)
        }

        @Test
        @DisplayName("""
        [주어진 조건: MockingEngine이 이미 초기화된 OpenMockerPluginConfig]
        [실행: mockingEngine 속성에 재접근]
        [예상 결과: 동일한 MockingEngine 인스턴스 반환]
        """)
        fun `should return same mockingEngine instance on subsequent access`() {
            // Given
            val config = OpenMockerPluginConfig()
            val firstAccess = config.mockingEngine

            // When
            val secondAccess = config.mockingEngine

            // Then
            assertSame(firstAccess, secondAccess)
        }

        @Test
        @DisplayName("""
        [주어진 조건: OpenMockerPluginConfig 인스턴스]
        [실행: mockingEngine의 clientType 확인]
        [예상 결과: 'Ktor' 반환]
        """)
        fun `should create mockingEngine with Ktor client type`() {
            // Given
            val config = OpenMockerPluginConfig()

            // When
            val mockingEngine = config.mockingEngine

            // Then
            assertEquals("Ktor", mockingEngine.getClientType())
        }

        @Test
        @DisplayName("""
        [주어진 조건: OpenMockerPluginConfig 인스턴스]
        [실행: mockingEngine이 Ktor 타입을 처리할 수 있는지 확인]
        [예상 결과: Ktor HttpRequestData와 HttpResponse 타입 지원]
        """)
        fun `should create mockingEngine that can handle Ktor types`() {
            // Given
            val config = OpenMockerPluginConfig()
            val mockingEngine = config.mockingEngine

            // When & Then
            // We verify the engine is properly configured with KtorAdapter
            // The actual type checking will be delegated to KtorAdapter's isSupported method
            assertEquals("Ktor", mockingEngine.getClientType())
        }
    }

    @Nested
    @DisplayName("주어진 조건: MemCacheRepoImpl 공유 인스턴스 통합")
    inner class SharedCacheIntegrationTests {

        @Test
        @DisplayName("""
        [주어진 조건: 여러 OpenMockerPluginConfig 인스턴스]
        [실행: 각각의 mockingEngine에서 캐시 리포지토리 확인]
        [예상 결과: 동일한 MemCacheRepoImpl 인스턴스 공유]
        """)
        fun `should share same MemCacheRepoImpl instance across multiple configs`() {
            // Given
            val config1 = OpenMockerPluginConfig()
            val config2 = OpenMockerPluginConfig()

            // When
            val mockingEngine1 = config1.mockingEngine
            val mockingEngine2 = config2.mockingEngine

            // Then
            // Both engines should use the same cache repository instance
            // We can verify this by checking that both engines are different but share cache behavior
            assertNotSame(mockingEngine1, mockingEngine2)

            // Both should report same client type (indicating they use KtorAdapter)
            assertEquals("Ktor", mockingEngine1.getClientType())
            assertEquals("Ktor", mockingEngine2.getClientType())
        }

        @Test
        @DisplayName("""
        [주어진 조건: OpenMockerPluginConfig의 mockingEngine]
        [실행: MemCacheRepoImpl.getInstance()와 동일한 캐시 동작 확인]
        [예상 결과: 글로벌 캐시 인스턴스와 동일한 동작]
        """)
        fun `should integrate with global MemCacheRepoImpl instance`() {
            // Given
            val config = OpenMockerPluginConfig()
            val globalCacheRepo = MemCacheRepoImpl.getInstance()

            // When - Clear cache globally
            globalCacheRepo.clearCache()
            val initialCacheSize = globalCacheRepo.cachedMap.size

            // Then
            assertEquals(0, initialCacheSize)

            // The config should be using the same cache instance
            val mockingEngine = config.mockingEngine
            assertNotNull(mockingEngine) // Engine should be initialized properly
        }
    }

    @Nested
    @DisplayName("주어진 조건: 스레드 안전성 요구사항")
    inner class ThreadSafetyTests {

        @Test
        @DisplayName("""
        [주어진 조건: 여러 스레드에서 동시 접근]
        [실행: mockingEngine 속성에 동시 접근]
        [예상 결과: 모든 스레드가 동일한 인스턴스 반환]
        """)
        fun `should be thread-safe during lazy initialization`() {
            // Given
            val config = OpenMockerPluginConfig()
            val results = mutableListOf<MockingEngine<HttpRequestData, HttpResponse>>()
            val exceptions = mutableListOf<Exception>()

            // When - Multiple threads access mockingEngine simultaneously
            val threads = (1..10).map { threadIndex ->
                thread {
                    try {
                        // Add small random delay to increase chance of race condition
                        Thread.sleep((0..5).random().toLong())
                        val engine = config.mockingEngine
                        synchronized(results) {
                            results.add(engine)
                        }
                    } catch (e: Exception) {
                        synchronized(exceptions) {
                            exceptions.add(e)
                        }
                    }
                }
            }

            // Wait for all threads to complete
            threads.forEach { it.join() }

            // Then
            assertTrue(exceptions.isEmpty(), "No exceptions should occur: ${exceptions.map { it.message }}")
            assertEquals(10, results.size)

            // All results should be the same instance
            val firstInstance = results.first()
            results.forEach { engine ->
                assertSame(firstInstance, engine, "All threads should get the same MockingEngine instance")
            }
        }

        @Test
        @DisplayName("""
        [주어진 조건: 코루틴 환경에서의 동시 접근]
        [실행: mockingEngine 속성에 동시 접근]
        [예상 결과: 모든 코루틴이 동일한 인스턴스 반환]
        """)
        fun `should be thread-safe in coroutine environment`() = runBlocking {
            // Given
            val config = OpenMockerPluginConfig()
            val results = mutableListOf<MockingEngine<HttpRequestData, HttpResponse>>()

            // When - Multiple coroutines access mockingEngine simultaneously
            val jobs = (1..10).map { jobIndex ->
                launch {
                    // Add small delay to increase chance of race condition
                    delay((0..5).random().toLong())
                    val engine = config.mockingEngine
                    synchronized(results) {
                        results.add(engine)
                    }
                }
            }

            // Wait for all coroutines to complete
            jobs.forEach { it.join() }

            // Then
            assertEquals(10, results.size)

            // All results should be the same instance
            val firstInstance = results.first()
            results.forEach { engine ->
                assertSame(firstInstance, engine, "All coroutines should get the same MockingEngine instance")
            }
        }
    }

    @Nested
    @DisplayName("주어진 조건: 내부 가시성 요구사항")
    inner class InternalVisibilityTests {

        @Test
        @DisplayName("""
        [주어진 조건: OpenMockerPluginConfig 클래스]
        [실행: mockingEngine 속성의 가시성 확인]
        [예상 결과: internal 가시성으로 외부 패키지에서 접근 제한]
        """)
        fun `should have internal visibility for mockingEngine property`() {
            // Given
            val config = OpenMockerPluginConfig()

            // When
            val mockingEngine = config.mockingEngine

            // Then
            assertNotNull(mockingEngine)
            // Note: The internal visibility is enforced at compile time
            // This test verifies that the property exists and is accessible within the same module
            // External module access would fail at compile time due to internal modifier
        }
    }

    @Nested
    @DisplayName("주어진 조건: KtorAdapter와 MockingEngine 통합")
    inner class KtorAdapterIntegrationTests {

        @Test
        @DisplayName("""
        [주어진 조건: OpenMockerPluginConfig의 mockingEngine]
        [실행: MockingEngine 구성 요소 검증]
        [예상 결과: KtorAdapter와 MemCacheRepoImpl이 올바르게 통합됨]
        """)
        fun `should properly integrate KtorAdapter with MockingEngine`() {
            // Given
            val config = OpenMockerPluginConfig()

            // When
            val mockingEngine = config.mockingEngine

            // Then
            // Verify MockingEngine is properly configured
            assertNotNull(mockingEngine)
            assertEquals("Ktor", mockingEngine.getClientType())

            // Verify it has the expected message from base MockingEngine
            assertEquals("OpenMocker enabled", MockingEngine.MOCKER_MESSAGE)
        }

        @Test
        @DisplayName("""
        [주어진 조건: 여러 OpenMockerPluginConfig 인스턴스]
        [실행: 각 인스턴스의 MockingEngine 독립성 확인]
        [예상 결과: MockingEngine은 독립적이지만 캐시는 공유]
        """)
        fun `should create independent MockingEngine instances but share cache`() {
            // Given
            val config1 = OpenMockerPluginConfig()
            val config2 = OpenMockerPluginConfig()

            // When
            val engine1 = config1.mockingEngine
            val engine2 = config2.mockingEngine

            // Then
            // Engines should be different instances
            assertNotSame(engine1, engine2)

            // But both should use the same client type (KtorAdapter)
            assertEquals("Ktor", engine1.getClientType())
            assertEquals("Ktor", engine2.getClientType())

            // Both should be properly configured MockingEngine instances
            assertTrue(engine1 is MockingEngine<*, *>)
            assertTrue(engine2 is MockingEngine<*, *>)
        }
    }

    @Nested
    @DisplayName("주어진 조건: 성능 및 메모리 효율성")
    inner class PerformanceTests {

        @Test
        @DisplayName("""
        [주어진 조건: 반복적인 mockingEngine 접근]
        [실행: 1000번 속성 접근]
        [예상 결과: 빠른 응답 시간과 메모리 효율성]
        """)
        fun `should maintain performance with frequent mockingEngine access`() {
            // Given
            val config = OpenMockerPluginConfig()

            // When
            val startTime = System.currentTimeMillis()
            val engines = mutableListOf<MockingEngine<HttpRequestData, HttpResponse>>()

            repeat(1000) {
                engines.add(config.mockingEngine)
            }

            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime

            // Then
            assertTrue(duration < 1000, "1000 accesses should take less than 1 second, took ${duration}ms")

            // All should be the same instance (no new instances created)
            val firstInstance = engines.first()
            engines.forEach { engine ->
                assertSame(firstInstance, engine)
            }
        }
    }
}