---
name: android-navigation
description: Navigation 3(androidx.navigation3) 라우트 테이블 기반 앱 내비게이션 규칙입니다.
---

# Android 내비게이션 (Navigation 3 · 라우트 테이블)

앱 화면 전환은 **Navigation 3**(`androidx.navigation3`)로 구현합니다. 클래식 Navigation Compose(`NavHost`/`NavController` + 문자열 route)는 사용하지 않습니다.

**언제 참조하나요:**
- 새 화면(목적지) 추가 시
- 화면 간 이동 / 뒤로가기 구현 시
- 딥링크·앱링크 진입 설계 시
- 공통 정책성 이동(로그인·홈 등)을 다룰 때

## 핵심 원칙

- **단일 백스택 키 `GenericNavKey(path, args)`**: 모든 목적지를 이 한 타입으로 표현하고, 화면 분기는 `path`로 한다(`appRouteByPath`). 화면별 typed 키를 만들지 않는다.
- **도메인 `Page` + `NavRoute`**: 각 화면은 도메인에 `Page` 객체(`toRoute(): NavRoute`)를 정의한다. 호출부 타입 안전성은 `Page`가, 백스택 표현은 `GenericNavKey`가 맡는다. domain은 Nav3·`NavController`를 알지 않는다.
- **라우트 테이블 `appRoutes`**: `AppRoute(path, isBottomTab, render)`의 단일 리스트. 새 화면 = 여기에 한 줄.
- **backStack은 app이 소유**: `rememberNavBackStack(...)` → `NavBackStack<NavKey>`를 app이 들고 조작. `NavController` 없음.
- **이동은 `NavigationHelper` 단일 채널**: feature/ViewModel은 `navigationHelper.navigateTo(Page)`·`navigateToBack()`을 부른다. Composable에 이동 람다를 배선하지 않는다. presentation이 `NavSignal`을 수집해 backStack 조작으로 매핑한다.
- **단일 Scaffold / SnackbarHost**: 앱 chrome은 하나의 `Scaffold`가 소유하고 `NavDisplay`가 content를 채운다.

## 상세 가이드

[Navigation 3 구현 가이드](references/navigation3-guide.md) — 라우트 테이블 구조, `Page`/`NavRoute`/`GenericNavKey`, 화면 추가 절차, `NavigationHelper` 단일 채널, 딥링크 설계, ViewModel 스코프.
