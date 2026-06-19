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
