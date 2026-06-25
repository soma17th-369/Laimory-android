---
name: codex-agent-entry
description: Laimory Android 저장소에서 Codex가 기획과 리뷰 작업을 수행할 때 따르는 진입 지침입니다.
---

# AGENTS.md

이 문서는 Codex가 이 저장소에서 작업할 때 따르는 지침입니다.

## Agent 역할

Codex는 6단계 Agent 협업 플로우 중 **1. 기획**, **3. 기획 리뷰 반영**, **5. 구현 리뷰**를 담당합니다.
사용자가 명시적으로 Codex에게 코드 수정을 요청하지 않는 한, 직접 구현과 구현 리뷰 반영은 Claude가 담당하는 흐름을 우선합니다.

## 프로젝트 개요

**Laimory** — 모바일 기반 AI 라이프 로깅 앱 (AI·SW 마에스트로 17기 369팀)
- Package: `com.laimory`
- minSdk: 28 / targetSdk: 최신
- Architecture: Clean Architecture + MVI

## 참조

기획, 리뷰, 구현 방향 검토 시 공용 Agent 지침을 참고하세요.

- [기획 담당 Agent](.agent/agents/planning-lead.md)
- [기획 리뷰 Agent](.agent/agents/planning-reviewer.md)
- [아키텍처 검토 Agent](.agent/agents/architecture-guardian.md)
- [리뷰 담당 Agent](.agent/agents/review-guardian.md)
- [스킬 관리 Agent](.agent/agents/skill-guardian.md)
- [Ubiquitous Language Agent](.agent/agents/ubiquitous-language-guardian.md)
- [Agent Workflow 인덱스](.agent/skills/agent-workflow/index.md)
- [Agent 협업 플로우 가이드](.agent/skills/agent-workflow/collaboration/references/agent-collaboration-flow.md)
- [Ubiquitous Language 용어 사전](docs/ubiquitous-language/glossary.md)
- [Android 스킬 인덱스](.agent/skills/android/index.md)
- [Android 아키텍처 지침](.agent/skills/android/architecture/SKILL.md)
- [프로젝트 초기 세팅](.agent/skills/android/architecture/references/project-initial-setup.md)
- [Git 스킬 인덱스](.agent/skills/git/index.md)
- [브랜치 네이밍 가이드](.agent/skills/git/branch-naming/references/branch-naming-guide.md)
- [커밋 가이드](.agent/skills/git/commit/references/commit-guide.md)
- [스킬 관리 인덱스](.agent/skills/skill-management/index.md)
- [스킬 작성 가이드](.agent/skills/skill-management/creation/references/skill-creation-guide.md)
- [스킬 리뷰 가이드](.agent/skills/skill-management/review/references/skill-review-guide.md)

## Ubiquitous Language

새 제품/도메인 용어가 생기거나 기존 용어와 충돌할 때만 Ubiquitous Language 가디언과 용어 사전을 참조합니다.

## Agent 협업 플로우

기본 작업 흐름은 `기획(Codex) -> 기획 리뷰(Claude) -> 기획 리뷰 반영(Codex) -> 구현(Claude) -> 구현 리뷰(Codex) -> 구현 리뷰 반영(Claude)` 순서로 진행합니다.

## 문서 작성 규칙

레퍼런스 문서에서 순서, 단계, 우선순위, 판단 흐름을 명확히 드러내야 할 때는 번호형 제목을 사용할 수 있습니다.

- 상위 제목은 `1.`, `2.`, `3.` 형식으로 작성합니다.
- 하위 제목은 `1.1`, `1.2`, `2.1` 형식으로 작성합니다.
- 사전 조건, 배경, 전제처럼 본 흐름 앞에 두는 항목은 필요하면 `0.`을 사용할 수 있습니다.

## 빌드 명령

```bash
./gradlew assembleDebug
./gradlew test
./gradlew lint
./gradlew ktlintCheck
```
