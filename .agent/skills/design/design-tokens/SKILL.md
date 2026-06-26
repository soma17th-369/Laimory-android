---
name: design-tokens
description: Figma Variables(Primitives/Semantic)를 Jetpack Compose 테마(ColorScheme/Typography/Shape/Spacing)로 내려쓸 때 사용하는 디자인 토큰 매핑 지침입니다.
---

# 디자인 토큰 스킬

Figma 디자인 시스템의 토큰을 Compose 코드로 옮기거나, 테마(`LaimoryTheme`)·색·타이포·간격을 구현/수정할 때 이 스킬을 사용합니다.

**언제 참조하나요:**
- `core:ui` 모듈(공통 Composable·Theme·DesignSystem)의 `Color.kt` / `Type.kt` / `Shape.kt` / `Theme.kt` 작성·수정 시
- Figma `Semantic` 토큰을 `MaterialTheme.colorScheme.*`로 매핑할 때
- M3에 없는 확장색(success/warning/info/감정 5색)을 `LaimoryColors`로 추가할 때
- 라이트/다크 모드 색을 정의할 때

## 참조

- [디자인 토큰 매핑](references/design-tokens.md)