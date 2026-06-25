---
name: claude-skill-entry
description: Claude가 Laimory Android 저장소에서 구현 작업을 수행할 때 참조할 공용 Agent 문서 안내입니다.
---

# Claude Skill Entry

Claude는 Agent 협업 플로우에서 기획 리뷰, 구현, 구현 리뷰 반영을 담당하며, 실제 프로젝트 규칙은 공용 `.agent` 문서를 기준으로 참조합니다.

## 기본 흐름

- 2단계 기획 리뷰는 [기획 리뷰어](../.agent/agents/planning-reviewer.md)의 책임과 흐름을 따릅니다.
- 4단계 구현은 [구현 러너](../.agent/agents/implementation-runner.md)의 책임과 흐름을 따릅니다.
- 6단계 구현 리뷰 반영은 Codex의 구현 리뷰 결과를 기준으로 수정하고 재검증합니다.
- 구현 후 자체 구조 점검이 필요하면 [아키텍처 가디언](../.agent/agents/architecture-guardian.md) 기준을 참고합니다.

## 참조

- [Android 스킬 인덱스](../.agent/skills/android/index.md)
- [Agent 협업 플로우 가이드](../.agent/skills/agent-workflow/collaboration/references/agent-collaboration-flow.md)
- [기획 리뷰 Agent](../.agent/agents/planning-reviewer.md)
- [Android 아키텍처 지침](../.agent/skills/android/architecture/SKILL.md)
- [Android Scaffolding 지침](../.agent/skills/android/scaffolding/SKILL.md)
- [레이어 역할 가이드](../.agent/skills/android/scaffolding/references/layer-role-guide.md)
- [Gradle 빌드 검증 지침](../.agent/skills/android/gradle-build-check/SKILL.md)
- [GitHub 스킬 인덱스](../.agent/skills/github/index.md)
