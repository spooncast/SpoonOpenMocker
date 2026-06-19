---
name: knowledge-setup
description: >-
  `.claude/docs/` 지식 베이스(6개 카테고리: decisions·troubleshootings·domains·conventions·specs·flows)
  와 작업 스킬 오케스트레이션(create-ticket → create-branch → create-commit → review-commit /
  knowledge-update / create-pr)을 한 프로젝트에 **초기 세팅**하는 스킬. 카테고리별 README(인덱스+
  템플릿)와 knowledge-update·knowledge-drift·create-* 스킬을 복사하고, CLAUDE.md가 없으면 스켈레톤을
  만들고 있으면 "지식 베이스"·"작업 스킬 오케스트레이션" 섹션만 멱등 삽입한다. 자기완결형(assets 번들)
  이라 **다른 프로젝트에서도 그대로 세팅**할 수 있다. "지식 베이스 세팅", "docs 구조 만들어줘",
  "이 시스템 다른 프로젝트에 깔아줘", "knowledge setup", "CLAUDE.md 처음 만들어줘", "오케스트레이션
  세팅", "knowledge-setup" 같이 **최초 1회 부트스트랩**을 요청할 때 반드시 사용한다. 항목 추가는
  knowledge-update, 정합성 점검은 knowledge-drift 가 담당한다.
---

# knowledge-setup

지식 베이스(`.claude/docs/`)와 작업 스킬 오케스트레이션을 대상 프로젝트에 **한 번에 세팅**하는
부트스트랩 스킬이다. 평소의 항목 추가/점검이 아니라 "이 시스템을 새 프로젝트에 깔기" 위한 것이다.

## 설계 원칙 (왜 이렇게 동작하는가)

- **자기완결형(self-contained).** 다른 프로젝트에서 실행해도 동작해야 하므로, 세팅에 필요한 모든
  것(카테고리 README, 8개 스킬, CLAUDE.md 스니펫)을 이 스킬의 `assets/` 안에 번들로 들고 있다.
  현재 저장소를 참조하지 않는다. → assets 는 **스냅샷**이다. 원본 스킬/문서가 바뀌면 이 스킬을
  다시 만든 프로젝트에서 `assets/` 도 갱신해야 최신이 된다.
- **절대 덮어쓰지 않는다(idempotent).** 이미 있는 파일·스킬·CLAUDE.md 섹션은 건너뛴다. 부분적으로
  세팅된 프로젝트에 다시 실행해도 안전하다 — 사용자가 채운 내용을 망가뜨리지 않는 것이 최우선이다.
- **기계적인 복사는 스크립트가, 판단이 필요한 병합은 Claude 가.** docs/스킬 복사는 `setup.sh` 가
  멱등하게 처리한다. 임의의 기존 CLAUDE.md 에 섹션을 끼워 넣는 일은 위치·형식 판단이 필요하므로
  스크립트가 상태만 보고하고, 실제 편집은 아래 절차에 따라 Claude 가 수행한다.

## 세팅되는 것

| 대상 | 내용 |
|---|---|
| `.claude/docs/` | decisions · troubleshootings · domains · conventions · specs · flows 6개 폴더, 각 `README.md`(인덱스 + 항목 템플릿 + 네이밍 규칙). 인덱스는 비어 있는 상태(`_(아직 항목 없음)_`)로 시작. |
| 지식 스킬 | `knowledge-update`(기록·인덱스 갱신), `knowledge-drift`(정합성 점검 + `scripts/check_drift.py`) |
| 오케스트레이션 스킬 | `create-ticket` · `create-branch` · `create-worktree` · `create-commit` · `create-pr` · `review-commit` |
| `CLAUDE.md` | "지식 베이스" 섹션 + "작업 스킬 오케스트레이션" 섹션 |

## 절차

1. **대상 프로젝트 루트 확정.** 기본은 현재 작업 디렉토리. 사용자가 다른 경로를 지정했거나 현재
   위치가 의도와 다를 수 있으면 확인한다. (이 스킬을 보유한 프로젝트 자체에 다시 거는 경우는 거의
   없다 — 보통 새/다른 프로젝트가 대상이다.)

2. **스캐폴드 실행.** docs 구조와 8개 스킬을 멱등 복사한다:

   ```bash
   bash .claude/skills/knowledge-setup/scripts/setup.sh <project-root>
   ```

   출력의 `생성됨`/`건너뜀` 목록과 `--- CLAUDE.md ---` 섹션의 상태값
   (`CLAUDE_MD_STATE`, `HAS_KNOWLEDGE_BASE_SECTION`, `HAS_ORCHESTRATION_SECTION`)을 확인한다.

3. **CLAUDE.md 처리.** 스크립트가 보고한 상태에 따라 분기한다. 스니펫 원문은
   `assets/claude-md/` 에 있다(`knowledge-base-section.md`, `orchestration-section.md`, `skeleton.md`).

   - **`CLAUDE_MD_STATE=missing`** → `skeleton.md` 를 바탕으로 `<root>/CLAUDE.md` 를 새로 만든다.
     맨 위 제목/한 줄 요약은 대상 프로젝트에 맞게 채우고, `<!-- KNOWLEDGE_BASE_SECTION -->` 자리에
     `knowledge-base-section.md`, `<!-- ORCHESTRATION_SECTION -->` 자리에 `orchestration-section.md`
     내용을 넣는다(주석 마커는 지운다). 프로젝트 고유 정보(스택/빌드 명령 등)는 추측해 채우지 말고,
     사용자가 알려주면 덧붙이거나 사용자가 이후 작성하도록 둔다.

   - **`CLAUDE_MD_STATE=exists`** → 없는 섹션만 추가한다(멱등).
     - `HAS_KNOWLEDGE_BASE_SECTION=no` 이면 `knowledge-base-section.md` 를,
       `HAS_ORCHESTRATION_SECTION=no` 이면 `orchestration-section.md` 를 파일 끝에 덧붙인다.
     - 이미 `yes` 인 섹션은 손대지 않는다(사용자가 수정했을 수 있으므로 덮어쓰지 않는다).
     - 모듈별 가이드 등 기존 내용은 그대로 둔다. 섹션 사이 빈 줄 한 줄을 유지해 가독성을 지킨다.

4. **정합성 점검.** 세팅 직후 드리프트가 없는지 확인한다:

   ```bash
   python3 <project-root>/.claude/skills/knowledge-drift/scripts/check_drift.py <project-root>/.claude/docs
   ```

   비어 있는 인덱스 6개에 대해 "정합성 이상 없음(총 문제 0건)"이 나오는 것이 정상이다.

5. **보고.** 무엇이 생성/건너뛰어졌는지, CLAUDE.md 를 어떻게 처리했는지, 드리프트 점검 결과를
   간단히 정리한다. 이어서 사용자가 곧바로 쓸 수 있도록 안내한다: 이제 `knowledge-update` 로 항목을
   기록하고, 작업은 `create-ticket → create-branch → create-commit → create-pr` 흐름으로 진행하면
   된다고. (오케스트레이션 스킬 일부는 Jira/Bitbucket/dexus 에 종속적이므로, 대상 프로젝트에서
   토큰·프로젝트 키 등 환경 설정이 더 필요할 수 있음을 함께 알린다.)

## 주의

- 이 스킬은 **부트스트랩 전용**이다. 세팅이 끝난 뒤 항목을 기록할 때는 `knowledge-update`,
  인덱스 정합성을 볼 때는 `knowledge-drift` 를 쓴다 — 이 스킬을 반복 호출하지 않는다.
- 복사된 오케스트레이션 스킬(create-*)은 외부 서비스에 종속적이다. 대상 프로젝트의 워크플로가
  다르면(예: GitHub PR, 다른 이슈 트래커) 해당 스킬을 그 프로젝트에 맞게 수정해야 한다.
- 특히 `create-worktree` 는 번들 중 가장 프로젝트 특정적이다(spooncast-android 의 gradle·
  `secrets.properties`·`local.properties` 복사 전제). 다른 프로젝트에선 복사할 설정 파일과 빌드 검증
  방식을 그 프로젝트에 맞게 고치거나, 필요 없으면 제거한다.
