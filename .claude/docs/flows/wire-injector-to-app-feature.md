# OpenMocker event injector 를 앱 기능에 잇는 절차

- 적용 시점: HTTP 범위 밖 이벤트(WebSocket/WALA 등)를 플러그인 Inject 로 모킹/시연하려 할 때.
  즉 `POST /inject/{id}` 주입을 앱의 실제 기능·화면으로 흘려보내고 싶을 때.

## 단계
1. **seam 인터페이스 정의** — 수신 스트림 계약을 만든다(예: `ChatSocketClient`).
   `incoming: SharedFlow<T>` + 연결상태 + connect/disconnect. UI 는 이 계약만 구독한다.
2. **데모 구현(@Singleton)** — `incoming` 을 채울 `MutableSharedFlow` 와 `emit(payload)` 진입점을 둔다.
   운영이라면 실제 소켓 `onMessage` 가, 데모라면 injector 가 `emit` 을 호출한다. KDoc 에 그 자리를 명시.
   `@Singleton` 이어야 주입 측(injector)과 구독 측(ViewModel)이 같은 인스턴스를 공유한다.
3. **injector 구현** — `BufferedEventInjector` 를 상속(권장)해 `deliver(payload)` 에서 `client.emit(payload)`
   호출. 수신 이력 버퍼·sequence 채번은 베이스가 처리한다. 버퍼링이 필요 없으면 `OpenMockerEventInjector`
   를 직접 구현해도 된다. `id` 는 `POST /inject/{id}` 의 `{id}` 와 매칭(기존 호환 위해 안정적으로 유지)하고,
   URL 경로 안전 문자(`[A-Za-z0-9._-]`)만 쓴다.
4. **Application 배선(debug)** — `OpenMocker.control.start()` 후, Hilt 그래프 밖에서
   `EntryPointAccessors.fromApplication(this, EntryPoint::class.java)` 로 싱글톤 client 를 꺼내
   `OpenMocker.control.registerInjector(MyInjector(client))`. (Application.onCreate 은 @Inject 불가하므로 EntryPoint 필수.)
5. **구독 측 ViewModel/UI** — `incoming` 을 `viewModelScope` 에서 collect 해 화면 상태로 누적, 화면 표시.

## 체크리스트
- [ ] seam 은 UI 가 구현 종류를 모르도록 인터페이스로 분리했나
- [ ] 데모 구현이 `@Singleton` 이고, injector·ViewModel 이 같은 인스턴스를 공유하나
- [ ] injector `id` 가 제어 contract(`/inject/{id}`)와 기존 E2E 와 호환되고, URL 안전 문자만 쓰나
- [ ] Application 에서 `EntryPointAccessors` 로 싱글톤을 꺼내 등록했나(직접 `new` 한 의존을 넘기지 않았나)
- [ ] E2E: `curl POST /inject/{id}` 와 플러그인 Inject 양쪽으로 화면 반영 확인했나
