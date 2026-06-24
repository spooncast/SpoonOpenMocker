# WebSocket 메시지 연동 데모(플러그인 Inject 관찰) 명세

- 상태: 구현 완료
- 갱신일: 2026-06-24
- 관련: [flows/wire-sink-to-app-feature](../flows/wire-sink-to-app-feature.md)

## 목적
"IDE 플러그인으로 WebSocket 메시지 테스트가 가능한가"를 데모 앱에서 **눈으로 확인**할 수 있게 한다.
기존 `DemoEventSink` 는 주입 payload 를 `Log` 로만 출력해 제어 서버 → sink 도달 경로만 증명했고,
화면에서 메시지가 흐르는 것을 볼 수 없었다. 이 기능은 sink 주입을 **실제 WS 메시지 스트림**으로
이어 데모 화면(Realtime 탭)에 실시간 표시함으로써, 플러그인 Inject → 앱 수신 경로를 관찰 가능하게 만든다.

## 핵심 설계 — seam 하나로 운영/모킹이 갈린다
앱에 "실제 WS 클라이언트처럼 보이는" 추상화 `ChatSocketClient`(인터페이스)를 둔다.
- **운영 구현(가정)**: 실제 소켓을 열어 `onMessage` 프레임을 `incoming` Flow 로 forward.
- **데모 구현(`DemoChatSocketClient`)**: OpenMocker sink(`WsEventSink`)가 주입한 payload 를
  `emit()` 으로 같은 `incoming` Flow 에 밀어넣는다.

UI(`WsViewModel`)는 어느 구현인지 모른 채 `incoming` 만 구독하므로, **같은 seam(Flow)** 위에서
실제 소켓과 모킹이 교체된다.

```
[Plugin WsPanel] → POST /inject/demo → SinkRegistry
  → WsEventSink.inject(payload) → DemoChatSocketClient.emit(payload)
    → incoming: SharedFlow<String> → WsViewModel → WsPane(메시지 리스트)
```

## 사용자 시나리오
- 개발자가 IDE 플러그인 WsPanel 에서 sink 를 고르고 payload 를 Inject 하면, 데모 앱 Realtime 탭에
  해당 메시지가 즉시 나타난다.
- `curl -X POST /inject/demo` 로도 동일하게 메시지를 주입해 화면에서 확인할 수 있다(헤드리스 E2E).

## 기능 요구사항
1. `ChatSocketClient` seam: `incoming: SharedFlow<String>`, `connected: StateFlow<Boolean>`, `connect()/disconnect()`.
2. `DemoChatSocketClient`(`@Singleton`): `emit(payload)` 가 `incoming` 으로 메시지를 흘려보낸다. sink/ViewModel 이 같은 싱글톤 공유.
3. `WsEventSink`(`OpenMockerEventSink`, id=`"demo"`): `inject(payload)` → `client.emit(payload)`. `name="Realtime (WebSocket) Demo"`, preset 제공.
4. `DemoApplication`(debug): `EntryPointAccessors.fromApplication` 으로 싱글톤 client 를 꺼내 `WsEventSink` 등록.
5. `WsViewModel`/`WsPane`: `incoming` 구독 → 메시지 리스트 누적 표시, 연결상태 칩, Clear.
6. `MainActivity`: `TabRow`(Weather | Realtime) 로 두 화면 전환.

## 화면 / UX
Realtime 탭: 상단 연결상태 칩(Connected/Disconnected) + 수신 건수 + Clear 버튼,
본문은 수신 메시지 `LazyColumn`(최신으로 자동 스크롤), 비어 있으면 안내문.

## 범위 밖 (Out of scope)
- **라이브러리는 실제 WebSocket 프레임을 가로채지 않는다.** HTTP 처럼 요청 경로에 앉는 인터셉터가 아니라,
  sink 는 **out-of-band 주입 통로**다. 해석/모킹 책임은 전적으로 앱(sink 구현)에 있다.
- 운영용 실제 소켓 구현체는 만들지 않는다(seam 자리만 문서화).
- payload 스키마 해석/검증 없음 — raw 문자열을 그대로 표시한다.

## 미해결 질문
- (없음) 실제 소켓 인터셉션이 라이브러리 기능으로 필요해지면 별도 spec/decision 으로 다룬다.
