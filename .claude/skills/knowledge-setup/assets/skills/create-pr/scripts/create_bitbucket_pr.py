#!/usr/bin/env python3
"""
Bitbucket Cloud PR 생성 스크립트
Usage: python3 create_bitbucket_pr.py --title "제목" --description "설명" --source "feature/branch" --destination "main"

필요한 환경변수 (.env 또는 환경변수):
  BITBUCKET_WORKSPACE    - Bitbucket workspace slug
  BITBUCKET_REPO_SLUG    - Repository slug
  BITBUCKET_ACCESS_TOKEN - Bitbucket Access Token (Repository → Access tokens 에서 생성)
"""

import argparse
import json
import os
import sys
import urllib.request
import urllib.error
from pathlib import Path


BITBUCKET_API = "https://api.bitbucket.org/2.0"


def load_env():
    """스크립트 위치 및 현재 디렉토리에서 상위로 올라가며 .env 파일 탐색"""
    search_dirs = [
        Path(__file__).resolve().parent,          # scripts/
        Path(__file__).resolve().parent.parent,    # bitbucket-pr/
        Path(__file__).resolve().parent.parent.parent,  # skills/
        Path.cwd(),
        *Path.cwd().parents,
    ]
    for directory in search_dirs:
        env_file = directory / ".env"
        if env_file.exists():
            with open(env_file) as f:
                for line in f:
                    line = line.strip()
                    if line and not line.startswith("#") and "=" in line:
                        key, _, value = line.partition("=")
                        os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))
            return


def get_required_env(key):
    value = os.environ.get(key)
    if not value:
        print(f"[ERROR] 환경변수 '{key}'가 설정되지 않았습니다.")
        print(f"  .env 파일 또는 환경변수에 {key}=값 형태로 추가해주세요.")
        sys.exit(1)
    return value


def create_pr(title, description, source_branch, destination_branch, reviewers=None):
    load_env()

    workspace    = get_required_env("BITBUCKET_WORKSPACE")
    repo_slug    = get_required_env("BITBUCKET_REPO_SLUG")
    access_token = get_required_env("BITBUCKET_ACCESS_TOKEN")

    url = f"{BITBUCKET_API}/repositories/{workspace}/{repo_slug}/pullrequests"

    payload = {
        "title": title,
        "description": description,
        "source": {
            "branch": {"name": source_branch}
        },
        "destination": {
            "branch": {"name": destination_branch}
        },
        "close_source_branch": False,
    }

    if reviewers:
        payload["reviewers"] = [{"uuid": r} for r in reviewers]

    data = json.dumps(payload).encode("utf-8")

    req = urllib.request.Request(
        url,
        data=data,
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {access_token}",
        },
        method="POST",
    )

    try:
        with urllib.request.urlopen(req) as response:
            result = json.loads(response.read().decode())
            pr_id  = result.get("id", "")
            pr_url = result.get("links", {}).get("html", {}).get("href", "")
            print(f"[SUCCESS] PR 생성 완료!")
            print(f"  PR #{pr_id}: {title}")
            print(f"  URL: {pr_url}")
            return result
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        print(f"[ERROR] PR 생성 실패 (HTTP {e.code})")
        try:
            err = json.loads(body)
            msg = err.get("error", {}).get("message", body)
            print(f"  메시지: {msg}")
        except Exception:
            print(f"  응답: {body}")
        sys.exit(1)
    except urllib.error.URLError as e:
        print(f"[ERROR] 네트워크 오류: {e.reason}")
        sys.exit(1)


def main():
    parser = argparse.ArgumentParser(description="Bitbucket Cloud PR 생성")
    parser.add_argument("--title",       required=True, help="PR 제목")
    parser.add_argument("--description", required=True, help="PR 설명 (마크다운)")
    parser.add_argument("--source",      required=True, help="소스 브랜치명")
    parser.add_argument("--destination", required=True, help="대상 브랜치명")
    parser.add_argument("--reviewers",   nargs="*",     help="리뷰어 UUID 목록 (선택)")
    args = parser.parse_args()

    create_pr(
        title=args.title,
        description=args.description,
        source_branch=args.source,
        destination_branch=args.destination,
        reviewers=args.reviewers,
    )


if __name__ == "__main__":
    main()
