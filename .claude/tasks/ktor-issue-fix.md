# Ktor 지원 기능 문제점 수정 계획

## Plan 1: `expectSuccess = false`로 변경하여 에러 응답 모킹 허용

**심각도**: Critical
**파일**: `lib/src/main/java/net/spooncast/openmocker/lib/data/adapter/KtorAdapter.kt`

**문제**
`createMockResponse()`에서 임시 HttpClient 생성 시 `expectSuccess = true`가 설정되어 있어,
4xx/5xx 상태 코드를 모킹하면 응답 대신 `ClientRequestException`/`ServerResponseException`이 throw된다.
OkHttp는 `Response.Builder()`로 직접 응답을 생성하므로 상태 코드에 관계없이 정상 반환된다.

**수정 내용**
- `KtorAdapter.kt` line 84-85: `expectSuccess = true` → `expectSuccess = false`
- 오해를 유발하는 주석 제거

### 완료 조건
- [ ] `expectSuccess = false`로 변경됨
- [ ] 관련 주석 제거 또는 수정됨
- [ ] `./gradlew :lib:build` 빌드 성공
- [ ] `./gradlew :lib:testDebugUnitTest` 테스트 통과
- [ ] 데모 앱에서 4xx/5xx 상태 코드 모킹 시 예외 없이 응답 반환 확인

### 보안 점검
- [ ] `expectSuccess = false`는 라이브러리 내부 임시 MockEngine에만 적용되며, 사용자의 실제 HttpClient 설정에 영향 없음
- [ ] 모킹된 응답의 상태 코드가 그대로 전달되므로, 호출 측 에러 핸들링이 정상 동작하는지 확인

### 의존성
- [ ] 없음 — 이 변경은 독립적으로 적용 가능

---

## Plan 2: `extractResponseData`의 `suspend` 전파로 데드락 위험 제거

**심각도**: Moderate
**파일**: 5개 파일 수정

**문제**
`KtorAdapter.extractResponseData()`에서 `runBlocking { clientResponse.bodyAsText() }`를 호출한다.
이 메서드는 `OpenMockerPlugin.kt:34`의 `onResponse` 블록(suspend 컨텍스트)에서 호출되므로,
`runBlocking`이 현재 코루틴 스레드를 블로킹하여 데드락 위험이 있다.

**수정 내용**

| # | 파일 | 변경 |
|---|------|------|
| 1 | `HttpClientAdapter.kt` | `fun extractResponseData` → `suspend fun extractResponseData` |
| 2 | `KtorAdapter.kt` | `runBlocking` 블록 제거, `suspend fun`으로 변경 |
| 3 | `OkHttpAdapter.kt` | `suspend` 키워드 추가 (내부 로직 변경 없음) |
| 4 | `MockingEngine.kt` | `fun cacheResponse` → `suspend fun cacheResponse` |
| 5 | `OpenMockerInterceptor.kt` | `mockingEngine.cacheResponse()` 호출을 `runBlocking { }` 으로 감싸기 |

### 완료 조건
- [ ] `HttpClientAdapter.extractResponseData`가 `suspend fun`으로 선언됨
- [ ] `KtorAdapter.extractResponseData`에서 `runBlocking` 제거됨
- [ ] `OkHttpAdapter.extractResponseData`에 `suspend` 추가됨
- [ ] `MockingEngine.cacheResponse`가 `suspend fun`으로 선언됨
- [ ] `OpenMockerInterceptor`에서 `cacheResponse` 호출이 `runBlocking`으로 감싸져 있음
- [ ] `KtorAdapter`에 `runBlocking` import가 유지됨 (`createMockResponse`에서 사용)
- [ ] `./gradlew :lib:build` 빌드 성공
- [ ] `./gradlew :lib:testDebugUnitTest` 테스트 통과
- [ ] Ktor 플러그인의 `onResponse` 블록에서 데드락 없이 응답 캐싱 동작 확인

### 보안 점검
- [ ] `OpenMockerInterceptor`의 `runBlocking`은 OkHttp Interceptor 내부(non-suspend)에서만 사용되므로 데드락 위험 없음 — OkHttp Interceptor는 자체 스레드풀에서 실행됨
- [ ] `suspend` 전파가 public API 시그니처에 영향을 주지 않음 — 변경된 함수들은 모두 `internal` 가시성
- [ ] 코루틴 컨텍스트 전환으로 인한 스레드 안전성 문제 없음 — `MemCacheRepoImpl`은 `mutableStateMapOf`로 thread-safe

### 의존성
- [ ] 없음 — Plan 1과 독립적으로 적용 가능
- [ ] 단, 5개 파일 변경은 **동시에** 적용해야 함 (인터페이스 시그니처 변경이므로 부분 적용 시 컴파일 에러)
