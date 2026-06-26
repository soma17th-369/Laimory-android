---
name: claude-agent-entry
description: Laimory Android 저장소에서 Claude가 구현 작업을 수행할 때 따르는 진입 지침입니다.
---

# CLAUDE.md

이 문서는 Claude Code가 이 저장소에서 작업할 때 따르는 지침입니다.

## Agent 역할

Claude는 6단계 Agent 협업 플로우 중 **2. 기획 리뷰**, **4. 구현**, **6. 구현 리뷰 반영**을 담당합니다.
프로젝트 컨벤션과 아키텍처 기준은 공용 `.agent` 문서를 기준으로 삼습니다.

## 프로젝트 개요

**Laimory** — 모바일 기반 AI 라이프 로깅 앱 (AI·SW 마에스트로 17기 369팀)
- Package: `com.laimory`
- minSdk: 28 / targetSdk: 최신
- Architecture: Clean Architecture + MVI

## 참조

코드 작성 및 수정 시 공용 Agent 지침을 참고하세요.

- [공용 Agent 진입 지침](AGENTS.md)
- [Claude Skill Entry](.claude/SKILL.md)
- [Agent 협업 플로우 가이드](.agent/skills/agent-workflow/collaboration/references/agent-collaboration-flow.md)
- [기획 리뷰 Agent](.agent/agents/planning-reviewer.md)
- [구현 담당 Agent](.agent/agents/implementation-runner.md)
- [아키텍처 검토 Agent](.agent/agents/architecture-guardian.md)
- [Android 스킬 인덱스](.agent/skills/android/index.md)
- [Android 아키텍처 지침](.agent/skills/android/architecture/SKILL.md)
- [프로젝트 초기 세팅](.agent/skills/android/architecture/references/project-initial-setup.md)
- [디자인 스킬 인덱스](.agent/skills/design/index.md)
- [디자인 토큰 매핑 (Figma → Compose 테마)](.agent/skills/design/design-tokens/SKILL.md)
- [Figma → Compose 구현 워크플로우](.agent/skills/design/figma-to-compose/SKILL.md)

새 제품/도메인 용어가 생기거나 기존 용어와 충돌할 때만 [Ubiquitous Language 용어 사전](docs/ubiquitous-language/glossary.md)을 참조합니다.

## 빌드 명령

```bash
./gradlew assembleDebug
./gradlew test
./gradlew lint
./gradlew ktlintCheck
```
