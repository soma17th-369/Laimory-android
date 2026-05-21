# GitHub Copilot Instructions

모든 코드 리뷰 및 응답은 **한국어**로 작성한다.

---

## 프로젝트 개요

- **서비스명**: Laimory
- **설명**: 모바일 기반 AI 라이프 로깅 앱
- **플랫폼**: Native Android (Kotlin)
- **패키지**: `com.soma369.laimory`

---

## 기술 스택

| 항목 | 기술 |
|---|---|
| UI | Jetpack Compose |
| Architecture | Clean Architecture + MVI |
| DI | Hilt |
| 비동기 | Coroutines + Flow |
| 네트워크 | Retrofit + OkHttp |
| 로컬 DB | Room |
| 설정 저장 | DataStore |
| 직렬화 | kotlinx.serialization |
| 이미지 | Coil |
| 코드 스타일 | KtLint |

---

## 모듈 구조

```
:app                  진입점, Hilt Application, Navigation 루트
:feature:home         Screen(Compose) + ViewModel(MVI)
:core:domain          순수 Kotlin — Model, Repository interface, UseCase (외부 의존성 없음)
:core:data            RepositoryImpl, RemoteDataSource, LocalDataSource, Mapper
:core:ui              공통 Composable, Theme, MVI 베이스 클래스
```

### 의존성 방향

```
:app → :feature:home → :core:domain
                     → :core:ui
       :core:data   → :core:domain
```

- `:core:domain`은 아무것도 의존하지 않는다.

---

## 아키텍처 — Clean Architecture + MVI

### 레이어 역할

- **Presentation** (`:feature:*`) — Screen, ViewModel, UiState / UiIntent / UiSideEffect
- **Domain** (`:core:domain`) — UseCase, Repository Interface, Domain Model
- **Data** (`:core:data`) — RepositoryImpl, DataSource, DTO → Domain Mapper

### MVI 흐름

```
UiIntent → ViewModel.sendIntent() → handleIntent() → updateState() / sendEffect()
```

모든 ViewModel은 `:core:ui`의 `MviViewModel<S, I, E>`를 상속한다.

---

## 코드 컨벤션

### 파일 네이밍

```
Screen          → HomeScreen.kt
ViewModel       → HomeViewModel.kt
UiState         → HomeUiState.kt
UseCase         → GetTimelineUseCase.kt
Repository      → TimelineRepository.kt (interface)
RepositoryImpl  → TimelineRepositoryImpl.kt
DTO             → TimelineDto.kt
Entity          → TimelineEntity.kt
Mapper          → TimelineMapper.kt
```

### 패키지 구조 (feature 모듈)

```
com.soma369.laimory.feature.home
├── screen/
├── viewmodel/
├── state/        ← UiState, UiIntent, UiSideEffect
└── component/
```

### 브랜치 네이밍

```
feat/#이슈번호-작업내용
fix/#이슈번호-작업내용
chore/#이슈번호-작업내용
```

### 커밋 메시지

```
feat: 기능 설명 (#이슈번호)
fix: 버그 수정 내용 (#이슈번호)
chore: 작업 내용 (#이슈번호)
refactor: 리팩토링 내용 (#이슈번호)
```

---

## 코드 리뷰 기준

리뷰 시 아래 항목을 기준으로 검토한다.

1. **레이어 의존성 방향** — Domain이 외부를 의존하지 않는지 확인
2. **MVI 패턴 준수** — UiState는 불변, SideEffect는 Channel로 전달
3. **단일 책임 원칙** — UseCase는 하나의 동작만 담당
4. **KtLint 스타일** — 와일드카드 import 금지, Composable 함수명 대문자 허용
5. **Coroutines 사용** — `GlobalScope` 사용 금지, `viewModelScope` 사용
6. **Null 안전성** — 불필요한 `!!` 사용 금지