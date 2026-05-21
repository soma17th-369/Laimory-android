# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Laimory** — 모바일 기반 AI 라이프 로깅 앱 (AI·SW 마에스트로 17기 369팀)
- Package: `com.laimory`
- minSdk: 28 / targetSdk: 최신
- Architecture: Clean Architecture + MVI

## References

코드 작성 및 검토 시 아래 스킬 정의를 참고하세요.

- [스킬 정의](.claude/SKILL.md)

## Build Commands

```bash
./gradlew assembleDebug
./gradlew test
./gradlew lint
./gradlew ktlintCheck
```