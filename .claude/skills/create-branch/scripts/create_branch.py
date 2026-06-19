#!/usr/bin/env python3
"""베이스 브랜치에서 지정한 이름으로 새 작업 브랜치를 생성하고 체크아웃하는 스크립트"""

import subprocess
import sys


def run_git(args: list[str]) -> str:
    result = subprocess.run(
        ["git"] + args,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        raise RuntimeError(f"git {' '.join(args)} 실패:\n{result.stderr.strip()}")
    return result.stdout.strip()


def create_branch(base_branch: str, branch_name: str) -> None:
    # 베이스 브랜치 최신화
    print(f"📥 '{base_branch}' 브랜치를 fetch합니다...")
    run_git(["fetch", "origin", base_branch])

    # 새 브랜치 생성 + 체크아웃
    # --no-track: 새 브랜치 upstream 을 origin/<base> 로 설정하지 않는다.
    # 이걸 빼면 인자 없는 `git push` (IDE 단축키 포함) 가 base 브랜치로 직행하는
    # 사고가 발생할 수 있다.
    print(f"🌿 '{base_branch}'에서 '{branch_name}' 브랜치를 생성합니다...")
    run_git(["checkout", "-b", branch_name, "--no-track", f"origin/{base_branch}"])

    print(f"✅ '{branch_name}' 브랜치로 체크아웃 완료!")
    print(f"ℹ️  PR 을 위해 처음 push 할 때는 'git push -u origin {branch_name}' 으로 upstream 을 자기 자신에게 묶어주세요.")


def main():
    if len(sys.argv) != 3:
        print(f"사용법: {sys.argv[0]} <베이스_브랜치> <브랜치명>")
        print(f"  예시: {sys.argv[0]} main feature/42-image-search-api")
        sys.exit(1)

    base_branch = sys.argv[1]
    branch_name = sys.argv[2]

    try:
        create_branch(base_branch, branch_name)
    except RuntimeError as e:
        print(f"❌ 오류: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
