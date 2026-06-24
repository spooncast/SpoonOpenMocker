# 플러그인 배포는 사내 zip 수동 배포 (마켓플레이스 X, untilBuild 개방형)

- 날짜: 2026-06-24
- 상태: 채택됨

## 맥락
`plugin/`(OpenMocker 제어용 IntelliJ/AS 플러그인)을 실사용 가능하게 배포해야 했다(T17 / #131).
이 플러그인은 사내 QA·개발용 제어 도구로, 외부 사용자를 대상으로 하지 않는다. 배포 채널과
AS/IDEA 호환 상한(`untilBuild`)을 확정해야 했다.

## 결정
- **배포 채널**: 사내 **zip 수동 배포**를 1순위로 한다. `./gradlew buildPlugin` 산출
  `plugin/build/distributions/openmocker-plugin-<version>.zip` 을 팀에 공유하고, 각자
  *Settings → Plugins → Install Plugin from Disk…* 로 설치한다.
- **JetBrains Marketplace 미사용** — `publishPlugin` 을 배선하지 않는다.
- **`untilBuild` 개방형(상한 없음)** — `ideaVersion { sinceBuild = "242"; untilBuild = provider { null } }`.
  sinceBuild=242(AS Ladybug 2024.2+) 이상이면 최신 IDE 에서도 로드 가능.
- 배포 전 점검으로 `pluginVerification { ides { recommended() } }` 를 배선해 `verifyPlugin`(IntelliJ
  Plugin Verifier)을 수동 실행 가능하게 둔다.

## 근거 / 검토한 대안
- **마켓플레이스 vs 사내 zip**: 사내 전용 도구라 공개 마켓이 불필요하고, 심사·공개 노출 부담만 는다 → 사내 zip 채택.
- **`untilBuild` 고정("243.*") vs 개방형**: 고정하면 새 AS/IDEA 가 나올 때마다 상한을 올려 재배포해야 한다.
  사내 도구는 최신 AS 를 빠르게 추종하므로 그 리빌드 부담을 없애려 개방형 채택. 대가로 플랫폼 파괴적
  변경 시 깨질 위험은 감수한다(필요 시 `verifyPlugin` 으로 사전 점검).
- **내부 plugin repo(`updatePlugins.xml`) / `signPlugin`**: IDE 내 자동 업데이트·서명은 유용하나
  현 사용 규모엔 과하다 → 향후 옵션으로 보류.

## 영향
- `plugin/build.gradle.kts` — `untilBuild = provider { null }` + `pluginVerification { ides { recommended() } }`.
- `plugin/src/main/resources/META-INF/plugin.xml` — 0.1.0 `<change-notes>` 추가.
- `README.md` — `## IDE Plugin` 섹션에 "Installing the plugin" + "Building & distributing (internal)" 절 추가.
- 새 AS/IDEA 출시 시 플러그인 재배포 불필요(개방형). 단, 플랫폼 파괴적 변경이 의심되면 `verifyPlugin` 으로 확인 후 배포.
