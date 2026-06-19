---
name: create-worktree
description: >-
  spooncast-android 에서 별도 작업 공간이 필요할 때 git worktree 를 만들고, gitignored 인
  secrets.properties·local.properties 를 복사해 gradle 이 바로 빌드되는 상태로 만든다 (없으면
  settings.gradle.kts 에서 NPE 로 빌드 실패). "worktree 만들어줘", "워크트리 생성/따줘", "다른
  브랜치 병렬로 작업할 공간", "이 티켓 worktree 로 띄워줘", "/create-worktree" 처럼 워크트리를 새로
  만들려는 의도를 보이면 사용한다. 현재 작업 트리를 건드리지 않고 다른 브랜치를 병렬로 펼치고 싶을 때 적합하다.
---

# Create Worktree

spooncast-android 용 git worktree 를 만들고, gradle 이 바로 동작하도록 gitignored 설정 파일까지 복사하는 스킬.

## 왜 이 스킬이 필요한가

`git worktree add` 만으로는 git-ignored 파일이 새 worktree 로 따라오지 않는다. 이 프로젝트에서는 `secrets.properties` 가 없으면 `settings.gradle.kts` 평가 단계에서 `JFROG_USERNAME` 이 null → `NullPointerException` 으로 **빌드가 시작도 못 하고 실패**한다. `local.properties` 가 없으면 SDK 경로가 빠진다. 그래서 worktree 생성과 설정 복사를 한 흐름으로 묶는다.

## 입력 결정 — 어떤 브랜치를 펼칠 것인가

worktree 의 핵심은 **브랜치명**이다. worktree 폴더 경로는 브랜치 슬러그에서 자동 파생되므로(`<repo-부모>/spooncast-android-worktrees/<슬러그>`, `/`→`_`) 결정·확인 대상이 아니다 — `--path` 로만 덮어쓴다. 그래서 정할 것은 "어떤 브랜치를 펼칠지" 하나다.

### 호출 출처부터 본다

- **`create-branch` 에서 위임돼 온 경우** (사용자가 거기서 "새 worktree" 를 고름): 브랜치명·베이스가 이미 확정돼 있다. 아래 모드 판별·티켓 분기를 **건너뛰고** 곧장 "사용법"의 생성 단계로 간다. (같은 결정을 두 번 묻지 않기 위함.)
- **직접 호출된 경우**: 아래 순서로 브랜치를 정한다.

### 브랜치 정하기 (직접 호출)

1. **현재 세션 맥락에서 유추한다.** 직전 대화에 티켓 키(`AOSTEAM-XXXX`)나 대상 브랜치가 드러났으면 그것을 후보로 삼는다. 브랜치명 자체는 따로 컨펌하지 않는다 — 슬러그는 아래 규칙으로 자동 확정하고(브랜치는 싸고 rename 가능), 사용자가 명시적으로 다른 슬러그를 원하면 그때 반영한다.
2. **유추가 어려우면 `AskUserQuestion` 으로 물어본다.** 임의로 브랜치나 베이스를 가정하지 않는다. 물어볼 것:
   - 기존 브랜치를 펼칠지, 새 브랜치를 만들지
   - (새 브랜치면) 베이스 브랜치와 티켓 키/요약
   - (기존 브랜치면) 브랜치명

### 두 가지 모드

- **기존 브랜치 펼치기** — 이미 있는 로컬/원격 브랜치를 새 worktree 에 체크아웃한다. 인자: 브랜치명만.
- **새 브랜치 만들기** — 베이스에서 `<티켓키>/<영문요약>` 새 브랜치를 만들어 펼친다. 인자: 새 브랜치명 + `--base <베이스>`.
  - 새 브랜치명 슬러그 규칙은 `create-branch` 스킬과 동일하다(snake_case 영문, 2~5단어, 동작 중심). 그 컨벤션을 따른다.
  - 베이스 브랜치는 가정하지 않는다 — 맥락에서 파악하거나 사용자에게 묻는다. (아직 master 에 머지 안 된 기능 위에서 작업하려면 그 통합 브랜치를 베이스로 써야 origin/master 에 없는 코드가 포함된다.)
  - **티켓 키가 없으면** — 팀 컨벤션상 작업 브랜치는 `<티켓키>/...` 형식이다. 키가 없을 땐 자동으로 만들지 말고 `AskUserQuestion` 으로 "이 브랜치에 티켓을 붙일지" 묻는다.
    - **붙인다** → `create-ticket` 으로 먼저 티켓을 만들어(부모·제목 등 입력을 받아) 키를 확보한 뒤 `<티켓키>/<영문요약>` 을 조립한다.
    - **안 붙인다** → chore/실험 등 키 없는 브랜치명으로 진행한다.

## 사용법

브랜치/모드를 확정하고 사용자 확인을 받은 뒤 번들 스크립트를 실행한다:

```bash
# 기존 브랜치 펼치기
python3 <skill-directory>/scripts/create_worktree.py <브랜치명> --verify

# 새 브랜치 만들기 (베이스에서 분기)
python3 <skill-directory>/scripts/create_worktree.py <티켓키>/<영문요약> --base <베이스> --verify
```

**예시:**
```bash
python3 <skill-directory>/scripts/create_worktree.py AOSTEAM-6841/fix_fragment_crash --base release/11.11.0 --verify
```

스크립트가 하는 일(순서대로):
1. worktree 경로 결정 — 전용 폴더 `<repo-부모>/spooncast-android-worktrees/<브랜치슬러그>` (브랜치의 `/` 는 `_` 로 치환). `--path` 로 덮어쓸 수 있다.
2. worktree 생성 — 새 브랜치 모드면 `origin/<베이스>` fetch 후 `-b` 로 분기, 기존 브랜치 모드면 로컬 ref 우선·없으면 원격 추적.
3. **설정 복사** — 메인 repo 루트의 `secrets.properties`·`local.properties` 를 worktree 로 복사 (이 스킬의 핵심).
4. `--verify` 시 `./gradlew help` 로 설정이 정상인지 회귀 확인 (첫 실행은 느릴 수 있음).
5. 생성된 worktree 경로와 `cd` 명령을 출력.

## 검증(`--verify`)에 대해

`--verify` 는 전체 빌드가 아니라 `./gradlew help` 로 **설정 단계만** 돌려 설정 복사가 제대로 됐는지 가볍게 확인하는 것이다. NPE 류 설정 오류는 이 단계에서 바로 드러난다. 다만 새 worktree 는 gradle 캐시가 없어 첫 실행이 느릴 수 있으니, 빠른 생성만 원하면 `--verify` 를 빼도 된다.

## 생성 후 안내

worktree 가 만들어졌으면 다음 중 하나로 인계한다:

- **경로만 알려주면 되는 경우** — 스크립트가 출력한 `cd <경로>` 를 사용자에게 그대로 안내한다. 사용자가 자기 셸에서 이동해 작업한다.
- **이 세션에서 바로 그 worktree 로 작업을 이어갈 경우** — `EnterWorktree` 도구에 worktree **경로**(`name` 이 아니라 `path`)를 넘겨 세션을 그 안으로 전환한다. 스크립트가 만든 worktree 는 `git worktree list` 에 등록돼 있어 `path` 로 진입할 수 있다. `path` 진입은 **기존 worktree 로 들어가는 것**이라 새로 만들지 않는다(`name` 은 origin/기본브랜치에서 새 worktree 를 생성하므로 여기선 쓰지 않는다). 빠져나올 때도 이 worktree 를 지우지 않는다.

> 스크립트 자체는 셸의 작업 디렉터리를 바꿀 수 없다 — 이 세션을 worktree 로 옮기는 것은 위 `EnterWorktree(path)` 로만 가능하다.

## 주의사항

- git 저장소 안에서 실행해야 한다.
- 같은 로컬 브랜치는 두 worktree 에 동시에 체크아웃할 수 없다(git 제약). 이미 어딘가 체크아웃된 브랜치면 git 이 오류를 낸다 — 사용자에게 알린다.
- 대상 경로가 이미 존재하면 스크립트가 중단한다. 다른 `--path` 를 쓰거나 기존 worktree 를 `git worktree remove` 로 정리하도록 안내한다.
- 메인 repo 에 `secrets.properties`/`local.properties` 자체가 없으면 복사할 수 없다 — 경고를 출력하니 사용자에게 확인을 요청한다.
