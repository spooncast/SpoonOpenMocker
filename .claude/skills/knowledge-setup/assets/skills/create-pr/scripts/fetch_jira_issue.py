#!/usr/bin/env python3
"""
Jira 이슈 조회 스크립트.

사용법:
    python fetch_jira_issue.py TICKET-123

환경변수:
    JIRA_BASE_URL  - Jira 인스턴스 URL (예: https://yourcompany.atlassian.net)
    JIRA_EMAIL     - Jira 계정 이메일
    JIRA_API_TOKEN - Jira API 토큰

출력:
    JSON 형식의 이슈 정보 (summary, description, type, priority, labels,
    components, subtasks, comments, attachments, linked issues)

참고:
    이 파일은 스킬 패키징용 스텁입니다.
    Aaron의 기존 fetch_jira_issue.py로 교체하여 사용하세요.
    기존 스크립트의 출력 형식이 다르다면 SKILL.md의 Phase 1 파싱 로직을
    그에 맞게 조정하면 됩니다.
"""

import sys
import os
import json
import urllib.request
import urllib.error
import base64
from pathlib import Path


def load_env():
    """프로젝트 루트의 .env 파일에서 설정을 로드한다."""
    # 스크립트 위치 기준으로 프로젝트 루트를 탐색
    search_dirs = [
        Path.cwd(),
        Path(__file__).resolve().parent.parent,  # scripts/ -> jira-plan/
        Path(__file__).resolve().parent.parent.parent,  # jira-plan/ -> project root
    ]
    for d in search_dirs:
        env_path = d / ".env"
        if env_path.exists():
            with open(env_path, "r", encoding="utf-8") as f:
                for line in f:
                    line = line.strip()
                    if not line or line.startswith("#"):
                        continue
                    if "=" in line:
                        key, _, value = line.partition("=")
                        key = key.strip()
                        value = value.strip().strip("\"'")
                        if key and key not in os.environ:
                            os.environ[key] = value
            return
    # .env 파일이 없으면 기존 환경변수에 의존


def get_jira_config():
    load_env()

    base_url = os.environ.get("JIRA_BASE_URL", "")
    email = os.environ.get("JIRA_EMAIL", "")
    token = os.environ.get("JIRA_API_TOKEN", "")

    if not all([base_url, email, token]):
        missing = []
        if not base_url:
            missing.append("JIRA_BASE_URL")
        if not email:
            missing.append("JIRA_EMAIL")
        if not token:
            missing.append("JIRA_API_TOKEN")
        print(
            f"Error: 설정이 없습니다: {', '.join(missing)}",
            file=sys.stderr,
        )
        print(
            "프로젝트 루트에 .env 파일을 생성하세요. 예시:",
            file=sys.stderr,
        )
        print(
            '  JIRA_BASE_URL=https://yourcompany.atlassian.net\n'
            '  JIRA_EMAIL=you@company.com\n'
            '  JIRA_API_TOKEN=your-token',
            file=sys.stderr,
        )
        sys.exit(1)

    return base_url.rstrip("/"), email, token


def fetch_issue(issue_key: str) -> dict:
    base_url, email, token = get_jira_config()

    url = (
        f"{base_url}/rest/api/3/issue/{issue_key}"
        f"?fields=summary,description,issuetype,priority,labels,components,"
        f"subtasks,comment,attachment,issuelinks,status,assignee,reporter"
    )

    credentials = base64.b64encode(f"{email}:{token}".encode()).decode()
    headers = {
        "Authorization": f"Basic {credentials}",
        "Accept": "application/json",
    }

    req = urllib.request.Request(url, headers=headers)

    try:
        with urllib.request.urlopen(req) as response:
            data = json.loads(response.read().decode())
    except urllib.error.HTTPError as e:
        if e.code == 404:
            print(f"Error: 이슈 '{issue_key}'를 찾을 수 없습니다.", file=sys.stderr)
        elif e.code == 401:
            print("Error: 인증 실패. JIRA_EMAIL과 JIRA_API_TOKEN을 확인하세요.", file=sys.stderr)
        elif e.code == 403:
            print(f"Error: '{issue_key}' 접근 권한이 없습니다.", file=sys.stderr)
        else:
            print(f"Error: HTTP {e.code} - {e.reason}", file=sys.stderr)
        sys.exit(1)
    except urllib.error.URLError as e:
        print(f"Error: Jira 연결 실패 - {e.reason}", file=sys.stderr)
        sys.exit(1)

    fields = data.get("fields", {})

    # Description을 ADF에서 플레인 텍스트로 변환
    description = ""
    desc_field = fields.get("description")
    if desc_field and isinstance(desc_field, dict):
        description = extract_text_from_adf(desc_field)
    elif isinstance(desc_field, str):
        description = desc_field

    result = {
        "key": data.get("key"),
        "summary": fields.get("summary", ""),
        "description": description,
        "type": fields.get("issuetype", {}).get("name", ""),
        "priority": fields.get("priority", {}).get("name", ""),
        "status": fields.get("status", {}).get("name", ""),
        "assignee": (fields.get("assignee") or {}).get("displayName", "Unassigned"),
        "reporter": (fields.get("reporter") or {}).get("displayName", "Unknown"),
        "labels": fields.get("labels", []),
        "components": [c.get("name", "") for c in fields.get("components", [])],
        "subtasks": [
            {"key": s.get("key"), "summary": s.get("fields", {}).get("summary", "")}
            for s in fields.get("subtasks", [])
        ],
        "comments": [
            {
                "author": c.get("author", {}).get("displayName", ""),
                "body": extract_text_from_adf(c.get("body", {}))
                if isinstance(c.get("body"), dict)
                else str(c.get("body", "")),
                "created": c.get("created", ""),
            }
            for c in (fields.get("comment", {}).get("comments", []))[-5:]  # 최근 5개
        ],
        "linked_issues": [
            {
                "type": link.get("type", {}).get("outward", ""),
                "key": (
                    link.get("outwardIssue", {}) or link.get("inwardIssue", {})
                ).get("key", ""),
                "summary": (
                    link.get("outwardIssue", {}) or link.get("inwardIssue", {})
                )
                .get("fields", {})
                .get("summary", ""),
            }
            for link in fields.get("issuelinks", [])
        ],
    }

    return result


def extract_text_from_adf(adf_node: dict) -> str:
    """Atlassian Document Format을 플레인 텍스트로 변환."""
    if not isinstance(adf_node, dict):
        return str(adf_node) if adf_node else ""

    text_parts = []

    if adf_node.get("type") == "text":
        return adf_node.get("text", "")

    for child in adf_node.get("content", []):
        text_parts.append(extract_text_from_adf(child))

    separator = "\n" if adf_node.get("type") in ("doc", "paragraph", "bulletList", "orderedList", "listItem") else ""
    return separator.join(filter(None, text_parts))


def main():
    if len(sys.argv) < 2:
        print("사용법: python fetch_jira_issue.py TICKET-123", file=sys.stderr)
        sys.exit(1)

    issue_key = sys.argv[1].strip().upper()
    result = fetch_issue(issue_key)
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
