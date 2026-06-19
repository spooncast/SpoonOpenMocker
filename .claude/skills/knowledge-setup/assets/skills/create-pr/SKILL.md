---
name: create-pr
description: >-
  현재 브랜치에서 베이스 브랜치로 향하는 Bitbucket Pull Request를, Meta(PR Type) / 문제·신규 기능 설명 /
  적용 버전 / 원인 / 수정·구현 의 정해진 템플릿으로 작성해 생성한다. 리뷰어가 변경 성격과 맥락을 정해진
  자리에서 빠르게 잡도록 하는 것이 목적이다. "PR 만들어줘/생성해줘/올려줘", "pull request",
  "Bitbucket에 올려줘", "/create-pr" 등 PR을 만들려는 의도를 보이면 반드시 사용한다.
---

# create-pr

현재 브랜치를 베이스 브랜치로 머지하기 위한 Bitbucket PR을 일관된 형식으로 작성·생성한다. 핵심은 **본문을 정해진 템플릿(Meta·문제/신규 기능 설명·적용 버전·원인·수정/구현)으로 채우는 것**이다.

**왜 이 구성인가.** PR을 읽는 동료는 보통 시간에 쫓긴다. 맨 위 PR Type 으로 변경 성격(기능/리팩토링/버그/크래시)을 즉시 알리고, *문제 → 원인 → 수정* 흐름으로 *무엇을 왜 어떻게 바꿨는지*를 항상 같은 자리에서 찾게 하면 리뷰 피로가 줄고 머지가 빨라진다. 그래서 매 PR이 동일한 템플릿을 따른다.

## 생성 수단 (우선순위)

PR 생성과 Jira 티켓 조회는 아래 순서로 수단을 고른다. **항상 1번을 먼저 시도하고, 불가할 때만 2번으로 내려간다.**

1. **dexus MCP** (연결돼 있으면 우선)
   - PR 생성/조회: `mcp__dexus__bitbucket_pullrequest_manage` (`action=create` / `action=list` 등)
   - 티켓 조회: `mcp__dexus__jira_issue_manage` (`action=get`)
   - 도구 스키마는 `ToolSearch` 로 `select:mcp__dexus__bitbucket_pullrequest_manage,mcp__dexus__jira_issue_manage` 를 불러와 호출한다.
2. **Bitbucket API 스크립트** (dexus 미연결 시 fallback)
   - PR 생성: `<skill-directory>/scripts/create_bitbucket_pr.py` — `.env` 의 `BITBUCKET_WORKSPACE` / `BITBUCKET_REPO_SLUG` / `BITBUCKET_ACCESS_TOKEN` 필요.
   - 티켓 조회: `<skill-directory>/scripts/fetch_jira_issue.py` — `.env` 의 `JIRA_BASE_URL` / `JIRA_EMAIL` / `JIRA_API_TOKEN` 필요.

> dexus 연결 여부는 `mcp__dexus__*` 도구가 호출 가능한지로 판단한다. 확신이 안 서면 `ToolSearch` 로 위 도구를 시도해보고, 로드되면 dexus 경로, 실패하면 스크립트 경로로 진행한다.
> `<skill-directory>` 는 이 스킬 폴더(`.claude/skills/create-pr/`)다.

## 동작 순서

1. **상태 파악** — 현재 브랜치, 베이스 브랜치, 이 브랜치의 커밋·변경 파일, 원격 추적 상태를 확인한다.
2. **기존 PR 확인** — 같은 브랜치의 열린 PR이 있으면 중복 생성하지 않는다.
3. **연결 티켓 식별** — 브랜치명에서 Jira 티켓 키를 뽑아 해당 티켓의 요약·목적을 가져온다.
4. **본문 작성** — 아래 템플릿 형식으로 초안을 만든다.
5. **초안 확인** — 제목 / 본문 / 생성 수단을 사용자에게 보여주고 승인을 받는다. (승인 전에는 푸시·PR 생성을 하지 않는다.)
6. **PR 생성** — 승인되면 필요 시 브랜치를 푸시한 뒤 dexus 또는 스크립트로 생성하고, PR URL을 돌려준다.

## 1. 상태 파악

PR 본문은 추측이 아니라 **실제 변경**에 근거해야 한다. 먼저 무엇이 바뀌었는지 본다.

먼저 PR을 올릴 **타겟(베이스) 브랜치**를 정한다.

- **대화 중에 타겟 브랜치를 파악했으면, 그 타겟 브랜치로 PR을 생성한다.** (예: 직전 대화에서 `release/11.11.0`로 머지한다고 했으면 그 브랜치를 베이스로 쓴다.)
- 대화에서 파악이 어려우면 아래 순서로 추정하고, 그래도 못 찾으면 사용자에게 물어본다.

```bash
BRANCH=$(git branch --show-current)

# 베이스(분기 시점) 브랜치 추정 — reflog 에서 분기 기록을 찾는다
git reflog show --no-abbrev HEAD | grep "branch: Created from"
```

- **대화 맥락이나 reflog 로 베이스를 찾은 경우**: 해당 브랜치를 베이스로 사용한다.
- **못 찾은 경우**: 사용자에게 묻는다 — "타겟 브랜치를 자동으로 찾지 못했어요. 어떤 브랜치로 PR을 올릴까요? (예: develop, release/x.y.z)"

베이스(`BASE`)가 정해지면 변경을 분석한다.

```bash
git log "$BASE"..HEAD --oneline      # 이 브랜치가 베이스보다 앞선 커밋
git diff "$BASE"...HEAD --stat       # 변경 파일 요약 (3-dot: 분기 이후 변경만)
git status -sb                       # 원격 추적·동기화(ahead/behind) 상태
```

- `$BASE`와 차이가 없으면(앞선 커밋 0개) PR을 만들 게 없다 — 사용자에게 알리고 멈춘다.
- 현재 브랜치가 베이스 브랜치 자체이면 PR을 만들 수 없다. 작업 브랜치로 옮기도록 안내한다.

## 2. 기존 PR 확인

같은 브랜치로 이미 열린 PR이 있는데 또 만들면 중복이 되므로 먼저 조회한다.

- **dexus**: `mcp__dexus__bitbucket_pullrequest_manage` 로 source 브랜치가 현재 브랜치인 열린 PR을 조회한다.
- **스크립트 경로**: 별도 조회 수단이 없으므로, 사용자에게 "이 브랜치로 이미 올린 PR이 있나요?"를 확인하거나 Bitbucket 웹에서 확인하도록 안내한다.

열린 PR이 있으면 새로 만들지 말고 그 PR을 알리고, 본문 갱신이 필요한지 사용자에게 묻는다.

## 3. 연결 티켓 식별

이 레포의 작업 브랜치명은 Jira 티켓 키로 시작한다(예: `AOSTEAM-1500`, `AOSTEAM-1500/feature-login`). 브랜치명에서 `[A-Z]+-[0-9]+` 패턴으로 키를 뽑는다.

```bash
TICKET=$(git branch --show-current | grep -oE '[A-Z]+-[0-9]+' | head -1)
```

티켓 키를 찾으면 요약·목적을 가져온다.
- **dexus**: `mcp__dexus__jira_issue_manage` `action=get` 으로 조회.
- **스크립트**: `python3 <skill-directory>/scripts/fetch_jira_issue.py "$TICKET"` (JSON 출력).

가져온 티켓의 summary 는 **제목**에, description 의 요점은 본문 "문제/신규 기능 설명" 을 쓰는 근거로 삼는다. 티켓 본문을 통째로 복사하지 말고 요점만 옮긴다 — 리뷰어가 길게 읽지 않아도 맥락을 잡게 하는 게 목적이다.

- **브랜치명에 티켓 키가 없거나 조회에 실패하면**: 사용자에게 연결할 티켓이 있는지 짧게 묻는다. 정말 없으면 제목은 작업 성격을 요약해 쓰고, 본문은 아래 템플릿을 그대로 따른다.

## 4. 본문 작성 형식

본문은 아래 템플릿을 그대로 따른다. 섹션 순서·명칭을 바꾸지 않는다.

```markdown
## Meta

PR Type: <feature | refactoring | bug-fix | crash-fix 중 하나>

## 문제/신규 기능 설명

<문제나 구현한 기능에 대한 간단한 설명. 버그·크래시면 재현 시나리오를 함께.>

## 적용 버전

<11.1x.0 형식의 릴리스 버전>

## 원인

<발생 원인. 신규 기능(feature)이면 "신규 기능 — 해당 없음".>

## 수정/구현

<어떻게 수정·구현했는지. 리뷰어가 중점적으로 봐야 할 주요 포인트를 함께 짚는다.>
```

작성 원칙:

- **섹션 명칭·순서는 위 템플릿 그대로 쓴다** — `Meta` / `문제/신규 기능 설명` / `적용 버전` / `원인` / `수정/구현`.
- **PR Type** 은 커밋 타입·티켓 성격에서 고른다: `[feat]`→`feature`, `[refactor]`→`refactoring`, `[fix]`→`bug-fix`, 크래시 수정→`crash-fix`. 애매하면 사용자에게 확인한다.
- **적용 버전** 은 베이스 브랜치에서 끌어온다(예: `release/11.11.0` → `11.11.0`). 추정이 어려우면 사용자에게 묻는다.
- **원인** 은 버그/크래시 수정에만 채운다. 신규 기능이면 "신규 기능 — 해당 없음" 으로 둔다(섹션 자체는 삭제하지 않는다).
- **"문제/신규 기능 설명"·"수정/구현"은 가독성이 전부다.** 파일별로 나열하지 말고 **관심사·기능 단위로 묶고**, 긴 산문 대신 **표와 짧은 불릿**으로 스캔하기 쉽게 쓴다. 티켓 내용을 그대로 되풀이하지 않는다 — 실제 diff·커밋에 근거한다.
- 티켓 키는 **제목**에 노출해 리뷰어가 Jira 로 바로 이동할 수 있게 한다(본문에는 별도 티켓 섹션을 두지 않는다). GitHub 식 `Closes #번호` 는 쓰지 않는다 — Bitbucket/Jira 자동 종료 규칙과 다르다.
- 본문은 한국어로 쓰되, 코드/식별자/명령/경로는 원형 그대로 둔다.
- 자동 생성 푸터(예: "Generated with Claude Code")는 **넣지 않는다.** 본문을 깔끔하게 유지한다.

## 5. 초안 확인

PR을 만들기 전에 **항상** 제목과 본문, 그리고 생성 수단(dexus / 스크립트)·베이스 브랜치를 사용자에게 보여주고 승인을 받는다.

- **제목**: `<티켓 키> <브랜치에 해당하는 티켓의 타이틀>` 형식을 쓴다(예: `AOSTEAM-1500 이미지 검색 API 연동`). 티켓 타이틀은 3단계에서 조회한 티켓의 제목(summary)을 그대로 쓴다. 티켓이 없으면 작업 성격을 요약한 제목을 쓴다.
- **본문**: Meta(PR Type) / 문제·신규 기능 설명 / 적용 버전 / 원인 / 수정·구현.

사용자가 수정을 요청하면 반영해 다시 보여주고, "생성/올려/좋아" 등 명확한 승인이 있을 때만 다음 단계로 넘어간다. 승인 전에는 **푸시도 PR 생성도 하지 않는다** — 둘 다 원격을 바꾸는 동작이라 되돌리기 번거롭다.

## 6. PR 생성

PR은 브랜치가 원격에 있어야 만들 수 있다. 1단계의 `git status -sb`에서 푸시되지 않은 커밋(ahead)이 있거나 업스트림이 없으면 먼저 푸시한다.

```bash
# 업스트림이 없으면
git push -u origin "$BRANCH"
# 이미 추적 중인데 ahead면
git push
```

### 6-A. dexus MCP (우선)

`ToolSearch` 로 `select:mcp__dexus__bitbucket_pullrequest_manage` 를 불러온 뒤 `action=create` 로 호출한다. 전달 값: 제목, 본문(description), source(현재 브랜치), destination(베이스 브랜치). 생성 후 반환된 PR URL 을 사용자에게 전달한다.

### 6-B. 스크립트 (fallback)

본문에 표·코드블록 등 마크다운이 들어가므로 **본문을 임시 파일에 쓴 뒤 전달한다.**

```bash
# 본문을 임시 파일로 작성 (Write 도구 사용): /tmp/create-pr-body.md

python3 <skill-directory>/scripts/create_bitbucket_pr.py \
  --title "<제목>" \
  --description "$(cat /tmp/create-pr-body.md)" \
  --source "<현재 브랜치>" \
  --destination "<베이스 브랜치>"
```

- `.env` 에 `BITBUCKET_WORKSPACE` / `BITBUCKET_REPO_SLUG` / `BITBUCKET_ACCESS_TOKEN` 이 없으면 스크립트가 안내하며 종료한다.
- 생성 후 출력되는 PR URL 을 사용자에게 전달한다.

## 예시

브랜치 `AOSTEAM-1500`(티켓 `AOSTEAM-1500` 과 연결), 베이스 `release/11.11.0`.

**제목:** `AOSTEAM-1500 이미지 검색 API 연동`

```markdown
## Meta

PR Type: feature

## 문제/신규 기능 설명

검색어로 외부 이미지 검색 API를 호출해 결과를 메인 화면 그리드에 표시하는 기능을 추가한다.
스캐폴드 상태이던 앱에서 핵심 가치인 "이미지 검색"이 실제 동작하게 된다.

## 적용 버전

11.11.0

## 원인

신규 기능 — 해당 없음

## 수정/구현

- **네트워크 계층** — `SearchApiService` 인터페이스/구현체를 추가하고 `standardItemsApiHandler` 로 페이징 응답을 매핑.
- **화면** — 검색 결과를 `LazyVerticalGrid` 로 렌더링하고 로딩/빈 상태를 분기.
- 리뷰 포인트: 페이징 토큰 경계 처리(`SearchPagingSource`)와 빈 검색어 가드.
```

"수정/구현"이 파일 목록이 아니라 관심사(네트워크 / 화면)별로 묶이고 리뷰 포인트를 따로 짚은 점에 주목한다 — 리뷰어가 어디를 집중해 볼지 바로 안다. 버그 수정이라면 PR Type 을 `bug-fix` 로 두고 "원인" 에 발생 원인을 채운다.
