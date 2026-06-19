#!/usr/bin/env python3
"""지식 베이스(.claude/docs) 드리프트 점검.

README.md가 있는 각 카테고리 폴더에 대해 다음을 검사한다:
  - 고아 파일: 디스크에는 있으나 인덱스 표에 없는 항목 파일
  - 깨진 링크: 인덱스 표에는 있으나 디스크에 없는 파일
  - 네이밍 위반: kebab-case가 아닌 파일명

인덱스 표는 각 README.md의 "## 인덱스" 섹션(다음 "## " 헤딩 직전까지)에서 읽는다.
사용법:  python3 check_drift.py [docs_경로]   (기본값: .claude/docs)
종료 코드: 문제 없음 0, 문제 있음 1, 경로 오류 2.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

KEBAB = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*$")
MD_REF = re.compile(r"([A-Za-z0-9._-]+\.md)")


def index_block(readme_text: str) -> list[str]:
    """README에서 '## 인덱스' 섹션의 줄들만 추출(다음 '## ' 헤딩 전까지)."""
    lines = readme_text.splitlines()
    out: list[str] = []
    in_index = False
    for line in lines:
        if re.match(r"^##\s+인덱스", line):
            in_index = True
            continue
        if in_index and re.match(r"^##\s+", line):
            break
        if in_index:
            out.append(line)
    return out


def indexed_files(readme_text: str) -> set[str]:
    """인덱스 표 데이터 행의 첫 칼럼('파일')에서 참조하는 .md 파일명 집합.

    파일명은 첫 칼럼에만 들어가므로 거기서만 추출한다. 요약 칼럼 등 다른 칸에
    'CLAUDE.md' 같은 텍스트가 섞여 있어도 파일 참조로 오인하지 않도록 한다.
    """
    files: set[str] = set()
    for line in index_block(readme_text):
        s = line.strip()
        if not s.startswith("|"):
            continue
        cells = [c.strip() for c in s.strip("|").split("|")]
        if not cells or cells[0] == "파일":  # 빈 행/헤더 행
            continue
        if set(s) <= set("|-: "):  # 구분선 행
            continue
        for ref in MD_REF.findall(cells[0]):
            if ref.lower() != "readme.md":
                files.add(ref)
    return files


def check_category(cat_dir: Path) -> dict[str, list[str]]:
    readme = (cat_dir / "README.md").read_text(encoding="utf-8")
    actual = {p.name for p in cat_dir.glob("*.md") if p.name != "README.md"}
    indexed = indexed_files(readme)
    return {
        "orphans": sorted(actual - indexed),
        "broken": sorted(indexed - actual),
        "bad_names": sorted(f for f in actual if not KEBAB.match(Path(f).stem)),
    }


def main(argv: list[str]) -> int:
    docs = Path(argv[1]) if len(argv) > 1 else Path(".claude/docs")
    if not docs.is_dir():
        print(f"[오류] 지식 베이스 경로를 찾을 수 없음: {docs}")
        return 2

    categories = sorted(
        d for d in docs.iterdir() if d.is_dir() and (d / "README.md").exists()
    )
    if not categories:
        print(f"[경고] README.md를 가진 카테고리 폴더가 없음: {docs}")
        return 0

    print(f"지식 베이스 드리프트 점검: {docs}\n")
    total = 0
    labels = {
        "orphans": "고아 파일(인덱스에 없음)",
        "broken": "깨진 링크(파일 없음)",
        "bad_names": "네이밍 위반(kebab-case 아님)",
    }
    for cat in categories:
        res = check_category(cat)
        n = sum(len(v) for v in res.values())
        total += n
        print(f"## {cat.name} — {'OK' if n == 0 else f'문제 {n}건'}")
        for key, label in labels.items():
            if res[key]:
                print(f"  - {label}:")
                for f in res[key]:
                    print(f"      {f}")
        print()

    print(f"총 문제: {total}건")
    return 1 if total else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
