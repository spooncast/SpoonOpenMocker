# CLAUDE.md

## 프로젝트 개요

OpenMocker는 OkHttp와 Ktor 클라이언트 양쪽의 HTTP 요청을 모킹하는 Android 라이브러리다. 네트워크 요청을 가로채 응답을 캐시하고, 개발·테스트 중 API 응답을 모킹할 수 있게 해준다.

- **`lib/`**: OpenMocker 라이브러리 (배포 아티팩트)
- **`app/`**: 날씨 API 예제로 사용법을 보여주는 데모 앱

## 지식 베이스 (.claude/docs)

프로젝트 지식은 CLAUDE.md에 인라인으로 두지 않고 `.claude/docs/` 아래에 카테고리별로 보관한다. CLAUDE.md는 경로와 "참조 시점"만 안내하며, 실제 내용은 필요할 때 해당 파일을 직접 읽어 로드한다. (자동 인라인되는 `@import`는 사용하지 않는다 — 매 세션 컨텍스트가 비대해지는 것을 막기 위함.)

작업 흐름:
- 작업 전 — 관련 카테고리의 `README.md`(인덱스)를 먼저 훑고, 해당하는 항목 파일만 읽는다.
- 작업 후 — 새 결정/이슈/용어/컨벤션/명세/플로우가 생기면 해당 카테고리에 항목 파일을 추가하고 인덱스 README를 갱신한다.

| 카테고리 | 경로 | 참조 시점 | 기록 시점 |
|---|---|---|---|
| decisions | `.claude/docs/decisions/` | 기술/방향 선택을 할 때 | 결정이 내려졌을 때 |
| troubleshootings | `.claude/docs/troubleshootings/` | 버그·빌드·VOC 이슈를 다룰 때 | 이슈를 해결했을 때(증상/원인/해결) |
| domains | `.claude/docs/domains/` | 낯선 도메인 용어를 만났을 때 | 새 용어가 정의됐을 때 |
| conventions | `.claude/docs/conventions/` | 코드 작성·리뷰 시 | 컨벤션이 합의됐을 때 |
| specs | `.claude/docs/specs/` | 기능을 설계·구현하기 전에 | 기능 요구사항이 정의됐을 때 |
| flows | `.claude/docs/flows/` | 기능을 새로 추가할 때 | 개발 순서/절차가 정해졌을 때 |

각 카테고리 폴더의 `README.md`가 그 카테고리의 인덱스(항목 한 줄 요약 목록)이자 항목 작성 템플릿이다. 카테고리는 확장 가능하다 — 새 카테고리를 추가하면 폴더와 인덱스 `README.md`를 만들고 위 표에 한 행을 추가한다.

지식 베이스 운영은 두 스킬이 짝을 이룬다 — 항목 기록·인덱스 갱신은 `knowledge-update`, 인덱스↔파일 정합성 점검(고아·깨진 링크·네이밍 위반)은 `knowledge-drift`(`python3 .claude/skills/knowledge-drift/scripts/check_drift.py`)를 사용한다.

## 작업 스킬 오케스트레이션

작업 스킬은 완료 시 다음 스킬을 `AskUserQuestion` 으로 추천한다(임의 실행하지 않음). 예외는 `knowledge-update → knowledge-drift` 로, 이 정합성 점검만은 묻지 않고 필수로 수행한다.

```
create-ticket → create-branch ┬→[메인 트리]──────────────────┐
                              └→[새 worktree] create-worktree ┤
create-worktree(직접 진입) ─ 새 브랜치면 create-ticket 선행 ───┘
                                                              ▼
                                       (작업) → create-commit ┬→ review-commit →(수정)→ create-commit
                                                              ├→ knowledge-update ─(필수)→ knowledge-drift
                                                              └→ create-pr
```

- `create-commit` 이 허브다 — 완료 후 multiSelect 로 리뷰 / 지식 기록 / PR 생성을 추천한다.
- `create-branch` 는 완료 시 "어디서 작업할지"를 물어 **메인 트리**면 그대로 작업, **새 worktree**면 `create-worktree` 로 넘긴다. 같은 브랜치를 두 곳에 체크아웃할 수 없으므로 이 분기는 브랜치 생성 *전*에 둔다.
- `create-worktree` 는 `create-branch` 의 한 갈래이자 독립 진입점이다. 새 브랜치인데 티켓 키가 없으면 `create-ticket` 을 선행해 키를 확보한 뒤 브랜치명을 조립한다. 완료 후엔 작업이 끼어들므로 다음 스킬 추천은 없다(`create-branch` 와 동일 성격). (worktree 생성 메커니즘은 프로젝트 특정적이므로 대상 프로젝트에 맞게 적응한다.)
- `review-commit` 은 지적 사항이 있으면 수정 후 `create-commit`(수정분 재커밋)으로, 없으면 `create-pr` 로 추천한다.
- `create-pr`(종착점)은 다음 스킬 추천이 없다.
