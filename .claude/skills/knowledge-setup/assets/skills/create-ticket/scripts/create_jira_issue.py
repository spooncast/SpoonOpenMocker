#!/usr/bin/env python3
"""
Jira 이슈 생성 스크립트 (dexus MCP 미연결 시 fallback).

사용법:
    python3 create_jira_issue.py \
        --project AOSTEAM \
        --summary "이미지 검색 API 연동" \
        --description-file /tmp/create-ticket-body.md \
        --type Task \
        [--parent AOSTEAM-1234] \
        [--labels android client] \
        [--components network]

환경변수 (.env 또는 환경변수):
    JIRA_BASE_URL  - Jira 인스턴스 URL (예: https://yourcompany.atlassian.net)
    JIRA_EMAIL     - Jira 계정 이메일
    JIRA_API_TOKEN - Jira API 토큰

출력:
    생성된 이슈의 key 와 브라우저 URL.
"""

import argparse
import base64
import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path


def load_env():
    """스크립트 위치 및 현재 디렉토리에서 상위로 올라가며 .env 파일 탐색."""
    search_dirs = [
        Path(__file__).resolve().parent,                # scripts/
        Path(__file__).resolve().parent.parent,         # create-ticket/
        Path(__file__).resolve().parent.parent.parent,  # skills/
        Path.cwd(),
        *Path.cwd().parents,
    ]
    for directory in search_dirs:
        env_file = directory / ".env"
        if env_file.exists():
            with open(env_file, encoding="utf-8") as f:
                for line in f:
                    line = line.strip()
                    if line and not line.startswith("#") and "=" in line:
                        key, _, value = line.partition("=")
                        os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))
            return


def get_jira_config():
    load_env()
    base_url = os.environ.get("JIRA_BASE_URL", "")
    email = os.environ.get("JIRA_EMAIL", "")
    token = os.environ.get("JIRA_API_TOKEN", "")

    missing = [k for k, v in (
        ("JIRA_BASE_URL", base_url),
        ("JIRA_EMAIL", email),
        ("JIRA_API_TOKEN", token),
    ) if not v]
    if missing:
        print(f"[ERROR] 환경변수가 없습니다: {', '.join(missing)}", file=sys.stderr)
        print("  .env 또는 환경변수에 JIRA_BASE_URL / JIRA_EMAIL / JIRA_API_TOKEN 을 설정하세요.", file=sys.stderr)
        sys.exit(1)
    return base_url.rstrip("/"), email, token


def text_to_adf(text: str) -> dict:
    """플레인 텍스트(줄바꿈 구분)를 Atlassian Document Format 문서로 변환."""
    paragraphs = []
    for block in text.split("\n"):
        block = block.rstrip()
        if block:
            paragraphs.append({
                "type": "paragraph",
                "content": [{"type": "text", "text": block}],
            })
        else:
            paragraphs.append({"type": "paragraph", "content": []})
    if not paragraphs:
        paragraphs = [{"type": "paragraph", "content": []}]
    return {"type": "doc", "version": 1, "content": paragraphs}


def create_issue(project, summary, description, issue_type, parent=None, labels=None, components=None):
    base_url, email, token = get_jira_config()
    url = f"{base_url}/rest/api/3/issue"

    fields = {
        "project": {"key": project},
        "summary": summary,
        "issuetype": {"name": issue_type},
        "description": text_to_adf(description),
    }
    if parent:
        fields["parent"] = {"key": parent}
    if labels:
        fields["labels"] = labels
    if components:
        fields["components"] = [{"name": c} for c in components]

    data = json.dumps({"fields": fields}).encode("utf-8")
    credentials = base64.b64encode(f"{email}:{token}".encode()).decode()
    req = urllib.request.Request(
        url,
        data=data,
        headers={
            "Authorization": f"Basic {credentials}",
            "Content-Type": "application/json",
            "Accept": "application/json",
        },
        method="POST",
    )

    try:
        with urllib.request.urlopen(req) as response:
            result = json.loads(response.read().decode())
            key = result.get("key", "")
            browse_url = f"{base_url}/browse/{key}"
            print("[SUCCESS] Jira 이슈 생성 완료!")
            print(f"  {key}: {summary}")
            print(f"  URL: {browse_url}")
            return result
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        print(f"[ERROR] 이슈 생성 실패 (HTTP {e.code})", file=sys.stderr)
        try:
            err = json.loads(body)
            messages = err.get("errorMessages", [])
            field_errors = err.get("errors", {})
            if messages:
                print(f"  메시지: {'; '.join(messages)}", file=sys.stderr)
            if field_errors:
                for k, v in field_errors.items():
                    print(f"  - {k}: {v}", file=sys.stderr)
        except Exception:
            print(f"  응답: {body}", file=sys.stderr)
        sys.exit(1)
    except urllib.error.URLError as e:
        print(f"[ERROR] 네트워크 오류: {e.reason}", file=sys.stderr)
        sys.exit(1)


def main():
    parser = argparse.ArgumentParser(description="Jira 이슈 생성")
    parser.add_argument("--project", required=True, help="프로젝트 키 (예: AOSTEAM)")
    parser.add_argument("--summary", required=True, help="이슈 제목")
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--description", help="이슈 본문 (플레인 텍스트)")
    group.add_argument("--description-file", help="이슈 본문 파일 경로")
    parser.add_argument("--type", default="Task", help="이슈 타입 (기본: Task, 하위태스크는 Sub-task)")
    parser.add_argument("--parent", help="상위 이슈 키 (하위 태스크일 때)")
    parser.add_argument("--labels", nargs="*", help="라벨 목록")
    parser.add_argument("--components", nargs="*", help="컴포넌트 이름 목록")
    args = parser.parse_args()

    if args.description_file:
        description = Path(args.description_file).read_text(encoding="utf-8")
    else:
        description = args.description

    create_issue(
        project=args.project,
        summary=args.summary,
        description=description,
        issue_type=args.type,
        parent=args.parent,
        labels=args.labels,
        components=args.components,
    )


if __name__ == "__main__":
    main()
