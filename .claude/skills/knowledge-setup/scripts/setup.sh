#!/usr/bin/env bash
# setup.sh — 지식 베이스 + 작업 스킬 오케스트레이션을 대상 프로젝트에 스캐폴드한다.
#
# Usage: setup.sh <project-root>
#   <project-root> : 세팅할 프로젝트 루트 (CLAUDE.md가 있거나 생길 위치)
#
# 동작 (모두 idempotent — 기존 파일은 절대 덮어쓰지 않는다):
#   - <root>/.claude/docs/<category>/README.md        (6개 카테고리 인덱스/템플릿)
#   - <root>/.claude/skills/<skill>/                  (knowledge-update·knowledge-drift +
#                                                      create-ticket·create-branch·create-worktree·
#                                                      create-commit·create-pr·review-commit)
#   - CLAUDE.md 상태를 점검해 다음 단계(섹션 삽입/스켈레톤 생성)를 안내한다.
#     CLAUDE.md 편집 자체는 Claude가 SKILL.md 절차에 따라 수행한다.
#
# 이미 있는 파일/디렉토리는 건드리지 않으므로, 부분 세팅된 프로젝트에 다시 실행해도 안전하다.

set -euo pipefail

if [[ $# -lt 1 ]]; then
    echo "Usage: $0 <project-root>" >&2
    exit 2
fi

ROOT="$1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ASSETS="$SCRIPT_DIR/../assets"

if [[ ! -d "$ROOT" ]]; then
    echo "[오류] 프로젝트 루트를 찾을 수 없음: $ROOT" >&2
    exit 1
fi
ROOT="$(cd "$ROOT" && pwd)"

created=()
skipped=()

# 디렉토리 트리를 복사하되, 대상에 이미 있는 파일은 건너뛴다(사용자 내용 보호).
copy_tree_if_missing() {
    local src="$1" dst="$2"
    while IFS= read -r -d '' f; do
        local rel="${f#"$src"/}"
        local target="$dst/$rel"
        if [[ -e "$target" ]]; then
            skipped+=("${target#"$ROOT"/}")
        else
            mkdir -p "$(dirname "$target")"
            cp "$f" "$target"
            created+=("${target#"$ROOT"/}")
        fi
    done < <(find "$src" -type f -print0)
}

echo "=== knowledge-setup ==="
echo "대상 프로젝트: $ROOT"
echo ""

# 1) 지식 베이스 docs 구조
copy_tree_if_missing "$ASSETS/docs"   "$ROOT/.claude/docs"
# 2) 스킬
copy_tree_if_missing "$ASSETS/skills" "$ROOT/.claude/skills"

if [[ ${#created[@]} -gt 0 ]]; then
    echo "생성됨:"
    printf '  + %s\n' "${created[@]}"
    echo ""
fi
if [[ ${#skipped[@]} -gt 0 ]]; then
    echo "건너뜀(이미 존재):"
    printf '  = %s\n' "${skipped[@]}"
    echo ""
fi

# 3) CLAUDE.md 상태 점검 — 편집은 Claude가 수행
CLAUDE_MD="$ROOT/CLAUDE.md"
echo "--- CLAUDE.md ---"
if [[ ! -f "$CLAUDE_MD" ]]; then
    echo "CLAUDE_MD_STATE=missing"
    echo "→ CLAUDE.md 없음. assets/claude-md/skeleton.md 를 바탕으로 새로 생성하고 두 섹션을 채운다."
else
    has_kb="no"; has_orch="no"
    grep -qE '^##[[:space:]]+지식 베이스' "$CLAUDE_MD" && has_kb="yes"
    grep -qE '^##[[:space:]]+작업 스킬 오케스트레이션' "$CLAUDE_MD" && has_orch="yes"
    echo "CLAUDE_MD_STATE=exists"
    echo "HAS_KNOWLEDGE_BASE_SECTION=$has_kb"
    echo "HAS_ORCHESTRATION_SECTION=$has_orch"
    echo "→ 없는 섹션만 assets/claude-md/ 의 스니펫으로 멱등 삽입한다(이미 있으면 skip)."
fi

echo ""
echo "다음 단계:"
echo "  1) 위 CLAUDE.md 안내에 따라 섹션을 삽입/생성한다."
echo "  2) 정합성 점검: python3 $ROOT/.claude/skills/knowledge-drift/scripts/check_drift.py $ROOT/.claude/docs"
