---
name: figma-to-compose
description: Figma 화면을 Compose Route/Content/Screen 코드로 구현하는 워크플로우 참조 문서입니다.
---

# Figma → Compose 구현 워크플로우

Figma 파일: [Laimory](https://www.figma.com/design/Ln4yLrEV88RFknIwRWzycZ/Laimory) · `02 Feature Layouts`.

## 0. 전제

- 색·타이포·간격·모양은 **반드시 토큰으로** 옮긴다. hex/숫자 하드코딩 금지 → [디자인 토큰 매핑](../../design-tokens/references/design-tokens.md).
- 화면은 **Route → Content → Screen 3계층**으로 작성한다 → [Android 아키텍처](../../../android/architecture/SKILL.md).
- 다크모드는 `LaimoryTheme(darkTheme)`가 처리한다. 화면 코드는 모드를 의식하지 않는다.

## 1. 디자인 읽기 (Figma MCP)

`02 Feature Layouts`에서 대상 프레임의 node-id를 얻어 컨텍스트를 읽는다.

```
get_design_context(fileKey, nodeId)   // 프레임의 코드/스크린샷/토큰
get_variable_defs(fileKey, nodeId)    // 그 프레임이 쓰는 Semantic 토큰 확인
get_screenshot(fileKey, nodeId)       // 시각 확인
```

- 프레임 이름이 곧 명세다: `Timeline / Default`, `Timeline / Loading`, `Moment Detail / Empty` …
- 같은 Section의 여러 State 프레임을 **모두** 읽어 상태별 UI를 한 번에 파악한다.

## 2. 프레임 네이밍 → UiState 매핑

Figma `[Section] / [State]`의 State는 그대로 MVI `UiState`로 내려간다.

| Figma State | Compose 표현 |
|---|---|
| `Default` | 정상 데이터 렌더 |
| `Loading` | `state.isLoading` → 로딩 UI (State 컴포넌트 Loading) |
| `Empty` | 데이터 0건 → Empty UI |
| `Error` | 로드 실패 → Error UI + 재시도 |
| `Wireframe` | 구현 대상 아님(설계 참고용) |
| `Interaction Notes` | 인터랙션 명세 → Intent/SideEffect 설계 근거 |

```kotlin
// 한 Section = 한 UiState. State 프레임 = sealed/플래그로 표현
data class TimelineUiState(
    val isLoading: Boolean = false,
    val moments: List<MomentUi> = emptyList(),
    val error: Boolean = false,
) {
    val isEmpty: Boolean get() = !isLoading && !error && moments.isEmpty()
}
```

화면 본문은 상태 분기로 Default/Loading/Empty/Error를 렌더한다. Interaction Notes의 "탭 → 이동", "저장 → Snackbar" 등은 `UiIntent` / `UiSideEffect`로 옮긴다.

## 3. 컴포넌트 인스턴스 → 공용 컴포저블

Figma `00 Foundation`의 컴포넌트는 `core:designsystem`의 공용 컴포저블과 1:1로 대응시킨다. 화면마다 새로 그리지 말고 공용 컴포저블을 호출한다.

| Figma 컴포넌트 (variants) | Compose 컴포저블 |
|---|---|
| Button (Filled/Tonal/Outline/Text × Default/Disabled) | `LaimoryButton(style, enabled, …)` |
| TextField (Default/Focused/Error/Disabled) | `LaimoryTextField(state, …)` |
| Card (Record/Stat) | `RecordCard` / `StatCard` |
| TopBar (back·title·action) | `LaimoryTopBar(title, onBack, action)` |
| BottomNavigation (홈/캘린더/회고/설정) | `LaimoryBottomBar(current, onSelect)` |
| Dialog (One/Two button) | `LaimoryDialog(buttons = One/Two, …)` |
| Snackbar (Default/Error/Action) | 공용 Snackbar 인프라(`LocalSnackbarHostState`) + variant |
| State (Empty/Loading/Error) | `EmptyState` / `LoadingState` / `ErrorState` |

> Dialog/Snackbar/Error State는 **#54 공통 에러 처리(MessageHelper)** 와 직결된다. 에러 표현은 임의로 만들지 말고 이 컴포넌트로 통일한다.

## 4. 토큰으로 스타일링

`get_variable_defs`로 확인한 Semantic 토큰을 그대로 코드 토큰으로 쓴다.

```kotlin
// Figma: color/surface, color/on-surface, radius/lg, color/outline-variant
Surface(
    shape = MaterialTheme.shapes.large,                       // radius/lg = 16
    color = MaterialTheme.colorScheme.surface,                // color/surface
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,         // Title/Medium
        color = MaterialTheme.colorScheme.onSurface,          // color/on-surface
    )
}

// 감정/확장색은 laimoryColors
Box(Modifier.background(MaterialTheme.laimoryColors.calmContainer))
```

**절대 금지:** `Color(0xFF879BD3)` 같은 hex 직접 사용, `16.dp` 매직넘버 남발(토큰/Spacing 사용).

## 5. 화면 골격 (Route/Content/Screen)

```kotlin
// XxxScreen.kt — 세 함수 한 파일
@Composable fun TimelineRoute(/* innerPadding, nav 콜백, viewModel */) { … }   // public
@Composable private fun TimelineContent(/* state, intent, snackbar/sideEffect */) { … }
@Composable private fun TimelineScreen(state, onIntent) {                       // 순수 UI
    when {
        state.isLoading -> LoadingState()
        state.error     -> ErrorState(onRetry = { onIntent(Retry) })
        state.isEmpty   -> EmptyState(onAction = { onIntent(StartRecord) })
        else            -> TimelineList(state.moments, onIntent)               // Default
    }
}
```

- Preview는 각 State마다 작성한다 (`@Preview` Default/Loading/Empty/Error + 다크). Figma 프레임 = Preview 1:1.
- 다크 Preview: `LaimoryTheme(darkTheme = true) { … }`.

## 6. 체크리스트

- [ ] 대상 Section의 모든 State 프레임을 MCP로 읽었다
- [ ] State → UiState/Intent/SideEffect로 매핑했다 (Interaction Notes 반영)
- [ ] 색/타이포/모양/간격을 전부 토큰으로 썼다 (하드코딩 0)
- [ ] Figma 컴포넌트를 공용 컴포저블로 재사용했다 (새로 안 그림)
- [ ] Route/Content/Screen 3계층 + State별 Preview(라이트/다크) 작성
- [ ] Ubiquitous Language 준수 (Timeline/Moment 등) — [용어 사전](../../../ubiquitous-language/glossary/SKILL.md)
