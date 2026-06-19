#!/usr/bin/env bash
#
# review-commit 리뷰 범위를 계산하고 아티팩트를 생성한다.
#
# 사용법:
#   collect_diff.sh <base>          # 명시한 base(브랜치/태그/sha) 분기점..HEAD
#   collect_diff.sh <from>..<to>    # 임의 범위
#   collect_diff.sh <from> <to>     # 임의 범위
#
# base 는 반드시 명시한다. git 은 "현재 브랜치가 어느 브랜치에서 분기했는지" 를
# 기록하지 않으므로 자동 추정은 하지 않는다. <base> 한 개만 주면 그 base 와 HEAD 의
# merge-base(분기점)부터 HEAD 까지를 리뷰 범위로 본다 (base 에 쌓인 이후 커밋은 제외).
#
# 출력: 사람이 읽을 요약을 stdout 으로, 상세 아티팩트를 OUT_DIR 에 파일로 남긴다.
#   $OUT_DIR/meta.txt      범위/통계 요약
#   $OUT_DIR/commits.txt   범위 내 커밋 목록
#   $OUT_DIR/files.txt     변경 파일 목록(name-status)
#   $OUT_DIR/diff.patch    전체 통합 diff
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

HEAD_REF="$(git rev-parse --abbrev-ref HEAD)"

usage_err() {
  echo "ERROR: $1" >&2
  echo "" >&2
  echo "base 를 명시해 다시 실행하세요:" >&2
  echo "  collect_diff.sh <base>        예) collect_diff.sh develop" >&2
  echo "  collect_diff.sh <from>..<to>  예) collect_diff.sh HEAD~3..HEAD" >&2
  exit 1
}

# 범위 결정 — base 는 항상 명시 필요
if [[ $# -eq 0 ]]; then
  usage_err "리뷰 기준 base 가 지정되지 않았습니다."
elif [[ $# -ge 2 ]]; then
  FROM="$1"; TO="$2"
elif [[ "$1" == *".."* ]]; then
  FROM="${1%%..*}"; TO="${1##*..}"
  [[ -z "$TO" ]] && TO="HEAD"
else
  # <base> 한 개 → base 와 HEAD 의 분기점부터 HEAD 까지
  BASE_LABEL="$1"
  git rev-parse --verify --quiet "$BASE_LABEL" >/dev/null 2>&1 \
    || usage_err "base '$BASE_LABEL' 를 찾을 수 없습니다 (브랜치/태그/sha 확인)."
  FROM="$(git merge-base HEAD "$BASE_LABEL")"
  TO="HEAD"
fi

git rev-parse --verify --quiet "$FROM" >/dev/null 2>&1 || usage_err "'$FROM' 를 찾을 수 없습니다."
git rev-parse --verify --quiet "$TO"   >/dev/null 2>&1 || usage_err "'$TO' 를 찾을 수 없습니다."

RANGE="${FROM}..${TO}"
COMMIT_COUNT="$(git rev-list --count "$RANGE")"

if [[ "$COMMIT_COUNT" -eq 0 ]]; then
  echo "ERROR: $RANGE 범위에 커밋이 없습니다. 리뷰할 대상이 없습니다." >&2
  exit 2
fi

OUT_DIR="$(mktemp -d "${TMPDIR:-/tmp}/review-commit.XXXXXX")"

git log --no-merges --pretty='%h %s%n    (%an, %ad)' --date=short "$RANGE" > "$OUT_DIR/commits.txt"
git diff --name-status "$FROM" "$TO" > "$OUT_DIR/files.txt"
git diff "$FROM" "$TO" > "$OUT_DIR/diff.patch"

DIFFSTAT="$(git diff --shortstat "$FROM" "$TO")"
FILE_COUNT="$(grep -c . "$OUT_DIR/files.txt" || true)"
DIFF_LINES="$(wc -l < "$OUT_DIR/diff.patch" | tr -d ' ')"

{
  echo "current_branch: $HEAD_REF"
  echo "base: ${BASE_LABEL:-$FROM}"
  echo "range: $RANGE"
  echo "from_sha: $(git rev-parse "$FROM")"
  echo "to_sha: $(git rev-parse "$TO")"
  echo "commit_count: $COMMIT_COUNT"
  echo "file_count: $FILE_COUNT"
  echo "diffstat: $DIFFSTAT"
  echo "diff_lines: $DIFF_LINES"
  echo "out_dir: $OUT_DIR"
} > "$OUT_DIR/meta.txt"

cat "$OUT_DIR/meta.txt"
echo ""
echo "# 변경 파일 ($FILE_COUNT)"
cat "$OUT_DIR/files.txt"
echo ""
if [[ "$DIFF_LINES" -gt 6000 ]]; then
  echo "NOTE: diff 가 ${DIFF_LINES} 줄로 큽니다. 서브에이전트는 diff.patch 전체를 읽기보다"
  echo "      files.txt 를 보고 변경 핵심 파일을 직접 열어 확인하는 편이 토큰 효율적입니다."
fi
