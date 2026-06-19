# Mock 제어 툴링 구현 계획 — M0(lib) + 데모앱 curl 배선 + M2 제품 수준 플러그인

> 상태: **구현 계획 (확정 대기)** · 작성 2026-06-19 · 설계 문서 [mock-control-tooling-design.md](./mock-control-tooling-design.md) 기반
> 코드 교차검증 후 파일 단위로 구체화한 실행 계획. 이 SpoonOpenMocker 레포에서 실행 가능한 전부를 다룬다.

## Context

[mock-control-tooling-design.md](./mock-control-tooling-design.md) 에서 설계가 동결됐다. 연결된 안드로이드 기기의 **REST 응답 + WebSocket(WALA) 이벤트**를 데스크톱(AS/IntelliJ 플러그인)에서 mocking/주입하기 위해, 보유한 OpenMocker 라이브러리를 고도화한다 — ① `:lib`에 임베디드 HTTP 제어 서버, ② 범용 EventSink, ③ 제어용 IntelliJ 플러그인.

본 계획은 **이 SpoonOpenMocker 레포에서 실행 가능한 전부**를 다룬다(M1=spooncast-android는 별도 레포라 제외):
- **M0** — `:lib` 제어 서버 + EventSink + `upsertMock` + KtorAdapter 결함(KA-A~D) 수정 + 버전업.
- **데모앱 curl 배선** — `app/`에 debug 한정으로 제어 서버를 띄워 이 레포만으로 curl E2E 검증.
- **M2 제품 수준 플러그인** — 같은 레포 `plugin/` 독립 Gradle 빌드 + 사내 배포해 실사용 가능한 완성 ToolWindow(동결된 contract 위).

확정된 결정(사용자 승인):
- **KA-B**: mock 클라이언트의 `expectSuccess=true` **제거** — mock 에러도 실제 응답처럼 그대로 반환, throw 여부는 바깥(실제) 클라이언트 정책에 위임(실제/mock 동작 일치 + KA-A 누수 경로 제거).
- **코루틴 의존성**: `kotlinx-coroutines-core` **명시적 추가**(현재는 ktor/okhttp transitive에만 의존).

사전 확인 사실(코드 교차검증):
- **G3(“mock 한 번 쓰고 지워짐”)는 양쪽 경로 모두 미발생.** Ktor `OpenMockerPlugin.on(Send)`(`OpenMockerPlugin.kt:21-27`)·OkHttp `OpenMockerInterceptor.intercept()`(`OpenMockerInterceptor.kt:21-27`) 모두 mock 시 `proceed()`/`cacheResponse()` 없이 즉시 return → `cache()` 미호출 → mock 유지. 회귀 테스트로 고정한다.
- `CacheRepo`/`MemCacheRepoImpl`(싱글톤 `getInstance()`)/`CachedKey`/`CachedValue`/`CachedResponse` 전부 `internal` → `:lib` 내부 제어 서버에서 직접 호출 가능(모델 public 노출 불필요).
- `cache()`는 `CachedValue(response=...)`로 덮어써 mock을 null로 날리고(`MemCacheRepoImpl.kt:32`), `mock(key,resp)`는 키가 없으면 false → `upsertMock`(create-or-update)이 필요.
- `CachedValue`/`CachedResponse`는 `@Serializable`(kotlinx). DTO도 kotlinx.serialization 사용(기존 의존성).
- 테스트 컨벤션: JUnit5(`org.junit.jupiter`), 한글 백틱 테스트명, mockk, `ktor-client-mock`, `MemCacheRepoImpl` 싱글톤은 `@BeforeEach`에서 clear.
- `app/` 데모: `DemoApplication`(Application) + Ktor/OkHttp weather repo 둘 다 존재 → 배선 지점 명확.
- `settings.gradle.kts`에 `FAIL_ON_PROJECT_REPOS` → 루트에 IntelliJ Platform 리포 추가 불가 → `plugin/`은 자체 `settings.gradle.kts`를 둔 독립 빌드여야 함(설계와 일치). `plugin/` 미존재.

---

## 0. 전체 그림 (쉬운 개요)

만드는 것 = **"앱을 리모컨으로 조종하는 시스템"**.
```
[내 컴퓨터의 AS 플러그인]  →(HTTP 신호, adb forward)→  [폰 앱 안의 제어 서버]
   리모컨 UI                                            "가짜 응답 줘"/"가짜 이벤트 발생"
```
4개 덩어리로 만든다:
- **Part A (M0, 핵심)** — 폰 앱 안에 "리모컨 수신기"(제어 서버) + 이벤트 통로(EventSink) + `upsertMock`을 심고, 기존 KtorAdapter 버그(KA-A~D)도 같이 고친다. 이것만으로 `curl`로 조종 가능.
- **Part B (데모 배선)** — 이 레포의 데모 앱이 부팅 시 수신기를 켜도록 한 줄 연결 → 이 레포만으로 curl E2E 검증.
- **Part C (M2, 제품 수준 플러그인)** — 컴퓨터 쪽 리모컨을 **실사용 가능한 완성품**으로. 기기 선택·자동 adb forward·기록 테이블 폴링·mock 편집/해제·이벤트 발사·설정 영속화·상태 알림·사내 배포까지.
- 순서: 재료(T1·T2·T6) → 제어서버(T3→T4→T5) → 데모(T9) → 플러그인(T10~T17).

---

## Part A — M0: `:lib` 제어 서버 + EventSink + upsertMock

### 신규 파일 (패키지 `net.spooncast.openmocker.lib.control`)
`lib/src/main/java/net/spooncast/openmocker/lib/control/`

| 파일 | 책임 | 가시성 |
|---|---|---|
| `OpenMockerEventSink.kt` | `interface OpenMockerEventSink { val id; val name; fun inject(payload: String); fun presets(): List<Preset> }` + `data class Preset(name, payload)` | **public** |
| `SinkRegistry.kt` | sink 등록/해제/조회 in-memory 싱글톤(thread-safe), 테스트용 `clear()` | internal |
| `ControlService.kt` | `(CacheRepo, SinkRegistry)` 주입, 각 라우트를 repo/registry 호출로 매핑 + 내부 모델→DTO 변환. HTTP/소켓 무지 → 단위 테스트 | internal |
| `ControlServer.kt` | raw `ServerSocket` accept 루프(`Dispatchers.IO`), 최소 HTTP/1.1 파서, 라우팅→`ControlService`, JSON 직렬화, 생명주기(start/stop, 멱등) | internal |
| `dto/ControlDtos.kt` | `@Serializable` DTO: `RecordedEntryDto/ResponseDto/MockDto`, `MockRequestDto`, `SinkDto/PresetDto`, `OkDto` | internal |

### 수정 파일
| 파일 | 변경 |
|---|---|
| `OpenMocker.kt` | public 추가: `startControlServer(port:Int=8099)`, `stopControlServer()`, `registerSink(sink)`, `unregisterSink(id)` — `MemCacheRepoImpl.getInstance()`/`SinkRegistry` 직접 배선 |
| `data/repo/CacheRepo.kt` | 인터페이스에 `fun upsertMock(method, urlPath, code, body, duration: Long = 0L): Boolean` |
| `data/repo/MemCacheRepoImpl.kt` | `upsertMock` 구현(create-or-update) |
| `lib/build.gradle.kts` | `kotlinx-coroutines-core` 명시적 `implementation` 추가 + `version` 상향(`:148`) |
| `gradle/libs.versions.toml` | coroutines 카탈로그 항목 추가 |

**변경 없음**: `CachedKey`/`CachedValue`/`CachedResponse`/`HttpReq`/`HttpResp` 모델(G1 동결, path-only 키 유지), `MockingEngine`, OkHttp/Ktor 어댑터의 키 생성 로직, 기존 Compose UI(`ui/`).

### ControlServer 설계
- `start(port)`: 멱등(`running` 가드) → `ServerSocket().bind(InetSocketAddress(loopback, port))`(로컬호스트 전용, `adb forward`와 일치) → `CoroutineScope(Dispatchers.IO + SupervisorJob())`에서 accept 루프.
- accept 루프: `while(isActive) { socket=accept(); scope.launch { handle(socket) } }`. `stop()`이 소켓 close → `accept()`가 `SocketException` → 루프 정상 종료.
- **bind 실패 정책**: throw 대신 `android.util.Log`로 로깅 후 noop(비-PROD 디버그 서버 실패가 앱 기동을 깨면 안 됨).
- 커넥션 처리(`Connection: close`, 요청 1건/커넥션):
  - **요청 파서**: raw `InputStream`에서 head(요청라인+헤더)를 CRLF 단위로 직접 읽고, `Content-Length`만큼 body 바이트를 정확히 읽어 UTF-8 디코드. ⚠️ `BufferedReader`가 body 바이트를 삼키는 고전 버그 회피 — `readHttpRequest(InputStream)`를 순수 함수로 분리(단위 테스트 대상).
  - 라우팅: 요청라인 `METHOD target` → path/query 분리, 아래 표대로 분기.
  - 응답: `HTTP/1.1 {status}` + `Content-Type: application/json` + `Content-Length` + `Connection: close` + body(UTF-8). 핸들러 전체 try/catch → 예외 시 400/500 JSON.
- **직렬화 경계**: `ControlService`는 타입 DTO 반환, `ControlServer`가 `Json.encodeToString`/`decodeFromString` 담당 → 서비스는 순수·테스트 용이.

라우팅 ↔ contract (동결, 설계 §6):
| 라우트 | ControlService | 응답 |
|---|---|---|
| `GET /rest/recorded` | `recorded()` | 200 `[RecordedEntryDto]` |
| `POST /rest/mock` | body→`MockRequestDto`→`upsertMock(dto)` | 200 `{"ok":true}` |
| `DELETE /rest/mock?all=true` | `clearAll()` | 200 |
| `DELETE /rest/mock?method=&path=` | `unMock(method,path)` | 200(멱등, 키 없어도 200) |
| `GET /inject/sinks` | `sinks()` | 200 `[SinkDto]` |
| `POST /inject/{id}` | `inject(id, rawBody)` | 200 `{"ok":true}` / 404 |

`/inject/{id}` body는 **파싱 없이 통째** 전달. `/rest/mock` body만 우리가 JSON 파싱.

### ControlService 설계
```
internal class ControlService(private val cacheRepo: CacheRepo, private val sinkRegistry: SinkRegistry) {
    fun recorded(): List<RecordedEntryDto>      // cachedMap.toMap() 방어복사 후 매핑(SnapshotStateMap CME 회피)
    fun upsertMock(req: MockRequestDto): Boolean // cacheRepo.upsertMock(...) 위임
    fun unMock(method: String, path: String): Boolean
    fun clearAll()                               // cacheRepo.clearCache()
    fun sinks(): List<SinkDto>                   // sinkRegistry.all().map{ id,name,presets }
    fun inject(id: String, payload: String): Boolean // sinkRegistry.get(id)?.inject(payload) ?: false
}
```
`recorded()` 매핑: `CachedValue.response→ResponseDto(code,body)`, `value.mock?→MockDto(code,body,duration)|null`.

### upsertMock 설계 (`MemCacheRepoImpl`)
```
override fun upsertMock(method, urlPath, code, body, duration): Boolean {
    val key = CachedKey(method, urlPath)
    val mock = CachedResponse(code, body, duration)
    val existing = _cachedMap[key]
    _cachedMap[key] = existing?.copy(mock = mock)        // 기존이면 response 보존, mock만 갱신
        ?: CachedValue(response = mock, mock = mock)      // 없으면 baseline=mock 으로 생성
    return true
}
```
주의: 외부에서 보내는 `path`는 어댑터 저장키와 동일하게 **쿼리 제외 `encodedPath`**(G1 동결) — contract 예시와 일치, 코드 변경 없음.

### KtorAdapter 결함 수정 (`KtorAdapter.kt`)
- **KA-B(확정: expectSuccess 제거)**: 내부 mock 클라이언트에서 `expectSuccess=true` 삭제 → mock 에러 응답을 그대로 반환. throw 여부는 바깥 클라이언트 validator가 결정.
- **KA-A**: expectSuccess 제거로 `client.request{}`가 에러 코드에 throw하지 않음 → `close()` 정상 도달. 추가 방어로 `try/finally`(per-call client일 때) 적용.
- **KA-D**: 가능하면 내부 `HttpClient(MockEngine)`를 **1회 생성·재사용**(per-call 데이터는 request attribute로 주입)해 mock마다 클라이언트 생성 churn 제거. 재사용 배선이 위험하면 최소안=per-call + `try/finally`(KA-A는 이미 해결). 수동 `HttpClientCall` 구성(Ktor internal API)은 M0에서 비채택.
- **KA-C(본문 비소비 읽기)**: `extractResponseData`에서 `clientResponse.call.save().response.bodyAsText()` 사용 — `save()`가 본문을 ByteArray로 보존한 사본을 반환하므로 원본 채널 미소비 → 앱이 이후 정상 본문 수신. (OkHttp의 `peekBody`와 대칭. 대용량 시 버퍼링은 동일 트레이드오프로 수용.)

### 테스트 (JUnit5 컨벤션)
- `control/ControlServiceTest.kt` — recorded 변환(mock 있음/없음), upsert/unMock/clearAll 위임 검증, sinks 변환, inject 성공/미등록(false). (`CacheRepo`는 mockk, `SinkRegistry`는 실객체+clear)
- `control/SinkRegistryTest.kt` — register/get/unregister/all, 같은 id 재등록=last-wins. (`@BeforeEach` clear)
- `data/repo/MemCacheRepoImplTest.kt`(추가) — upsert: 신규 생성/기존 response 보존+mock 갱신/duration 기본값.
- `data/adapter/KtorAdapterTest.kt`(추가) — `createMockResponse`가 4xx/5xx도 throw 없이 반환(KA-B), 반복 호출 안정(KA-A), `extractResponseData` 후 앱이 본문 재독 가능(KA-C 회귀).
- `client/ktor/OpenMockerPluginG3Test.kt`(신규) + `client/okhttp/OpenMockerInterceptorG3Test.kt`(신규) — record→upsertMock→동일 호출 반복 시 **계속 mock 유지**(cache 재기록 없음) 검증.
- `control/ControlServerTest.kt`(선택, `@Tag("integration")`) — 임시 포트 bind 후 `HttpURLConnection`으로 `GET /rest/recorded` 200 + JSON 확인 후 stop. `readHttpRequest` 순수 파서 단위 테스트 별도.

---

## Part B — 데모앱 curl 배선 (`app/`)

목적: 이 레포만으로 M0를 curl E2E 검증.
- `app/.../di/DemoApplication.kt`: `onCreate`에서 **debug 한정**으로 `OpenMocker.startControlServer()` 호출 + 데모용 `OpenMockerEventSink`(id="demo", preset 1~2개) `registerSink`. (release 가드: `BuildConfig.DEBUG`)
- 데모 weather 호출(Ktor/OkHttp)이 이미 OpenMocker를 타므로 `GET /rest/recorded`에 기록이 잡힘 → curl로 mock/inject 왕복 검증.
- 필요 시 `app/build.gradle.kts`에 `BuildConfig` 활성 확인(이미 Android 기본).

**변경 없음**: 데모 weather 로직·UI·DI 모듈 구조. 제어 서버 start/sink 등록 한 지점만 추가.

> 검증 절차는 아래 Verification 참조.

---

## Part C — M2 플러그인 (제품 수준 구현)

루트 Android 빌드에 include하지 않는 **standalone Gradle 프로젝트**(자체 `settings.gradle.kts`). 이유: 루트 `FAIL_ON_PROJECT_REPOS`와 IntelliJ Platform 리포 충돌 회피 + 안드로이드 개발자 클론 무게 회피. IDE는 두 프로젝트로 따로 import.

**목표 = 사내 배포해 실사용 가능한 완성품.** 단순 골격이 아니라 기기 관리·자동 forward·실시간 기록 폴링·mock 편집/해제/전체삭제·이벤트 발사·설정 영속화·연결 상태 피드백·에러 복원력·패키징/배포까지 포함.

### 빌드 (`plugin/` 루트)
| 파일 | 내용 |
|---|---|
| `plugin/settings.gradle.kts` | standalone, `rootProject.name="openmocker-plugin"`. (루트 settings에 include 안 함) |
| `plugin/build.gradle.kts` | `org.jetbrains.intellij.platform` **2.x** + `org.jetbrains.kotlin.jvm`. `repositories { mavenCentral(); intellijPlatform { defaultRepositories() } }`. `dependencies { intellijPlatform { create("IC", "<버전>"); testFramework(Platform) } }`. `intellijPlatform { pluginConfiguration { ideaVersion { sinceBuild="<AS 대응>"; untilBuild=... } } ; pluginVerification {...} }`. JVM 타깃은 대상 IDE에 맞춤(2024.2+ → 21). **외부 런타임 의존성 0** — HTTP는 Java `HttpClient`, JSON은 플랫폼 번들 Gson 사용. |
| `plugin/gradle.properties`, `plugin/gradle/wrapper/*` | Gradle wrapper |

### 플러그인 디스크립터
| 파일 | 내용 |
|---|---|
| `plugin/src/main/resources/META-INF/plugin.xml` | `<id>`, `<name>`, `<vendor>`, `<depends>com.intellij.modules.platform</depends>`, `<extensions>`: `toolWindow`(id="OpenMocker", anchor="right", factoryClass), `applicationConfigurable`(설정), `applicationService`(설정·세션은 `@Service`로 무등록 가능), `notificationGroup` |
| `plugin/src/main/resources/icons/*` | ToolWindow 아이콘 |

### 코어 (패키지 `net.spooncast.openmocker.plugin`)
| 파일 | 책임 |
|---|---|
| `net/ControlClient.kt` | Java 11 `HttpClient`로 동결 contract 5종 호출(`GET /rest/recorded`, `POST /rest/mock`, `DELETE /rest/mock`, `GET /inject/sinks`, `POST /inject/{id}`). 타임아웃·상태코드·예외 → `Result` 래핑. JSON 직렬/역직렬은 번들 Gson |
| `net/ContractDtos.kt` | contract 미러 모델(`RecordedEntry/ResponseData/MockData`, `MockRequest`, `Sink/Preset`) — `:lib` DTO와 1:1, 단 독립 빌드라 별도 정의 |
| `adb/AdbService.kt` | `adb` 탐색(`ANDROID_HOME`/PATH), `adb devices` 파싱, `adb -s <serial> forward tcp:<port> tcp:<port>`, `--remove`/`forward --list`. `ProcessBuilder` + 타임아웃, 실패 메시지 구조화 |
| `settings/MockerSettings.kt` | `@Service` + `PersistentStateComponent` — port(기본 8099), 마지막 선택 기기 serial, poll 주기, 자동 forward 여부 저장 |
| `settings/MockerConfigurable.kt` | Settings/Preferences UI(포트·폴링 주기·자동 forward) |
| `session/MockerSession.kt` | `@Service`(project) — 연결/포워드 상태, 선택 기기, 폴링 핸들 보유. 상태 변경 리스너 |

### UI (Swing + Kotlin UI DSL)
| 파일 | 책임 |
|---|---|
| `ui/MockerToolWindowFactory.kt` | `ToolWindowFactory` 구현 — 상단 툴바(기기 드롭다운·새로고침·연결상태) + REST/WS 탭 구성, `contentManager`에 부착 |
| `ui/RestPanel.kt` | 기록 테이블(`JBTable`+`RecordedTableModel`: method/path/mocked?) + 주기 폴링/수동 새로고침. 행 선택 → 우측 편집기(status code 필드 + body 텍스트영역) → **저장**(`POST /rest/mock`) / **mock 해제**(`DELETE /rest/mock?method=&path=`) / **전체 clear**(`?all=true`) |
| `ui/RecordedTableModel.kt` | `AbstractTableModel` — recorded 목록 보관·갱신, mocked 여부 표시 |
| `ui/WsPanel.kt` | sink 드롭다운(`GET /inject/sinks`) + 프리셋 버튼(preset.payload 채움) + 자유 텍스트 영역 → **발사**(`POST /inject/{id}` raw body) |
| `ui/StatusBar.kt` | 연결/포워드/서버 상태 배너(연결됨·forward 실패·서버 무응답), 액션 버튼 |

### 라이프사이클 / 스레딩
- ToolWindow 활성 시: 선택 기기로 `adb forward` 보장 → 폴링 시작. 비활성/닫힘 시 폴링 중단.
- 폴링: 백그라운드(`Alarm` 또는 코루틴/`ScheduledExecutorService`)에서 `ControlClient.recorded()` 호출 → **EDT(`ToolWindowManager.invokeLater`)에서 테이블 갱신**. 주기는 설정값(기본 1.5~2s).
- 다기기: 상단 `adb devices` 드롭다운, 변경 시 forward 재설정.
- 에러 복원력: adb 미설치/기기 없음/포트 사용중/서버 무응답 → 배너 + `Notification`로 안내, 폴링은 백오프. 절대 IDE를 막지 않음(전부 비차단).

### 배포
- `./gradlew buildPlugin` → 배포용 zip. **사내 배포**(zip 수동 또는 내부 plugin repo `updatePlugins.xml`). 마켓플레이스 X.
- `verifyPlugin`/`pluginVerification`으로 AS 호환(since/until) 확인. (옵션) `signPlugin`.

### 테스트
- `ControlClientTest` — 로컬 stub HTTP 서버(또는 `:lib` 제어 서버 기동) 상대로 5종 라우트 round-trip.
- `AdbServiceTest` — `adb devices`/`forward --list` 출력 파싱 순수 함수 단위 테스트.
- `RecordedTableModelTest` — 갱신/정렬/mocked 표시 로직.
- (옵션) `MockerSettingsTest` — state 직렬화 round-trip.

---

## Verification (E2E)

1. 빌드/단위테스트: `./gradlew :lib:testDebugUnitTest` (G3 회귀 + ControlService + upsertMock + KtorAdapter 수정 전부 green).
2. 데모앱 설치 후 기기 연결, `adb forward tcp:8099 tcp:8099`.
3. 데모에서 weather 호출 1회 → `curl localhost:8099/rest/recorded` 에 항목 확인.
4. `curl -X POST localhost:8099/rest/mock -d '{"method":"GET","path":"/<path>","code":500,"body":"{}","duration":0}'` → 재호출 시 mock 500 반환, **반복 호출에도 계속 mock**(G3).
5. `curl -X POST localhost:8099/inject/demo -d '<raw payload>'` → 데모 sink `inject` 호출 확인(200) / 미등록 id는 404.
6. `curl -X DELETE 'localhost:8099/rest/mock?method=GET&path=/<path>'` 및 `?all=true` 동작 확인.
7. 플러그인(제품 수준): `plugin/`에서 `./gradlew runIde` → ToolWindow 열림 → 기기 드롭다운에서 기기 선택 시 자동 `adb forward` → REST 탭에 기록 테이블 폴링 표시 → 행 선택해 status/body 편집·저장 → 데모 앱 재호출 시 mock 반영 → WS 탭에서 sink 선택·프리셋/자유텍스트 발사 → 서버 끊김/기기 없음 시 상태 배너·알림 노출. `./gradlew verifyPlugin` 통과, `buildPlugin` zip 생성.

---

## Task 분해 (등록 예정 — 의존은 blockedBy)

M0(critical path: T3→T4→T5):
- **T1** upsertMock(repo): `CacheRepo`+`MemCacheRepoImpl`+repo 테스트. (무의존)
- **T2** EventSink+SinkRegistry: `OpenMockerEventSink.kt`(public)+`SinkRegistry.kt`+테스트. (무의존)
- **T3** DTO+ControlService+테스트. (blockedBy T1,T2)
- **T4** ControlServer(ServerSocket/HTTP 파서)+코루틴 의존성 추가+`readHttpRequest` 테스트. (blockedBy T3)
- **T5** OpenMocker facade 배선. (blockedBy T2,T4)
- **T6** KtorAdapter KA-A/B/C/D 수정+테스트. (무의존, 병렬)
- **T7** G3 회귀 테스트(Ktor+OkHttp). (blockedBy T1)
- **T8** 버전업(`build.gradle.kts:148`)+카탈로그. (blockedBy T5)

데모(Part B):
- **T9** 데모앱 curl 배선(`DemoApplication` debug start+demo sink). (blockedBy T5)

플러그인(Part C, 제품 수준):
- **T10** plugin/ 독립 빌드 셋업: `settings.gradle.kts`+`build.gradle.kts`(IntelliJ Platform 2.x)+wrapper+`plugin.xml`(빈 ToolWindow 등록) → `runIde`로 로드 확인. (blockedBy T8 — contract/버전 안정 후)
- **T11** `ControlClient` + `ContractDtos`(contract 5종 호출, Gson) + round-trip 테스트. (blockedBy T10)
- **T12** `AdbService`(adb 탐색·`devices`·`forward`) + 출력 파싱 테스트. (blockedBy T10)
- **T13** 설정 영속화: `MockerSettings`(`PersistentStateComponent`) + `MockerConfigurable`. (blockedBy T10)
- **T14** REST 패널: `RecordedTableModel`+`RestPanel`(폴링·편집·저장·해제·전체clear). (blockedBy T11, T13)
- **T15** WS 패널: `WsPanel`(sink 드롭다운·프리셋·자유텍스트·발사). (blockedBy T11)
- **T16** 세션/라이프사이클: `MockerSession`+`MockerToolWindowFactory`+상단 기기 드롭다운+`StatusBar`+`Notification` 배선(자동 forward·폴링 시작/중단·에러 복원력). (blockedBy T12, T14, T15)
- **T17** 패키징/배포: `buildPlugin` zip + `pluginVerification`(AS 호환) + 사내 배포 절차 문서화. (blockedBy T16)

병렬 가능: T1·T2·T6 즉시 시작. T7은 T1 후. 데모(T9)는 facade(T5) 후. 플러그인 셋업(T10)은 버전(T8) 후, 그 뒤 T11·T12·T13 병렬 → T14·T15 → T16 → T17.

> **규모 주의**: Part C 제품화로 task가 17개(M0 8 + 데모 1 + 플러그인 8)로 늘었다. 플러그인(T10~T17)은 IntelliJ Platform 학습·AS 호환 검증 비용이 있어 M0/데모와 별도 마일스톤으로 진행 권장.
