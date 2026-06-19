# Mock 제어 툴링 설계 — OpenMocker 고도화 + WALA 주입 + AS 플러그인

> 상태: **설계 동결 (확정)** · 작성 2026-06-19 · OpenMocker 세션에서 소스 확인 후 결정 동결
> 다음 작업: **M0 — OpenMocker `:lib` 제어 서버 + EventSink + upsertMock 구현 → curl 검증**
> 이 문서는 spooncast-android 핸드오프(§1~3 조사 사실)를 OpenMocker 실제 소스로 검증하고, 모든 설계 축을 확정한 self-contained 명세다.

---

## 0. 한 줄 요약

연결된 안드로이드 기기에 **REST 응답 + WALA(WebSocket) 이벤트를 mocking** 한다.
방향: 보유한 **OpenMocker(`com.github.spooncast:SpoonOpenMocker`, 현재 0.0.19)** 를 고도화 — ① REST mock을 외부에서 제어할 **임베디드 제어 서버**, ② 범용 **이벤트 주입(EventSink)** 통로(WALA용)를 더하고, ③ **Android Studio/IntelliJ 플러그인**으로 둘을 조작한다.

목표 사용자 시나리오 (확정):
1. **REST** — OpenMocker 활성 시 API 응답이 자동 저장된다. 플러그인에서 기록된 응답 리스트를 보고, 특정 응답을 수정하면 자동으로 mock 처리되어 **다음 호출부터 mock 응답이 반환**된다.
2. **WebSocket** — OpenMocker 활성 시 플러그인에서 특정 웹소켓 메시지를 **(수신 위장으로) 발사**해 앱이 그 이벤트를 받은 것처럼 반응하게 한다. (예: 라이브 종료 팝업 재현 — QATEAM-4709)

---

## 1. 동기 / 배경

- 발단: QATEAM-4709 (애청온도 상세 진입 중 **라이브 종료** 팝업 미노출) 재현 → **라이브 종료 소켓 이벤트를 임의 발생**시킬 수 있어야 함.
- 확장: WALA 이벤트뿐 아니라 **REST API 응답도** 자유롭게 mocking.
- 사용성: 앱 내 OpenMocker UI(알림 → Activity)보다 **데스크톱 플러그인 제어**가 편함.
- 자산화: 제어 서버 + EventSink + 플러그인은 OpenMocker의 범용 기능이라, 이 앱 밖에서도 재사용 가능한 라이브러리 자산이 된다.

---

## 2. 왜 "외부 프록시(MITM)"가 아니라 "인앱 방식"인가 (확인된 제약)

외부 MITM 프록시는 비용 대비 막힘이 많아 **인앱 방식**을 택했다.

- 기기: Pixel 6a USB 연결, `adb` 사용 가능. `mitmproxy`/`charles` 미설치.
- REST는 **Ktor (engine: `Android`)**, WALA는 **OkHttp WebSocket(`wss://`)** — 둘 다 TLS.
- dev `network_security_config.xml` 은 `cleartextTrafficPermitted=true` 만 있고 **user trust-anchors 없음** → 기기에 CA 깔아도 신뢰 안 함(비루팅). 즉 외부 MITM은 어차피 **코드 수정 + 재빌드** 필요.
- cert pinning 없음.
- 결론: 어차피 코드를 손대야 한다면 TLS 우회보다 **인앱 mock 계층**이 깔끔·안정적.

---

## 3. 확인된 현재 구조 (OpenMocker 소스 기준, 사실)

### 3.1 REST: Ktor + 이미 통합된 OpenMocker (spooncast-android)

`core/network/.../di/NetworkModule.kt`
```kotlin
import net.spooncast.openmocker.lib.client.ktor.OpenMockerPlugin
install(OpenMockerPlugin) {
    enabled = BuildConfig.FLAVOR != BuildType.PROD   // dev/stage 에서 활성
}
```
- 의존성: `gradle/libs.versions.toml` → `spoonOpenMocker = "0.0.19"`.
- 트리거: 알림 → `OpenMockerActivity`.

### 3.2 OpenMocker 0.0.19 실제 소스 표면 (⚠️ 핸드오프 AAR 추정 대비 정정)

핸드오프 문서의 AAR 디컴파일 추정과 **실제 소스가 다르다.** 정정:

| 핸드오프 추정 (AAR) | 실제 소스 (현재) |
|---|---|
| `CacheRepo` 가 외부 접근 가능한 인터페이스 | **`internal interface CacheRepo`** — 패키지 밖 비가시 |
| `getCachedMap()` / Java 스타일 시그니처 | `val cachedMap: Map<CachedKey, CachedValue>` (프로퍼티) |
| `cache(method, path, code, body)` | `cache(method, urlPath, responseCode, responseBody)` |
| 모델 public | `CachedKey`/`CachedValue`/`CachedResponse` **전부 internal** |
| facade가 제어 통로 노출 가능성 | `OpenMocker` object는 `getInterceptor/show/showNotification/hideNotification`만 |

확정 사실:
- **Facade** `net.spooncast.openmocker.lib.OpenMocker` (object) — public API는 위 4개뿐.
- **저장소** `MemCacheRepoImpl` (internal, 싱글톤 `getInstance()`), 내부는 Compose `SnapshotStateMap`(UI reactive 관찰, 쓰기 thread-safe).
  ```
  val cachedMap: Map<CachedKey, CachedValue>
  fun cache(method, urlPath, responseCode, responseBody)   // 응답 관측 기록. 호출 시 CachedValue 새로 생성 → 기존 mock 제거됨
  fun clearCache()
  fun getMock(method, urlPath): CachedResponse?
  fun mock(key: CachedKey, response: CachedResponse): Boolean   // 키가 이미 있어야 true
  fun unMock(key: CachedKey): Boolean
  ```
- **모델**: `CachedKey(method, path)`, `CachedResponse(code, body, duration=0)`, `CachedValue(response, mock?)`.
- **키 생성**: 어댑터(`KtorAdapter`/`OkHttpAdapter`) 모두 `path = url.encodedPath` → **쿼리스트링 제외**. cachedMap은 `(method, path) → response/mock`만 저장(요청 URL·쿼리·헤더 미보존).
- 어댑터 구조: `MockingEngine<TReq,TResp>` + `HttpClientAdapter` — HTTP 전용, **WebSocket 개념 없음.**

함의:
- 제어 서버를 **`:lib` 모듈 안에** 두면 모든 게 `internal` 이라 `MemCacheRepoImpl.getInstance()` 와 신규 레지스트리를 **직접 호출** 가능 → facade/모델 노출 불필요(캡슐화 유지). → 핸드오프의 "CacheRepo 외부 노출 방식 결정" 문제 소멸.
- WALA(WebSocket)는 HTTP mocking 범위 밖 → **범용 EventSink** 를 신설하고 앱이 WALA를 연결.

### 3.3 WALA: OkHttp WebSocket, 단일 파싱 진입점 (spooncast-android)

`spooncast/.../ui/chat/repo/WalaChatRepository.kt`
- `okhttp3.WebSocketListener()` 직접 구현. `onMessage(webSocket, text)` 가 **유일한 수신 파싱 진입점**:
  - `gson.fromJson(text, WalaMessageV2)` → `eventName` 으로 serializer 선택 → `WalaEventV2` 조립 → `chatEventBus.sendEvent` + `_chatMessageFlow.emit`.
  - 라이브 종료: `WALA_ROOM_CLOSE`, `WALA_ROOM_FORCE_CLOSE`.
- **핵심: `onMessage` 본문은 `webSocket` 파라미터를 안 씀.** → `private fun handleRawMessage(text: String)` 로 추출하면 `onMessage` 와 신규 `debugInject(text)` 가 공유 → 기존 serializer 전부 재사용, 어떤 WALA 이벤트든 raw JSON 한 줄로 **수신 위장** 주입 가능.

---

## 4. 동결된 결정 (전 축 확정)

| Axis / 항목 | 결정 | 근거 |
|---|---|---|
| A. REST 엔진 | **OpenMocker 소스 고도화** (제어 서버 내장) | 소스 보유. 외부 싱글톤 접근·신규작성 폐기 |
| B. WALA 방식 | OpenMocker에 **범용 EventSink** 추가 + 앱이 WALA sink 등록 → `debugInject` | push 모델, OpenMocker는 글자만 전달·해석은 앱 |
| C. 데스크톱↔앱 통로 | **임베디드 HTTP 제어 서버 + `adb forward`** | 양방향·대용량 JSON·상태조회 우수 |
| D. 데스크톱 UI | **AS/IntelliJ 플러그인** | 서버가 엔진, 플러그인은 편의 레이어 |
| 제어 서버 위치 | **`:lib` 본체 + opt-in start** | internal 직접 접근, start 안 하면 죽은 코드. 분리 모듈은 캡슐화 깸 |
| 서버 의존성 | **raw `ServerSocket` 코루틴 (무의존)** | 라우트 5개·localhost·작은 JSON. transitive dep 안 남김 |
| EventSink 추상화 | **범용 EventSink** (WebSocket 어댑터로 구조화 안 함) | pull/push 성격 달라 어댑터 공유 이득 없음. 과투자 회피 |
| G1. mock 키 granularity | **`(method, path)` 현행 유지** (쿼리 미반영) | 대상 API가 path로 구분됨. `CachedKey` 변경 없음 |
| G5. WS 메시지 출처 | **플러그인 자유 raw 텍스트 + 앱 프리셋 버튼** | 재빌드 없이 임의 메시지 발사, presets는 편의 |
| WS 방향 | **수신 위장 (a)** | 동기(QATEAM-4709)와 일치. 서버 실송신(b) 아님 |
| 플러그인 레포 | **같은 레포 `plugin/` 독립 Gradle 빌드** | contract↔클라이언트 원자적 동기화 + 툴체인 격리 |

전제(중요): 플러그인은 데스크톱 JVM이라 앱 인프로세스를 직접 못 만짐. **"앱 내 에이전트(제어 서버) ↔ 플러그인" 2-파트**, 사이는 `adb` 로 연결.

---

## 5. 목표 아키텍처

```
[AS/IntelliJ 플러그인 (데스크톱, plugin/ 독립 빌드)]
   ToolWindow UI
   - REST 패널: 기록 테이블 / status·body 편집 / 저장·해제
   - WS 패널: sink 선택 + 프리셋 버튼 + 자유 텍스트 / 발사
        │  HTTP over  adb forward tcp:8099 tcp:8099
        ▼
[제어 서버 (OpenMocker :lib 내, non-PROD opt-in, raw ServerSocket)]
   GET    /rest/recorded         → CacheRepo.cachedMap
   POST   /rest/mock             → CacheRepo.upsertMock()
   DELETE /rest/mock?method=&path= / ?all=true → unMock() / clearCache()
   GET    /inject/sinks          → SinkRegistry 목록(+presets)
   POST   /inject/{id}           → registry[id].inject(rawPayload)
        │ (REST 정식 API)                  │ (범용 EventSink)
        ▼                                  ▼
   OpenMocker CacheRepo               앱이 등록한 WALA sink
   → Ktor OpenMockerPlugin 응답 대체   → WalaChatRepository.debugInject(text)
                                       → handleRawMessage(text) (onMessage 공유)
                                       → _chatMessageFlow.emit / chatEventBus
```

---

## 6. API Contract (동결 — 앱·플러그인 공용 계약)

전부 `localhost:8099`, HTTP/1.1, 기본 off.

**REST mock 제어**
```
GET    /rest/recorded
  → 200 [{ "method","path", "response":{"code","body"}, "mock":{"code","body","duration"}|null }]

POST   /rest/mock     body: { "method","path","code","body","duration":0 }
  → 200 {"ok":true}        // upsert: 항목 없으면 생성 후 mock 세팅 (create-or-update)

DELETE /rest/mock?method=GET&path=/api/live   → 200   // 해당 mock 해제(unMock)
DELETE /rest/mock?all=true                    → 200   // clearCache 전체
```

**이벤트 주입**
```
GET    /inject/sinks
  → 200 [{ "id":"wala","name":"WALA Socket",
           "presets":[{"name":"room_close","payload":"{...raw json...}"}] }]

POST   /inject/{id}   body: <raw payload string, 그대로>
  → 200 {"ok":true} / 404 (sink 없음)   // 파싱 없이 registry[id].inject(body) 토스
```
주의: `POST /rest/mock` 바디는 우리가 파싱하는 JSON, `POST /inject/{id}` 바디는 **건드리지 않고 통째로** sink에 전달하는 raw 메시지.

---

## 7. 컴포넌트별 작업

### 7.1 OpenMocker `:lib` (M0 — 메인 작업)

신규 패키지 `net.spooncast.openmocker.lib.control`:

| 파일 | 책임 | 가시성 |
|---|---|---|
| `OpenMockerEventSink.kt` | `interface OpenMockerEventSink { val id; val name; fun inject(payload: String); fun presets(): List<Preset> }` + `data class Preset(name, payload)` | **public** (앱이 구현) |
| `SinkRegistry.kt` | sink 등록/해제/조회 in-memory 싱글톤 | internal |
| `ControlService.kt` | CacheRepo + SinkRegistry 받아 도메인 동작(recorded/upsert/unmock/clear/sinks/inject). HTTP와 분리 → 단위 테스트 | internal |
| `ControlServer.kt` | `ServerSocket` 코루틴(Dispatchers.IO), HTTP/1.1 최소 파서, 라우팅 → ControlService | internal |
| `dto/*.kt` | 요청/응답 DTO (`@Serializable`, kotlinx — 기존 의존성) | internal |

변경:
- **`OpenMocker.kt`** (public 추가): `startControlServer(port = 8099)`, `stopControlServer()`, `registerSink(sink)`, `unregisterSink(id)`. 서버가 in-module → `MemCacheRepoImpl.getInstance()` 직접 접근.
- **`CacheRepo` + `MemCacheRepoImpl`**: `upsertMock(method, path, code, body, duration)` 추가 — 항목 없으면 생성, 있으면 mock만 세팅(기존 `cache()`가 mock을 날리는 문제 회피).
- 버전 올리고 spooncast-android `libs.versions.toml` 의 `spoonOpenMocker` 갱신.

**⚠️ 구현 전 실측 (G3, 리스크):** `OpenMockerPlugin.on(Send)` 가 mock으로 short-circuit한 응답이 `onResponse` 를 다시 타서 `cache()` 로 mock을 덮어쓰는지 작은 테스트로 확인. 덮어쓰면 "다음 호출부터 **계속** mock"이 깨지므로, 그 경우 mock 적용 응답은 재기록 skip 가드 추가.

> **검증 결과 (2026-06-19, Ktor 공식 문서 + 소스 교차 확인):** **G3는 Ktor 경로에서 발생하지 않음(블로커 아님).**
> Ktor 문서의 훅 실행 순서상 `onResponse`(=`receivePipeline`)는 **실제 send(`proceed`) 횟수만큼만** 발생한다. mock 경로(`OpenMockerPlugin.kt:21-27`)는 `proceed()` 없이 `return@on createMockResponse(...).call` 로 short-circuit → outer client의 `onResponse` 미발생 → `cache()` 미호출 → mock 유지. 또한 mock 응답은 `createMockResponse` 내부의 **별도 `HttpClient(mockEngine)`**(OpenMockerPlugin 미설치)에서 생성됨.
> → 재기록 skip 가드는 Ktor에선 사실상 불필요. 단 ① 테스트 1개로 비재기록 확정, ② **OkHttp 경로는 별도 확인** 필요.

#### 7.1.1 KtorAdapter 실제 결함 (G3 검증 중 발견 — M0에서 함께 수정)

G3 점검 과정에서 `KtorAdapter.createMockResponse` / `extractResponseData` 에 실제 결함을 확인. **모두 M0 범위에서 수정한다.**

| ID | 위치 | 증상 | 수정 방향 | 심각도 |
|---|---|---|---|---|
| **KA-A** | `KtorAdapter.kt:85,88-102` | `expectSuccess = true` + `client.close()` 가 `client.request{}` **뒤**에 위치 → 4xx/5xx mock 시 ResponseException throw로 `close()` skip → **HttpClient 누수**(에러 응답 mock마다). 에러 핸들링 테스트가 mock 핵심 용도라 빈발 가능 | `try/finally` 로 `close()` 보장 | 🔴 실제 버그 |
| **KA-B** | `KtorAdapter.kt:85` | `expectSuccess = true` 가 내부 mock 클라이언트에 하드코딩 → outer(실제) 클라이언트가 expectSuccess 미사용이면 **실제 에러 응답은 정상 반환, mock 에러 응답만 throw** → mock/실제 동작 불일치(fidelity 깨짐) | mock 클라이언트는 expectSuccess 끄고 응답 그대로 전달(outer validator에 위임) 검토 | 🟡 설계 검토 |
| **KA-C** | `KtorAdapter.kt:40-48` | `onResponse → cacheResponse` 경로에서 `runBlocking { bodyAsText() }` 로 응답 본문을 읽음. Ktor 본문 채널은 1회성일 수 있어 **훅이 먼저 소비하면 앱이 빈 본문 수신** 가능. 기존 `try/catch → ""` 폴백이 과거 읽기 실패 방증 | 본문을 소비하지 않고 읽는 방식(saved response 보장 / 안전 복제) 확인·적용 | 🟡 검증 필요 |
| **KA-D** | `KtorAdapter.kt:62-103` | mock 응답 1건마다 `HttpClient`+`MockEngine`+`ContentNegotiation` 신규 생성 + suspend 컨텍스트에서 `runBlocking` 블로킹 → 비효율·안티패턴 | mock 응답 생성을 클라이언트 신규 생성 없이 구성하거나 재사용 | 🟡 개선 권장 |

### 7.2 spooncast-android 앱 배선 (M1)

1. `WalaChatRepository`: `onMessage` 파싱부를 `private fun handleRawMessage(text: String)` 로 추출(동작 동일, `webSocket` 미사용 확인됨). `ChatRepo` 또는 debug 전용 인터페이스에 `fun debugInject(text: String)` → `handleRawMessage(text)`. **non-PROD 가드.**
2. WALA `OpenMockerEventSink` 구현체: `inject(payload) = walaChatRepository.debugInject(payload)`, presets = room_close / force_close 등 라이브 종료 위주.
3. 앱 시작(non-PROD): `OpenMocker.registerSink(walaSink)` + `OpenMocker.startControlServer()`. 등록 위치는 OpenMocker 알림 띄우는 초기화 지점 근처.
4. 빌드 가드: 전부 `BuildConfig.FLAVOR != BuildType.PROD`.

### 7.3 AS/IntelliJ 플러그인 (M2 — `plugin/` 독립 빌드)

- **위치/빌드**: 같은 레포 `plugin/` 하위에 **자체 `settings.gradle.kts`를 둔 standalone Gradle 빌드** (루트 Android 빌드에 include 안 함 — IntelliJ Platform 리포지토리 셋업이 루트의 `FAIL_ON_PROJECT_REPOS`와 충돌하고, Android 개발자 클론 무게를 피하기 위함). IDE는 두 프로젝트로 따로 import.
- **빌드 도구**: IntelliJ Platform Gradle Plugin 2.x. (구체 버전·since/until-build는 구현 시 확정)
- **타깃**: IntelliJ Platform(IC) 베이스 → AS·IDEA 양쪽 로드. adb는 **셸 아웃**(`adb`는 `ANDROID_HOME`에서 탐색)이라 android 플러그인 API 의존 불필요.
- **UI**: Swing + Kotlin UI DSL.
- **HTTP**: Java 11 `HttpClient` (의존성 0).
- **ToolWindow 2패널 ↔ contract 매핑**:

  | 패널 | 동작 | 호출 |
  |---|---|---|
  | REST | 기록 테이블(method/path/mocked?) 주기 폴링 | `GET /rest/recorded` |
  | | 행 선택 → status code + body 편집 → 저장 | `POST /rest/mock` |
  | | mock 해제 / 전체 clear | `DELETE /rest/mock` |
  | WS | sink 드롭다운 + 프리셋 버튼 + 자유 텍스트 → 발사 | `GET /inject/sinks`, `POST /inject/{id}` |

- **라이프사이클**: ToolWindow 열릴 때 `adb -s <serial> forward tcp:8099 tcp:8099` 보장 → 기록 새로고침(수동 버튼 + 주기 폴링). 포트는 설정. 다중 기기는 `adb devices` 드롭다운.
- **배포**: 사내 — zip 수동 배포 또는 내부 plugin repo. 마켓플레이스 X.

---

## 8. 빌드/검증 순서 (마일스톤)

- **M0** — OpenMocker `:lib`: 제어 서버 + EventSink API + `upsertMock` (7.1) + **KtorAdapter 결함 KA-A~D 수정(7.1.1)** → **curl 로 검증**. (G3 비재기록 테스트 포함)
- **M1** — spooncast-android: WALA 배선 (7.2) + 버전 갱신 → `curl localhost:8099/inject/wala -d '<room_close json>'` 로 라이브 종료 재현(QATEAM-4709). `curl .../rest/mock` 로 REST mock 재현.
- **M2** — 플러그인 (7.3): 동결된 contract 위에 UI.

> M0·M1만으로 curl/스크립트로 즉시 사용 가능. 플러그인은 편의 레이어.

---

## 9. 해소된 항목 / 남은 확인거리

해소(§4에서 동결):
- 제어 서버 의존성 → raw `ServerSocket`. / 위치 → `:lib` 본체 opt-in.
- EventSink 정식화 → 범용 EventSink. / mock 키 granularity → path만.
- 플러그인 UI 스택 → Swing. / 레포 → 같은 레포 `plugin/` 독립 빌드.

검증 완료:
- **G3** — mock 재기록 우려 → **Ktor 경로에서 미발생 확정**(문서+소스, 7.1). 테스트 1개로 확정 + OkHttp 경로만 잔여 확인.

남은 확인거리(구현 시):
- **KA-A~D** — `KtorAdapter` 실제 결함 4건, M0에서 함께 수정(7.1.1). KA-A(에러 mock 시 client 누수)가 확정 버그.
- **OkHttp 경로 G3** — OkHttp 인터셉터에서도 mock short-circuit이 `cache()` 재호출을 유발하지 않는지 확인.
- **G2** — 기록 리스트가 method+path+response만 → 플러그인 표시 granularity 한계(쿼리·풀 URL·헤더 미표시). path-only 결정과 일관되므로 수용.
- 포트 고정(8099) vs 협상 — 우선 고정, 설정으로 변경 허용.
- 플러그인 IntelliJ Platform Gradle Plugin 버전·AS 호환 범위(since/until-build) 확정.

---

## 부록 A. 참고 파일

OpenMocker (`:lib`)
- `OpenMocker.kt` (facade), `data/repo/CacheRepo.kt` · `MemCacheRepoImpl.kt`, `data/MockingEngine.kt`, `data/adapter/{KtorAdapter,OkHttpAdapter}.kt`, `model/{CachedKey,CachedValue,HttpReq}.kt`, `client/ktor/OpenMockerPlugin.kt`

spooncast-android
- `core/network/.../di/NetworkModule.kt` — OpenMockerPlugin 설치
- `spooncast/.../ui/chat/repo/WalaChatRepository.kt` — WALA WebSocket, `onMessage`/`_chatMessageFlow`
- `spooncast/src/{dev,stage,prod}/res/xml/network_security_config.xml` — TLS 트러스트
- `gradle/libs.versions.toml` — `spoonOpenMocker` 버전 핀
