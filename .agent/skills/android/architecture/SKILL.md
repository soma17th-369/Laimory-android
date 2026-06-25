---
name: android-architecture
description: Codex와 Claude가 함께 사용하는 Android 아키텍처 및 구현 규칙입니다.
---

# Laimory Android - 공용 Agent 지침

이 프로젝트에서 Codex와 Claude가 기획, 구현, 리뷰를 수행할 때 아래 레퍼런스를 참조한다.

## Agent 역할

- Codex: 1단계 기획, 3단계 기획 리뷰 반영, 5단계 구현 리뷰
- Claude: 2단계 기획 리뷰, 4단계 구현, 6단계 구현 리뷰 반영
- 공통: `.agent` 문서를 프로젝트 규칙과 아키텍처의 기준으로 사용한다.

---

## 1. 프로젝트 초기 세팅

[프로젝트 초기 세팅](references/project-initial-setup.md)

기술 스택, 모듈 구조, 레이어 설계, MVI 패턴, API 응답 구조, 코드 컨벤션, 세팅 순서를 담고 있다.

**언제 참조하나:**
- 모듈 추가 및 build.gradle.kts 작성 시
- Clean Architecture + MVI 레이어 코드 작성 시
- ViewModel, UiState, UiIntent, UiSideEffect 구현 시
- 새 라이브러리 의존성 추가 시

---

## 2. 화면 컴포저블 3계층 구조

모든 Feature 화면은 **Route → Content → Screen** 3계층으로 작성한다.

### 계층 정의

| 계층 | 접근제한자 | 책임 |
|------|-----------|------|
| `XxxRoute` | `public` | ViewModel 주입, 상태 수집, 네비게이션 콜백 수신 |
| `XxxContent` | `private` | Effect/SideEffect collect, 다이얼로그 오케스트레이션 |
| `XxxScreen` | `private` | 순수 Stateless UI. ViewModel 의존성 없음 |

### 코드 구조

```kotlin
// Route — public, NavGraph에서 호출
@Composable
fun TimelineRoute(
    innerPadding: PaddingValues,
    onNavigateToDetail: (id: Long) -> Unit,
    viewModel: TimelineViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TimelineContent(
        innerPadding = innerPadding,
        state = state,
        onNavigateToDetail = onNavigateToDetail,
        onIntent = viewModel::sendIntent,
        snackbarFlow = viewModel.snackbar,
        sideEffectFlow = viewModel.sideEffect,
    )
}

// Content — private, Effect 처리 및 다이얼로그 오케스트레이션
@Composable
private fun TimelineContent(
    innerPadding: PaddingValues,
    state: TimelineUiState,
    onNavigateToDetail: (id: Long) -> Unit,
    onIntent: (TimelineUiIntent) -> Unit,
    snackbarFlow: Flow<String>,
    sideEffectFlow: Flow<TimelineUiSideEffect>,
) {
    val snackbarHostState = LocalSnackbarHostState.current

    LaunchedEffect(Unit) {
        snackbarFlow.collect { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(Unit) {
        sideEffectFlow.collect { effect ->
            when (effect) {
                is TimelineUiSideEffect.NavigateToDetail -> onNavigateToDetail(effect.id)
            }
        }
    }

    TimelineScreen(
        innerPadding = innerPadding,
        state = state,
        onIntent = onIntent,
    )

    if (state.isDialogVisible) {
        TimelineDialog(onDismiss = { onIntent(TimelineUiIntent.DismissDialog) })
    }
}

// Screen — private, 순수 UI
@Composable
private fun TimelineScreen(
    innerPadding: PaddingValues,
    state: TimelineUiState,
    onIntent: (TimelineUiIntent) -> Unit,
) {
    // UI 구성만. ViewModel, Effect, 다이얼로그 없음
}
```

### 규칙 요약

- **Route**만 `hiltViewModel()`을 호출한다. Content/Screen은 ViewModel을 직접 참조하지 않는다.
- **Content**에서 `LocalSnackbarHostState.current`로 스낵바를 표시하고, `sideEffect`로 네비게이션을 실행한다.
- **다이얼로그**는 Content에서 state 조건으로 오버레이한다. Screen 내부에 두지 않는다.
- **Screen**은 파라미터만으로 렌더링이 결정되어야 한다. Preview 작성이 가능한 상태를 유지한다.
- NavGraph에서는 항상 **Route** 함수만 참조한다.

### 파일 네이밍

```
TimelineScreen.kt  ← Route / Content / Screen 세 함수 모두 같은 파일에 작성
```

### 스낵바 인프라

스낵바는 `LaimoryNavGraph` 레벨의 단일 `Scaffold`에서 중앙 관리한다.

```kotlin
// core:ui 모듈
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("No SnackbarHostState provided")
}

// LaimoryNavGraph
CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        NavHost(...) { ... }
    }
}
```
