---
name: skill-creation-guide
description: Laimory Android 저장소의 .agent 스킬을 설계, 작성, 분리, 연결하는 기준입니다.
---

# 스킬 작성 가이드

이 문서는 Claude/Codex Skill 생태계의 좋은 점을 참고하되, Laimory 저장소의 `.agent/skills` 구조에 맞게 적용하는 기준입니다.

## 0. 기본 전제

- `.agent/skills`는 프로젝트 규칙을 에이전트가 일관되게 참조하기 위한 문서 구조입니다.
- 공식 Claude Skill 패키지를 그대로 복사하지 않고, 필요한 기준만 프로젝트 방식으로 재작성합니다.
- 새 스킬은 실제 반복 작업, 판단 기준, 검증 흐름이 생겼을 때 추가합니다.
- 단순 설명 문서 하나면 충분한 내용은 기존 reference에 추가하고, 불필요한 카테고리를 만들지 않습니다.

## 1. 권장 폴더 구조

```text
.agent/skills/<category>/
├── index.md
└── <topic>/
    ├── SKILL.md
    └── references/
        └── <topic>-guide.md
```

- `<category>`는 `android`, `git`, `github`, `design`, `skill-management`처럼 책임 영역을 나타냅니다.
- `<topic>`은 `branch-naming`, `commit`, `workflow`, `creation`, `review`처럼 구체적인 작업 단위를 나타냅니다.
- 스크립트나 산출물 템플릿이 실제로 필요해지기 전까지 `scripts/`, `assets/`는 만들지 않습니다.

## 2. 이름 규칙

- 폴더명과 skill name은 소문자 영문, 숫자, 하이픈만 사용합니다.
- 이름은 짧고 구체적으로 작성합니다.
- 범위가 모호한 이름은 피합니다. 예를 들어 `naming`보다 `branch-naming`을 사용합니다.
- 플랫폼 협업 규칙은 `github`, 로컬 버전관리 규칙은 `git`처럼 책임을 분리합니다.

## 3. SKILL.md 작성 기준

- frontmatter에는 `name`, `description`을 작성합니다.
- `description`에는 무엇을 하는 스킬인지와 언제 참조해야 하는지를 함께 씁니다.
- 본문은 짧게 유지하고, 세부 기준은 `references/`로 분리합니다.
- `## 참조`에는 Markdown 링크 목록을 사용합니다.
- `@references/...` 같은 멘션 문법은 문서 참조로 사용하지 않습니다.

## 4. Reference 작성 기준

- reference는 실제 판단 기준, 절차, 예시, 예외 처리를 담습니다.
- 순서나 판단 흐름이 중요한 문서는 번호형 제목을 사용합니다.
- 상위 제목은 `1.`, `2.`, `3.` 형식으로 작성합니다.
- 하위 제목은 `1.1`, `1.2`, `2.1` 형식으로 작성합니다.
- 사전 조건, 배경, 전제는 필요하면 `0.`을 사용할 수 있습니다.
- 같은 규칙을 여러 문서에 중복 작성하지 말고 원천 문서 하나를 링크합니다.

## 5. 분리 기준

- `SKILL.md`에는 스킬을 언제 쓰고 어떤 reference를 읽어야 하는지만 둡니다.
- 상세 표, 예시, 체크리스트, 긴 절차는 reference로 분리합니다.
- 한 reference가 너무 커져 서로 다른 작업 흐름을 섞기 시작하면 topic을 나눕니다.
- 다른 카테고리의 책임을 설명해야 한다면 복사하지 말고 링크합니다.

## 6. 작성 흐름

1. 스킬이 필요한 반복 작업이나 판단 상황을 정의합니다.
2. 기존 카테고리에 넣을 수 있는지 먼저 확인합니다.
3. 새 카테고리나 topic이 필요하면 폴더 구조를 만듭니다.
4. `SKILL.md`에는 짧은 설명과 reference 링크를 둡니다.
5. reference에는 번호형 제목, 기준, 예시, 예외 처리를 작성합니다.
6. 관련 Agent 문서와 상위 index 문서에 링크를 추가합니다.
7. 로컬 Markdown 링크가 깨지지 않는지 확인합니다.

## 7. 피해야 할 패턴

- 설명만 있고 실제 사용 시점이 드러나지 않는 `description`
- `SKILL.md`에 긴 세부 기준을 모두 넣는 구조
- 같은 규칙을 여러 reference에 복사하는 구조
- 현재 필요하지 않은 `scripts/`, `assets/`, README성 문서 추가
- 기존 카테고리로 충분한데 새 카테고리를 만드는 것
