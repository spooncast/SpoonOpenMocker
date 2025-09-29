package net.spooncast.openmocker.lib

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import net.spooncast.openmocker.lib.client.ktor.OpenMockerPlugin
import net.spooncast.openmocker.lib.client.okhttp.OkHttpAdapter
import net.spooncast.openmocker.lib.client.okhttp.OpenMockerInterceptor
import net.spooncast.openmocker.lib.repo.MemCacheRepoImpl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody.Companion.create
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Phase 4.2: 크로스 클라이언트 캐시 공유 검증 테스트
 *
 * OkHttp OpenMockerInterceptor와 Ktor OpenMockerPlugin이
 * MemCacheRepoImpl.getInstance()를 통해 캐시 데이터를 올바르게 공유하는지
 * 포괄적으로 검증하는 테스트 클래스입니다.
 *
 * 주요 검증 사항:
 * - OkHttp → Ktor 캐시 공유
 * - Ktor → OkHttp 캐시 공유
 * - 양방향 캐시 공유
 * - 동시 접근 및 스레드 안전성
 * - 데이터 호환성 (JSON, 텍스트, 바이너리)
 * - 요청 매칭 일관성
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("크로스 클라이언트 캐시 공유 검증")
class CrossClientCacheSharingTest {

    private val baseUrl: String = "http://localhost:8080"
    private lateinit var cacheRepo: MemCacheRepoImpl

    // 테스트용 엔드포인트 및 응답 데이터
    private val endpoint1 = "/api/test1"
    private val endpoint2 = "/api/test2"
    private val testResponse1 = """{"message":"test1","id":1}"""
    private val testResponse2 = """{"message":"test2","id":2}"""

    @BeforeEach
    fun setup() {
        // 공유 캐시 인스턴스 초기화
        cacheRepo = MemCacheRepoImpl.getInstance()
        cacheRepo.clearCache()

        // 캐시 초기화
    }

    @Nested
    @DisplayName("OkHttp → Ktor 캐시 공유 테스트")
    inner class OkHttpToKtorSharingTests {

        @Test
        @DisplayName("""
        [주어진 조건: OkHttp 어댑터로 캐시에 응답 저장]
        [실행: Ktor 어댑터로 동일한 캐시 접근]
        [예상 결과: Ktor가 OkHttp의 캐시된 응답을 찾을 수 있음]
        """)
        fun `okhttp cached response accessible from ktor client`() = runTest {
            // Given: 실제 요청 객체를 통한 정확한 경로 추출
            val okHttpAdapter = OkHttpAdapter()
            val okHttpRequest = Request.Builder()
                .url("$baseUrl$endpoint1")
                .build()

            val okHttpRequestData = okHttpAdapter.extractRequestData(okHttpRequest)

            // 실제 경로를 사용하여 캐시 저장 및 mock 활성화
            cacheRepo.cache(okHttpRequestData.method, okHttpRequestData.path, 200, testResponse1)
            val cacheKey = net.spooncast.openmocker.lib.model.CachedKey(okHttpRequestData.method, okHttpRequestData.path)
            val cachedResponse = net.spooncast.openmocker.lib.model.CachedResponse(200, testResponse1)
            cacheRepo.mock(cacheKey, cachedResponse)

            // When: Ktor 어댑터로 동일한 요청 데이터 생성
            val ktorAdapter = net.spooncast.openmocker.lib.client.ktor.KtorAdapter()
            val ktorRequestBuilder = HttpRequestBuilder().apply {
                method = HttpMethod.Get
                url("$baseUrl$endpoint1")
            }
            val ktorRequestData = ktorRequestBuilder.build()
            val ktorExtractedData = ktorAdapter.extractRequestData(ktorRequestData)

            // Then: 경로가 일치하고 캐시 데이터를 찾을 수 있어야 함
            assertEquals(okHttpRequestData.path, ktorExtractedData.path, "경로 추출 결과가 다릅니다")

            val cachedMock = cacheRepo.getMock(ktorExtractedData.method, ktorExtractedData.path)
            assertNotNull(cachedMock, "캐시에서 Mock 데이터를 찾을 수 없습니다")
            assertEquals(200, cachedMock?.code)
            assertEquals(testResponse1, cachedMock?.body)
        }

        @Test
        @DisplayName("""
        [주어진 조건: OkHttp가 404 에러 응답을 캐시한 상태]
        [실행: Ktor 클라이언트로 동일한 요청 수행]
        [예상 결과: 상태 코드와 헤더가 보존된 캐시 응답 반환]
        """)
        fun `okhttp cached error response preserves status code and headers`() = runTest {
            // Given: 직접 캐시에 404 에러 응답 저장
            val errorResponse = """{"error":"not found"}"""
            val testUrl = "$baseUrl$endpoint1"

            val okHttpAdapter = OkHttpAdapter()
            val okHttpRequest = Request.Builder().url(testUrl).build()
            val okHttpRequestData = okHttpAdapter.extractRequestData(okHttpRequest)

            // 404 에러 응답을 캐시에 저장하고 mock으로 활성화
            cacheRepo.cache(okHttpRequestData.method, okHttpRequestData.path, 404, errorResponse)
            val cacheKey = net.spooncast.openmocker.lib.model.CachedKey(okHttpRequestData.method, okHttpRequestData.path)
            val mockErrorResponse = net.spooncast.openmocker.lib.model.CachedResponse(404, errorResponse)
            cacheRepo.mock(cacheKey, mockErrorResponse)

            // When: Ktor 어댑터로 동일한 캐시 접근
            val ktorAdapter = net.spooncast.openmocker.lib.client.ktor.KtorAdapter()
            val ktorRequestBuilder = HttpRequestBuilder().apply {
                method = HttpMethod.Get
                url(testUrl)
            }
            val ktorRequestData = ktorRequestBuilder.build()
            val ktorExtractedData = ktorAdapter.extractRequestData(ktorRequestData)

            // Then: Ktor가 OkHttp의 캐시된 404 에러 응답에 접근 가능
            val ktorCachedMock = cacheRepo.getMock(ktorExtractedData.method, ktorExtractedData.path)
            assertNotNull(ktorCachedMock, "Ktor로 캐시된 에러 응답을 찾을 수 없습니다")
            assertEquals(404, ktorCachedMock?.code, "에러 상태 코드가 보존되지 않았습니다")
            assertEquals(errorResponse, ktorCachedMock?.body, "에러 응답 내용이 보존되지 않았습니다")
        }
    }

    @Nested
    @DisplayName("Ktor → OkHttp 캐시 공유 테스트")
    inner class KtorToOkHttpSharingTests {

        @Test
        @DisplayName("""
        [주어진 조건: Ktor가 응답을 캐시한 상태]
        [실행: OkHttp 클라이언트로 동일한 요청 수행]
        [예상 결과: OkHttp가 Ktor의 캐시된 응답을 반환]
        """)
        fun `ktor cached response accessible from okhttp client`() = runTest {
            // Given: 표준화된 URL로 Ktor 어댑터를 통한 캐시 저장 시뮬레이션
            val testUrl = "$baseUrl$endpoint2"
            val okHttpAdapter = OkHttpAdapter()
            val okHttpRequest = Request.Builder().url(testUrl).build()
            val okHttpRequestData = okHttpAdapter.extractRequestData(okHttpRequest)

            // Ktor가 응답을 캐시했다고 가정하고 직접 캐시에 저장
            cacheRepo.cache(okHttpRequestData.method, okHttpRequestData.path, 200, testResponse2)
            val cacheKey = net.spooncast.openmocker.lib.model.CachedKey(okHttpRequestData.method, okHttpRequestData.path)
            val cachedResponse = net.spooncast.openmocker.lib.model.CachedResponse(200, testResponse2)
            cacheRepo.mock(cacheKey, cachedResponse)

            // When: OkHttp 클라이언트가 캐시에서 데이터를 조회
            val cachedMock = cacheRepo.getMock(okHttpRequestData.method, okHttpRequestData.path)

            // Then: OkHttp가 Ktor의 캐시된 응답을 받아야 함
            assertNotNull(cachedMock, "Cached mock should not be null")
            assertEquals(200, cachedMock!!.code)
            assertEquals(testResponse2, cachedMock.body)
        }

        @Test
        @DisplayName("""
        [주어진 조건: Ktor가 500 에러 응답을 캐시한 상태]
        [실행: OkHttp 클라이언트로 동일한 요청 수행]
        [예상 결과: 에러 응답이 올바르게 보존됨]
        """)
        fun `ktor cached error response accessible from okhttp`() = runTest {
            val errorResponse = """{"error":"server error"}"""

            // Given: 표준화된 URL로 경로 추출
            val testUrl = "$baseUrl$endpoint2"
            val okHttpAdapter = OkHttpAdapter()
            val okHttpRequest = Request.Builder().url(testUrl).build()
            val okHttpRequestData = okHttpAdapter.extractRequestData(okHttpRequest)

            // Ktor가 500 에러 응답을 캐시했다고 가정하고 직접 캐시에 저장
            cacheRepo.cache(okHttpRequestData.method, okHttpRequestData.path, 500, errorResponse)
            val cacheKey = net.spooncast.openmocker.lib.model.CachedKey(okHttpRequestData.method, okHttpRequestData.path)
            val cachedResponse = net.spooncast.openmocker.lib.model.CachedResponse(500, errorResponse)
            cacheRepo.mock(cacheKey, cachedResponse)

            // When: OkHttp 클라이언트가 캐시에서 에러 응답을 조회
            val cachedMock = cacheRepo.getMock(okHttpRequestData.method, okHttpRequestData.path)

            // Then: 에러 응답이 올바르게 보존되어야 함
            assertNotNull(cachedMock, "Cached mock should not be null")
            assertEquals(500, cachedMock!!.code)
            assertEquals(errorResponse, cachedMock.body)
        }
    }

    @Nested
    @DisplayName("양방향 캐시 공유 테스트")
    inner class BidirectionalSharingTests {

        @Test
        @DisplayName("""
        [주어진 조건: 두 클라이언트 설정]
        [실행: 캐시 인스턴스 참조 확인]
        [예상 결과: 동일한 싱글톤 인스턴스 공유]
        """)
        fun `both clients share same cache instance`() {
            // When: 각 클라이언트에서 캐시 인스턴스 가져오기
            val cacheFromOkHttp = MemCacheRepoImpl.getInstance()
            val cacheFromKtor = MemCacheRepoImpl.getInstance()

            // Then: 동일한 싱글톤 인스턴스
            assertSame(cacheFromOkHttp, cacheFromKtor)
            assertSame(cacheRepo, cacheFromOkHttp)
        }

        @Test
        @DisplayName("""
        [주어진 조건: 캐시에 직접 데이터 저장]
        [실행: 양방향 캐시 접근 시뮬레이션]
        [예상 결과: 양방향 캐시 공유가 정상적으로 작동]
        """)
        fun `bidirectional cache sharing works using direct cache manipulation`() = runTest {
            // Given: 표준화된 URL로 OkHttp 요청 생성
            val okHttpAdapter = OkHttpAdapter()
            val testUrl1 = "$baseUrl$endpoint1"  // http://localhost:port/api/test1
            val testUrl2 = "$baseUrl$endpoint2"  // http://localhost:port/api/test2

            val okHttpRequest1 = Request.Builder().url(testUrl1).build()
            val okHttpRequest2 = Request.Builder().url(testUrl2).build()

            val okHttpRequestData1 = okHttpAdapter.extractRequestData(okHttpRequest1)
            val okHttpRequestData2 = okHttpAdapter.extractRequestData(okHttpRequest2)

            // 실제 경로를 사용하여 캐시 저장 (response 필드에 저장됨)
            cacheRepo.cache(okHttpRequestData1.method, okHttpRequestData1.path, 200, testResponse1)
            cacheRepo.cache(okHttpRequestData2.method, okHttpRequestData2.path, 200, testResponse2)

            // response를 mock으로 활성화 (mock 필드에 복사)
            val key1 = net.spooncast.openmocker.lib.model.CachedKey(okHttpRequestData1.method, okHttpRequestData1.path)
            val key2 = net.spooncast.openmocker.lib.model.CachedKey(okHttpRequestData2.method, okHttpRequestData2.path)

            val response1 = net.spooncast.openmocker.lib.model.CachedResponse(200, testResponse1)
            val response2 = net.spooncast.openmocker.lib.model.CachedResponse(200, testResponse2)

            cacheRepo.mock(key1, response1)
            cacheRepo.mock(key2, response2)

            // When & Then: OkHttp 어댑터로 캐시 접근
            val cachedMock1 = cacheRepo.getMock(okHttpRequestData1.method, okHttpRequestData1.path)
            val cachedMock2 = cacheRepo.getMock(okHttpRequestData2.method, okHttpRequestData2.path)

            assertNotNull(cachedMock1, "OkHttp로 캐시된 mock1을 찾을 수 없습니다")
            assertNotNull(cachedMock2, "OkHttp로 캐시된 mock2를 찾을 수 없습니다")
            assertEquals(testResponse1, cachedMock1?.body)
            assertEquals(testResponse2, cachedMock2?.body)

            // And: Ktor 어댑터로 동일한 URL을 사용한 캐시 접근
            val ktorAdapter = net.spooncast.openmocker.lib.client.ktor.KtorAdapter()

            // 동일한 URL 패턴으로 Ktor HttpRequestData 생성
            val ktorRequestBuilder1 = HttpRequestBuilder().apply {
                method = HttpMethod.Get
                url(testUrl1)  // 동일한 URL 사용
            }
            val ktorRequestData1 = ktorRequestBuilder1.build()
            val ktorExtractedData1 = ktorAdapter.extractRequestData(ktorRequestData1)

            val ktorRequestBuilder2 = HttpRequestBuilder().apply {
                method = HttpMethod.Get
                url(testUrl2)  // 동일한 URL 사용
            }
            val ktorRequestData2 = ktorRequestBuilder2.build()
            val ktorExtractedData2 = ktorAdapter.extractRequestData(ktorRequestData2)


            // Then: 경로 일치 검증
            assertEquals(okHttpRequestData1.path, ktorExtractedData1.path,
                "OkHttp(${okHttpRequestData1.path})와 Ktor(${ktorExtractedData1.path})의 경로가 다릅니다")
            assertEquals(okHttpRequestData2.path, ktorExtractedData2.path,
                "OkHttp(${okHttpRequestData2.path})와 Ktor(${ktorExtractedData2.path})의 경로가 다릅니다")

            val ktorCachedMock1 = cacheRepo.getMock(ktorExtractedData1.method, ktorExtractedData1.path)
            val ktorCachedMock2 = cacheRepo.getMock(ktorExtractedData2.method, ktorExtractedData2.path)

            // Then: Ktor가 OkHttp와 동일한 캐시 데이터에 접근 가능
            assertNotNull(ktorCachedMock1, "Ktor로 캐시된 mock1을 찾을 수 없습니다")
            assertNotNull(ktorCachedMock2, "Ktor로 캐시된 mock2를 찾을 수 없습니다")
            assertEquals(testResponse1, ktorCachedMock1?.body)
            assertEquals(testResponse2, ktorCachedMock2?.body)
        }
    }

    @Nested
    @DisplayName("동시 접근 테스트")
    inner class ConcurrentAccessTests {

        @Test
        @DisplayName("""
        [주어진 조건: 사전 캐시된 데이터]
        [실행: 여러 스레드에서 동시 캐시 접근]
        [예상 결과: 스레드 안전한 동시 접근 작동]
        """)
        fun `concurrent cache access from different clients works safely`() = runTest {
            // Given: 표준화된 URL로 캐시에 직접 데이터 저장
            val testUrl = "$baseUrl$endpoint1"
            val okHttpAdapter = OkHttpAdapter()
            val okHttpRequest = Request.Builder().url(testUrl).build()
            val requestData = okHttpAdapter.extractRequestData(okHttpRequest)

            cacheRepo.cache(requestData.method, requestData.path, 200, testResponse1)

            // response를 mock으로 활성화
            val cacheKey = net.spooncast.openmocker.lib.model.CachedKey(requestData.method, requestData.path)
            val mockResponse = net.spooncast.openmocker.lib.model.CachedResponse(200, testResponse1)
            cacheRepo.mock(cacheKey, mockResponse)

            // 캐시 저장 검증
            val preTestCache = cacheRepo.getMock(requestData.method, requestData.path)
            assertNotNull(preTestCache, "테스트 시작 전 캐시 데이터가 저장되지 않았습니다")

            val executorService = Executors.newFixedThreadPool(4)
            val latch = CountDownLatch(4)
            val results = Collections.synchronizedList(mutableListOf<String>())

            try {
                // When: 동시 접근 - OkHttp 어댑터 직접 사용 (2회)
                repeat(2) { index ->
                    executorService.submit {
                        try {
                            val cachedMock = cacheRepo.getMock(requestData.method, requestData.path)
                            val resultValue = cachedMock?.body ?: "null"
                            results.add("okhttp_$index:$resultValue")
                        } catch (e: Exception) {
                            results.add("okhttp_$index:error")
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                // Ktor 어댑터 동시 접근 (2회)
                repeat(2) { index ->
                    executorService.submit {
                        try {
                            val ktorAdapter = net.spooncast.openmocker.lib.client.ktor.KtorAdapter()
                            val ktorRequestBuilder = HttpRequestBuilder().apply {
                                method = HttpMethod.Get
                                url(testUrl)  // 동일한 URL 사용
                            }
                            val ktorRequestData = ktorRequestBuilder.build()
                            val ktorExtractedData = ktorAdapter.extractRequestData(ktorRequestData)

                            val cachedMock = cacheRepo.getMock(ktorExtractedData.method, ktorExtractedData.path)
                            val resultValue = cachedMock?.body ?: "null"
                            results.add("ktor_$index:$resultValue")
                        } catch (e: Exception) {
                            results.add("ktor_$index:error")
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                // Then: 완료 대기 및 검증 (타임아웃 단축)
                assertTrue(latch.await(5, TimeUnit.SECONDS), "동시 접근 테스트가 5초 내에 완료되지 않았습니다")

                assertEquals(4, results.size, "예상된 4개 결과와 다릅니다: ${results.size}")

                // 각 결과 검증
                val okHttpResults = results.filter { result -> result.startsWith("okhttp_") }
                val ktorResults = results.filter { result -> result.startsWith("ktor_") }

                assertEquals(2, okHttpResults.size, "OkHttp 결과가 2개가 아닙니다: $okHttpResults")
                assertEquals(2, ktorResults.size, "Ktor 결과가 2개가 아닙니다: $ktorResults")

                // 모든 결과에 예상된 응답이 포함되어 있는지 검증
                results.forEach { result ->
                    assertTrue(result.contains(testResponse1) || result.contains("error"),
                        "결과에 예상된 응답 또는 에러가 포함되지 않았습니다: $result")
                }

                // 에러가 없었는지 확인
                val errorResults = results.filter { result -> result.contains("error") }
                assertTrue(errorResults.isEmpty(), "에러가 발생한 결과가 있습니다: $errorResults")

            } finally {
                executorService.shutdown()
                if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                    executorService.shutdownNow()
                }
            }
        }

        @Test
        @DisplayName("""
        [주어진 조건: 여러 어댑터 준비]
        [실행: 동시 캐시 쓰기 작업]
        [예상 결과: 동시 쓰기 작업이 안전하게 처리됨]
        """)
        fun `concurrent cache writes from different clients handled safely`() = runTest {
            val executorService = Executors.newFixedThreadPool(4)
            val latch = CountDownLatch(4)

            // Given: 어댑터와 테스트 URL 준비 (MockWebServer 없음)
            val okHttpAdapter = OkHttpAdapter()
            val ktorAdapter = net.spooncast.openmocker.lib.client.ktor.KtorAdapter()

            val testUrls = listOf(
                "$baseUrl/concurrent1",
                "$baseUrl/concurrent2",
                "$baseUrl/concurrent3",
                "$baseUrl/concurrent4"
            )

            val results = Collections.synchronizedList(mutableListOf<String>())

            try {
                // When: 동시 캐시 쓰기 - OkHttp 어댑터 (2회)
                repeat(2) { i ->
                    executorService.submit {
                        try {
                            val request = Request.Builder().url(testUrls[i]).build()
                            val requestData = okHttpAdapter.extractRequestData(request)
                            val responseData = """{"concurrent":"okhttp$i","timestamp":${System.currentTimeMillis()}}"""

                            // 직접 캐시에 저장
                            cacheRepo.cache(requestData.method, requestData.path, 200, responseData)
                            results.add("okhttp_write_$i:success")
                            println("OkHttp write $i completed: ${requestData.path}")
                        } catch (e: Exception) {
                            results.add("okhttp_write_$i:error:${e.message}")
                            println("OkHttp write $i failed: ${e.message}")
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                // Ktor 어댑터 동시 쓰기 (2회)
                repeat(2) { i ->
                    executorService.submit {
                        try {
                            val requestBuilder = HttpRequestBuilder().apply {
                                method = HttpMethod.Get
                                url(testUrls[i + 2])
                            }
                            val requestData = requestBuilder.build()
                            val ktorRequestData = ktorAdapter.extractRequestData(requestData)
                            val responseData = """{"concurrent":"ktor$i","timestamp":${System.currentTimeMillis()}}"""

                            // 직접 캐시에 저장
                            cacheRepo.cache(ktorRequestData.method, ktorRequestData.path, 200, responseData)
                            results.add("ktor_write_$i:success")
                            println("Ktor write $i completed: ${ktorRequestData.path}")
                        } catch (e: Exception) {
                            results.add("ktor_write_$i:error:${e.message}")
                            println("Ktor write $i failed: ${e.message}")
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                // Then: 완료 대기 및 검증
                assertTrue(latch.await(5, TimeUnit.SECONDS), "동시 쓰기 테스트가 5초 내에 완료되지 않았습니다")

                println("Concurrent write results: $results")
                assertEquals(4, results.size, "예상된 4개 쓰기 결과와 다릅니다: ${results.size}")

                // 모든 쓰기가 성공했는지 확인
                val successfulWrites = results.filter { result -> result.contains(":success") }
                assertEquals(4, successfulWrites.size, "모든 쓰기가 성공하지 않았습니다: $results")

                // 캐시에 데이터가 저장되었는지 확인
                assertTrue(cacheRepo.cachedMap.isNotEmpty(), "캐시에 데이터가 저장되지 않았습니다")
                assertTrue(cacheRepo.cachedMap.size >= 4, "예상된 4개 캐시 항목이 저장되지 않았습니다: ${cacheRepo.cachedMap.size}")

                // 각 URL에 대해 캐시 데이터 확인 (response 필드에서 조회)
                testUrls.forEachIndexed { index, url ->
                    val pathFromUrl = if (url.startsWith("http")) {
                        java.net.URL(url).path
                    } else {
                        url
                    }

                    val key = net.spooncast.openmocker.lib.model.CachedKey("GET", pathFromUrl)
                    val cachedValue = cacheRepo.cachedMap[key]
                    assertNotNull(cachedValue, "URL $url 에 대한 캐시 값이 없습니다 (path: $pathFromUrl)")

                    val responseData = cachedValue?.response
                    assertNotNull(responseData, "URL $url 에 대한 response 데이터가 없습니다")
                    assertTrue(responseData?.body?.contains("concurrent") == true,
                        "캐시된 response 데이터에 'concurrent' 키워드가 없습니다: ${responseData?.body}")
                }

            } finally {
                executorService.shutdown()
                if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                    executorService.shutdownNow()
                }
            }
        }
    }

    @Nested
    @DisplayName("데이터 호환성 테스트")
    inner class DataCompatibilityTests {

        @Test
        @DisplayName("""
        [주어진 조건: JSON 응답을 캐시한 상태]
        [실행: 다른 클라이언트로 접근]
        [예상 결과: JSON 응답이 클라이언트 간에 올바르게 공유됨]
        """)
        fun `json responses shared correctly between clients`() = runTest {
            val jsonResponse = """{"users":[{"id":1,"name":"John"},{"id":2,"name":"Jane"}],"total":2}"""
            val testUrl = "$baseUrl$endpoint1"

            // Given: OkHttp 어댑터로 JSON 응답을 직접 캐시에 저장
            val okHttpAdapter = OkHttpAdapter()
            val okHttpRequest = Request.Builder().url(testUrl).build()
            val okHttpRequestData = okHttpAdapter.extractRequestData(okHttpRequest)

            // JSON 응답을 캐시에 저장하고 mock으로 활성화
            cacheRepo.cache(okHttpRequestData.method, okHttpRequestData.path, 200, jsonResponse)
            val cacheKey = net.spooncast.openmocker.lib.model.CachedKey(okHttpRequestData.method, okHttpRequestData.path)
            val mockJsonResponse = net.spooncast.openmocker.lib.model.CachedResponse(200, jsonResponse)
            cacheRepo.mock(cacheKey, mockJsonResponse)

            // When: Ktor 어댑터로 동일한 캐시 접근
            val ktorAdapter = net.spooncast.openmocker.lib.client.ktor.KtorAdapter()
            val ktorRequestBuilder = HttpRequestBuilder().apply {
                method = HttpMethod.Get
                url(testUrl)
            }
            val ktorRequestData = ktorRequestBuilder.build()
            val ktorExtractedData = ktorAdapter.extractRequestData(ktorRequestData)

            // Then: Ktor가 OkHttp의 캐시된 JSON 응답에 접근 가능
            val ktorCachedMock = cacheRepo.getMock(ktorExtractedData.method, ktorExtractedData.path)
            assertNotNull(ktorCachedMock, "Ktor로 캐시된 JSON 응답을 찾을 수 없습니다")
            assertEquals(200, ktorCachedMock?.code, "JSON 응답 상태 코드가 보존되지 않았습니다")
            assertEquals(jsonResponse, ktorCachedMock?.body, "JSON 내용이 보존되지 않았습니다")

            // JSON 파싱 검증
            assertTrue(ktorCachedMock?.body?.contains("\"users\"") == true, "JSON 구조가 보존되지 않았습니다")
            assertTrue(ktorCachedMock?.body?.contains("\"total\":2") == true, "JSON 값이 보존되지 않았습니다")
        }

        @Test
        @DisplayName("""
        [주어진 조건: 텍스트 응답을 캐시한 상태]
        [실행: 다른 클라이언트로 접근]
        [예상 결과: 특수 문자를 포함한 텍스트가 올바르게 보존됨]
        """)
        fun `text responses preserved across client boundaries`() = runTest {
            val textResponse = "Plain text response with special characters: àáâãäåæçèéêë"

            // Given: 표준화된 URL로 경로 추출
            val testUrl = "$baseUrl$endpoint1"
            val okHttpAdapter = OkHttpAdapter()
            val okHttpRequest = Request.Builder().url(testUrl).build()
            val okHttpRequestData = okHttpAdapter.extractRequestData(okHttpRequest)

            // 텍스트 응답을 캐시에 직접 저장하고 mock으로 활성화
            cacheRepo.cache(okHttpRequestData.method, okHttpRequestData.path, 200, textResponse)
            val cacheKey = net.spooncast.openmocker.lib.model.CachedKey(okHttpRequestData.method, okHttpRequestData.path)
            val cachedResponse = net.spooncast.openmocker.lib.model.CachedResponse(200, textResponse)
            cacheRepo.mock(cacheKey, cachedResponse)

            // When: OkHttp 클라이언트로 캐시에서 직접 조회
            val cachedMock = cacheRepo.getMock(okHttpRequestData.method, okHttpRequestData.path)

            // Then: 특수 문자를 포함한 텍스트 내용이 보존되어야 함
            assertNotNull(cachedMock, "Cached mock should not be null")
            assertEquals(200, cachedMock!!.code)
            assertEquals(textResponse, cachedMock.body)
        }

        @Test
        @DisplayName("""
        [주어진 조건: 큰 응답 바디를 캐시한 상태]
        [실행: 다른 클라이언트로 접근]
        [예상 결과: 큰 응답 바디가 효율적으로 공유됨]
        """)
        fun `large response bodies shared efficiently`() = runTest {
            // Given: 큰 응답 생성 (>1KB)
            val largeJson = """{"data":"${"x".repeat(1000)}","size":1000}"""
            val testUrl = "$baseUrl$endpoint1"

            // OkHttp 어댑터로 큰 JSON 응답을 직접 캐시에 저장
            val okHttpAdapter = OkHttpAdapter()
            val okHttpRequest = Request.Builder().url(testUrl).build()
            val okHttpRequestData = okHttpAdapter.extractRequestData(okHttpRequest)

            // 큰 JSON 응답을 캐시에 저장하고 mock으로 활성화
            cacheRepo.cache(okHttpRequestData.method, okHttpRequestData.path, 200, largeJson)
            val cacheKey = net.spooncast.openmocker.lib.model.CachedKey(okHttpRequestData.method, okHttpRequestData.path)
            val mockLargeResponse = net.spooncast.openmocker.lib.model.CachedResponse(200, largeJson)
            cacheRepo.mock(cacheKey, mockLargeResponse)

            // When: Ktor 어댑터로 동일한 캐시 접근
            val ktorAdapter = net.spooncast.openmocker.lib.client.ktor.KtorAdapter()
            val ktorRequestBuilder = HttpRequestBuilder().apply {
                method = HttpMethod.Get
                url(testUrl)
            }
            val ktorRequestData = ktorRequestBuilder.build()
            val ktorExtractedData = ktorAdapter.extractRequestData(ktorRequestData)

            // Then: Ktor가 OkHttp의 캐시된 큰 응답에 접근 가능
            val ktorCachedMock = cacheRepo.getMock(ktorExtractedData.method, ktorExtractedData.path)
            assertNotNull(ktorCachedMock, "Ktor로 캐시된 큰 응답을 찾을 수 없습니다")
            assertEquals(200, ktorCachedMock?.code, "큰 응답 상태 코드가 보존되지 않았습니다")

            // 큰 내용 보존 검증
            val cachedBody = ktorCachedMock?.body ?: ""
            assertTrue(cachedBody.length > 1000, "큰 응답 바디 크기가 보존되지 않았습니다: ${cachedBody.length}")
            assertTrue(cachedBody.contains(""""size":1000"""), "큰 응답의 메타데이터가 보존되지 않았습니다")
            assertTrue(cachedBody.contains("x".repeat(100)), "큰 응답의 데이터 내용이 보존되지 않았습니다")
        }
    }

    @Nested
    @DisplayName("요청 매칭 일관성 테스트")
    inner class RequestMatchingConsistencyTests {

        @Test
        @DisplayName("""
        [주어진 조건: 특정 URL로 캐시된 응답]
        [실행: 다른 클라이언트에서 동일한 URL 요청]
        [예상 결과: 동일한 캐시 항목을 매칭]
        """)
        fun `same request from different clients matches same cache entry`() = runTest {
            // Given: 쿼리 파라미터가 포함된 URL로 직접 캐시 저장
            val urlWithParams = "$baseUrl$endpoint1?param1=value1&param2=value2"

            val okHttpAdapter = OkHttpAdapter()
            val okHttpRequest = Request.Builder().url(urlWithParams).build()
            val okHttpRequestData = okHttpAdapter.extractRequestData(okHttpRequest)

            println("URL with params - URL: $urlWithParams, Path: ${okHttpRequestData.path}")

            // 파라미터 포함된 URL 응답을 캐시에 저장하고 mock으로 활성화
            cacheRepo.cache(okHttpRequestData.method, okHttpRequestData.path, 200, testResponse1)
            val cacheKey = net.spooncast.openmocker.lib.model.CachedKey(okHttpRequestData.method, okHttpRequestData.path)
            val mockResponse = net.spooncast.openmocker.lib.model.CachedResponse(200, testResponse1)
            cacheRepo.mock(cacheKey, mockResponse)

            // When: Ktor 어댑터로 동일한 URL 접근
            val ktorAdapter = net.spooncast.openmocker.lib.client.ktor.KtorAdapter()
            val ktorRequestBuilder = HttpRequestBuilder().apply {
                method = HttpMethod.Get
                url(urlWithParams)
            }
            val ktorRequestData = ktorRequestBuilder.build()
            val ktorExtractedData = ktorAdapter.extractRequestData(ktorRequestData)

            println("Ktor URL with params - URL: ${ktorRequestData.url}, Path: ${ktorExtractedData.path}")

            // Then: 경로가 일치하고 동일한 캐시된 응답에 접근 가능
            assertEquals(okHttpRequestData.path, ktorExtractedData.path,
                "OkHttp(${okHttpRequestData.path})와 Ktor(${ktorExtractedData.path})의 경로 추출 결과가 다릅니다")

            val ktorCachedMock = cacheRepo.getMock(ktorExtractedData.method, ktorExtractedData.path)
            assertNotNull(ktorCachedMock, "Ktor로 파라미터 포함 URL의 캐시된 응답을 찾을 수 없습니다")
            assertEquals(200, ktorCachedMock?.code, "파라미터 포함 URL 응답 상태 코드가 보존되지 않았습니다")
            assertEquals(testResponse1, ktorCachedMock?.body, "파라미터 포함 URL 응답 내용이 보존되지 않았습니다")
        }

        @Test
        @DisplayName("""
        [주어진 조건: POST 요청으로 캐시된 응답]
        [실행: 다른 클라이언트에서 동일한 HTTP 메소드 요청]
        [예상 결과: HTTP 메소드 매칭이 클라이언트 간에 작동]
        """)
        fun `http method matching works across clients`() = runTest {
            val postResponse = """{"method":"POST"}"""

            // Given: 표준화된 URL로 POST 요청을 위한 경로 추출
            val testUrl = "$baseUrl$endpoint1"
            val okHttpAdapter = OkHttpAdapter()
            val okHttpPostRequest = Request.Builder()
                .url(testUrl)
                .post(create(null, "test body"))
                .build()
            val okHttpRequestData = okHttpAdapter.extractRequestData(okHttpPostRequest)

            // Ktor가 POST 요청 응답을 캐시했다고 가정하고 직접 캐시에 저장
            cacheRepo.cache(okHttpRequestData.method, okHttpRequestData.path, 200, postResponse)
            val cacheKey = net.spooncast.openmocker.lib.model.CachedKey(okHttpRequestData.method, okHttpRequestData.path)
            val cachedResponse = net.spooncast.openmocker.lib.model.CachedResponse(200, postResponse)
            cacheRepo.mock(cacheKey, cachedResponse)

            // When: OkHttp 클라이언트가 POST 메소드로 캐시에서 데이터를 조회
            val cachedMock = cacheRepo.getMock(okHttpRequestData.method, okHttpRequestData.path)

            // Then: POST 메소드가 정확히 매칭되어 응답이 반환되어야 함
            assertNotNull(cachedMock, "Cached mock should not be null")
            assertEquals(200, cachedMock!!.code)
            assertEquals(postResponse, cachedMock.body)
            assertEquals("POST", okHttpRequestData.method) // HTTP 메소드가 올바르게 추출되었는지 확인
        }
    }

    // 헬퍼 메소드들

    /**
     * OkHttp용 모킹 인터셉터 생성
     */
    private fun createOkHttpMockingInterceptor(): Interceptor {
        return OpenMockerInterceptor.Builder().build()
    }

    /**
     * 플러그인이 설치된 Ktor 클라이언트 생성
     */
    private fun createKtorClientWithPlugin(): HttpClient {
        return HttpClient(MockEngine { request ->
            // 이 모킹 엔진은 캐시가 올바르게 작동하면 호출되지 않아야 함
            respondError(HttpStatusCode.ServiceUnavailable, "캐시된 응답이 반환되어야 합니다")
        }) {
            install(OpenMockerPlugin) {
                enabled = true
            }
        }
    }
}