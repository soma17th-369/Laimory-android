---
name: label-guide
description: GitHub Issue Type과 구분되는 라벨의 역할, 카테고리, 네이밍 기준입니다.
---

# 라벨 가이드

Issue Type은 Epic, Task, Feature, Bug, Refactor처럼 작업 계층과 큰 유형을 표현합니다.
라벨은 작업이 영향을 받는 영역을 보조적으로 분류합니다.

## 1. 기본 원칙

- 라벨은 Issue Type을 대체하지 않습니다.
- 라벨은 prefix 없이 영어 영역명으로 작성합니다.
- Priority, Size, Status는 프로젝트 필드로 관리하고 라벨로 중복하지 않습니다.
- 하나의 이슈에는 보통 영역 라벨 1개 이상을 붙입니다.
- 이모지는 라벨 이름에 넣지 않습니다.
- 라벨 색상은 적당히 채도가 있는 밝은 톤을 우선 사용해 이슈 목록에서 구분되지만 과하게 튀지 않게 합니다.
- 확신이 없으면 라벨을 많이 붙이지 않고, 가장 직접적인 영역만 선택합니다.

## 2. Area 라벨

작업이 영향을 주는 영역을 표현합니다.

| 라벨 | 기준 |
|---|---|
| `android` | Android 앱 코드 전반 |
| `docs` | README, 가이드, 문서 중심 작업 |
| `ai-agent` | `.agent`, AGENTS.md, CLAUDE.md, Agent workflow |
| `github` | Issue, PR, GitHub Actions, 라벨, 템플릿, CI/CD |
| `figma` | Figma MCP, Figma 파일, 디자인 초안 |

## 3. 확장 기준

처음부터 세부 라벨을 많이 만들지 않습니다.
같은 주제의 이슈가 반복되어 검색과 필터링이 실제로 필요해질 때만 추가합니다.

추가 기준:

- 같은 주제의 열린 이슈가 3개 이상 반복됩니다.
- PR 리뷰나 회의에서 해당 주제를 자주 따로 추적합니다.
- 기존 라벨만으로 담당자나 영향 범위를 구분하기 어렵습니다.

Android 세부 후보로는 `performance`, `database`, `firebase`, `on-device-ai`, `navigation`, `network`가 있습니다.
다만 지금 단계에서는 모두 `android` 라벨에 포함하고, 필요한 시점에 사용자와 합의 후 분리합니다.

## 4. 예시

- Android 버그: `android`
- 레이어 구조 정리: `android`
- 문서 보완: `docs`
- Figma 파운데이션 초안: `figma`
- GitHub 라벨 정리: `github`
- CI/CD 자동화: `github`
- Agent 문서 보완: `ai-agent`
