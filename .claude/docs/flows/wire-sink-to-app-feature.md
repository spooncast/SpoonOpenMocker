# OpenMocker inject sink 를 앱 기능에 잇는 절차

- 적용 시점: HTTP 범위 밖 이벤트(WebSocket/WALA 등)를 플러그인 Inject 로 모킹/시연하려 할 때.
  즉 `POST /inject/{id}` 주입을 앱의 실제 기능·화면으로 흘려보내고 싶을 때.

## 단계
1. **seam 인터페이스 정의** — 수신 스트림 계약을 만든다(예: `ChatSocketClient`).
   `incoming: SharedFlow<T>` + 연결상태 + connect/disconnect. UI 는 이 계약만 구독한다.
2. **데모 구현(@Singleton)** — `incoming` 을 채울 `MutableSharedFlow` 와 `emit(payload)` 진입점을 둔다.
   운영이라면 실제 소켓 `onMessage` 가, 데모라면 sink 가 `emit` 을 호출한다. KDoc 에 그 자리를 명시.
   `@Singleton` 이어야 주입 측(sink)과 구독 측(ViewModel)이 같은 인스턴스를 공유한다.
3. **sink 구현** — `OpenMockerEventSink` 를 구현해 `inject(payload)` 에서 `client.emit(payload)` 호출.
   `id` 는 `POST /inject/{id}` 의 `{id}` 와 매칭(기존 호환 위해 안정적으로 유지). preset 제공.
4. **Application 배선(debug)** — `OpenMocker.startControlServer()` 후, Hilt 그래프 밖에서
   `EntryPointAccessors.fromApplication(this, EntryPoint::class.java)` 로 싱글톤 client 를 꺼내
   `OpenMocker.registerSink(MySink(client))`. (Application.onCreate 은 @Inject 불가하므로 EntryPoint 필수.)
5. **구독 측 ViewModel/UI** — `incoming` 을 `viewModelScope` 에서 collect 해 화면 상태로 누적, 화면 표시.

## 체크리스트
- [ ] seam 은 UI 가 구현 종류를 모르도록 인터페이스로 분리했나
- [ ] 데모 구현이 `@Singleton` 이고, sink·ViewModel 이 같은 인스턴스를 공유하나
- [ ] sink `id` 가 제어 contract(`/inject/{id}`)와 기존 E2E 와 호환되나
- [ ] Application 에서 `EntryPointAccessors` 로 싱글톤을 꺼내 등록했나(직접 `new` 한 의존을 넘기지 않았나)
- [ ] E2E: `curl POST /inject/{id}` 와 플러그인 Inject 양쪽으로 화면 반영 확인했나
