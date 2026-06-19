#!/usr/bin/env python3
"""Create a git worktree for spooncast-android and make it gradle-ready.

A fresh `git worktree` does not receive git-ignored files, so this project's
`secrets.properties` / `local.properties` are missing and gradle fails at
`settings.gradle.kts` evaluation (JFROG_USERNAME NullPointerException) before
any build even starts. This script creates the worktree and copies those
config files over so the worktree builds immediately.

Two modes:
  - Existing branch:  create_worktree.py <branch>
  - New branch:       create_worktree.py <ticket>/<slug> --base <baseref>

Options:
  --path <dir>   Override the worktree directory (default: dedicated
                 `<repo-parent>/spooncast-android-worktrees/<sanitized-branch>`).
  --verify       After setup, run `./gradlew help` to confirm the worktree
                 configures (catches a missing/incomplete config copy).
"""
import argparse
import shutil
import subprocess
import sys
from pathlib import Path

# git-ignored files the worktree needs for gradle to configure. Keep in sync
# with the project's .gitignore — these break the build by their absence.
CONFIG_FILES = ["secrets.properties", "local.properties"]


def run(cmd, cwd=None, check=True, capture=False):
    print(f"$ {' '.join(cmd)}")
    result = subprocess.run(
        cmd, cwd=cwd, text=True,
        capture_output=capture,
    )
    if capture:
        if result.stdout:
            print(result.stdout, end="")
        if result.stderr:
            print(result.stderr, end="", file=sys.stderr)
    if check and result.returncode != 0:
        sys.exit(f"\n명령 실패 (exit {result.returncode}): {' '.join(cmd)}")
    return result


def git_out(args, cwd=None):
    return subprocess.run(
        ["git", *args], cwd=cwd, text=True,
        capture_output=True,
    )


def main():
    parser = argparse.ArgumentParser(description="Create a gradle-ready git worktree.")
    parser.add_argument("branch", help="Branch to check out (existing), or new branch name with --base.")
    parser.add_argument("--base", help="Base ref to branch FROM. Presence means: create <branch> as a NEW branch.")
    parser.add_argument("--path", help="Worktree directory (default: dedicated worktrees folder).")
    parser.add_argument("--verify", action="store_true", help="Run `./gradlew help` after setup.")
    args = parser.parse_args()

    # Main repo root (the worktree the command runs in — it holds the configs).
    top = git_out(["rev-parse", "--show-toplevel"])
    if top.returncode != 0:
        sys.exit("git 저장소 안에서 실행해야 합니다.")
    source_root = Path(top.stdout.strip())

    # Worktree path: dedicated sibling folder, branch sanitized into a dir name.
    if args.path:
        worktree_path = Path(args.path).expanduser().resolve()
    else:
        dir_name = args.branch.replace("/", "_")
        worktree_path = source_root.parent / "spooncast-android-worktrees" / dir_name

    if worktree_path.exists():
        sys.exit(f"이미 존재하는 경로입니다: {worktree_path}\n다른 --path 를 지정하거나 기존 worktree 를 정리하세요.")

    worktree_path.parent.mkdir(parents=True, exist_ok=True)

    # --- create the worktree ---
    if args.base:
        # New branch from base.
        run(["git", "fetch", "origin", args.base], cwd=source_root)
        run(["git", "worktree", "add", "--no-track", "-b", args.branch,
             str(worktree_path), f"origin/{args.base}"], cwd=source_root)
    else:
        # Existing branch — prefer a local ref, else track the remote one.
        local = git_out(["show-ref", "--verify", "--quiet", f"refs/heads/{args.branch}"], cwd=source_root)
        if local.returncode == 0:
            run(["git", "worktree", "add", str(worktree_path), args.branch], cwd=source_root)
        else:
            run(["git", "fetch", "origin", args.branch], cwd=source_root)
            run(["git", "worktree", "add", "--track", "-b", args.branch,
                 str(worktree_path), f"origin/{args.branch}"], cwd=source_root)

    # --- copy git-ignored config files so gradle can configure ---
    copied, missing = [], []
    for name in CONFIG_FILES:
        src = source_root / name
        if src.exists():
            shutil.copy2(src, worktree_path / name)
            copied.append(name)
        else:
            missing.append(name)
    if copied:
        print(f"\n설정 파일 복사 완료: {', '.join(copied)}")
    if missing:
        print(f"경고: 메인 repo 에 없어 복사 못한 파일: {', '.join(missing)} "
              f"(gradle 가 깨질 수 있으니 확인 필요)", file=sys.stderr)

    # --- optional verification ---
    if args.verify:
        print("\n=== gradle 설정 검증 (./gradlew help) — 첫 실행은 느릴 수 있습니다 ===")
        result = run(["./gradlew", "help", "-q"], cwd=worktree_path, check=False, capture=True)
        if result.returncode == 0:
            print("gradle 설정 OK — worktree 빌드 준비 완료.")
        else:
            print("gradle 설정 실패 — 설정 파일 복사 또는 베이스 브랜치를 확인하세요.", file=sys.stderr)

    print(f"\n✅ worktree 생성 완료:\n  {worktree_path}")
    print(f"\n이동: cd {worktree_path}")


if __name__ == "__main__":
    main()
