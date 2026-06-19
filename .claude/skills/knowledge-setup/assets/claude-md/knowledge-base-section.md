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
