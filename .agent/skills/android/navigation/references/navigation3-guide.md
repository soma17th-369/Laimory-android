# Navigation 3 구현 가이드 (라우트 테이블)

`androidx.navigation3`(Nav3) 기반 앱 내비게이션의 구체 규칙. 단일 `GenericNavKey` + 라우트 테이블(`appRoutes`) 구조.

## 의존성

- `androidx.navigation3:navigation3-runtime` — `NavKey`, `NavBackStack`, `rememberNavBackStack`, `entryProvider {}`
- `androidx.navigation3:navigation3-ui` — `NavDisplay`
- app 모듈에 `kotlin-serialization` 플러그인 필요 (`GenericNavKey`가 `@Serializable`)
- version catalog의 `nav3Core` 버전으로 핀한다. **Compose BOM은 Nav3를 커버하지 않으므로** 직접 핀한다.
- ⚠️ `androidx.lifecycle:lifecycle-viewmodel-navigation3`(per-entry ViewModel 스코프 데코레이터)는 **AGP 9.1 / compileSdk 37**을 요구한다. 현재 툴체인(AGP 8.9 / SDK 36)에선 쓰지 않으며 `hiltViewModel()`은 activity 스코프로 동작한다. 툴체인 범프 시 도입한다.

## 구조 한눈에

```
[feature/ViewModel] --navigateTo(Page)--> [NavigationHelper(port)] --NavSignal--> [AppNavHost] --조작--> backStack
       │                                                                              │
   도메인 Page                                                          appRouteByPath[path].render(args)
```

- **도메인**(`core.domain.navigation`): `NavRoute(path, args)` · `Page { toRoute() }` · 화면 카탈로그(`HomePage`, `Feature1Page`). Nav3/`NavController` 무의존.
- **app**(`navigation`): `GenericNavKey(path, args)` 단일 키 · `AppRoute`+`appRoutes` 라우트 테이블 · `AppNavHost`(NavDisplay 디스패처) · `LaimoryNavGraph`(Scaffold+SnackbarHost 소유).

## 도메인: Page / NavRoute

```kotlin
data class NavRoute(val path: String, val args: Map<String, String> = emptyMap())
interface Page { fun toRoute(): NavRoute }

data object HomePage : Page {
    const val PATH = "/home"
    override fun toRoute() = NavRoute(PATH)
}
```

- 화면 목적지는 도메인 `Page` 객체로 정의한다. 인자가 생기면 `data class ... : Page`로 바꾸고 `toRoute()`에서 `args`를 채운다(복잡 타입은 JSON 문자열).
- **카탈로그를 `core.domain`에 두는 이유**: feature A가 feature B 화면으로 이동할 때 feature 간 직접 의존 없이 `Page`만 지목하면 된다.
- 호출부 타입 안전성은 `Page`/`*Args`가, 백스택 표현은 `GenericNavKey`가 맡는다(키 자체는 stringly).

## 단일 키 + 라우트 테이블

```kotlin
@Serializable
data class GenericNavKey(val path: String, val args: Map<String, String> = emptyMap()) : NavKey {
    companion object { fun of(route: NavRoute) = GenericNavKey(route.path, route.args) }
}

data class AppRoute(
    val path: String,
    val isBottomTab: Boolean = false,
    val render: @Composable (innerPadding: PaddingValues, args: Map<String, String>) -> Unit,
)

val appRoutes = listOf(
    AppRoute(HomePage.PATH) { pad, _ -> HomeRoute(pad, ...) },
    AppRoute(Feature1Page.PATH) { pad, _ -> Feature1Route(pad) },
)
val appRouteByPath = appRoutes.associateBy { it.path }
```

- **모든 목적지가 `GenericNavKey` 하나**. 화면 분기는 `path` → `appRouteByPath`.
- `@Serializable`이라야 backStack이 config change / process death를 넘어 복원된다.
- **동일 키(path+args) 중복 push 금지** — Nav3 contentKey는 키별 1회만 유효하다.

## backStack + NavDisplay (AppNavHost)

app이 backStack을 소유하고 단일 Scaffold content에 NavDisplay를 둔다. NavDisplay는 `GenericNavKey` 하나만 디스패치한다.

```kotlin
@Composable
fun AppNavHost(backStack: NavBackStack<NavKey>, innerPadding: PaddingValues) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },   // predictive back 내장
        entryProvider = entryProvider {
            entry<GenericNavKey> { navKey ->
                val route = appRouteByPath[navKey.path] ?: return@entry
                route.render(innerPadding, navKey.args)
            }
        },
    )
}
```

## 화면(목적지) 추가 절차

1. 도메인에 `Page` 객체 추가 (`PATH` + `toRoute()`).
2. `appRoutes`에 `AppRoute(ThePage.PATH) { pad, args -> TheRoute(pad, ...) }` 한 줄 추가.
3. 이동은 `navigationHelper.navigateTo(ThePage)` (→ 아래 단일 채널). feature는 Nav3/키를 몰라도 되고 도메인 `Page`만 안다.
4. 인자는 `Page`의 프로퍼티 → `toRoute().args`로 직렬화, `render`에서 `args`로 디코딩.

## ViewModel 스코프

- 현재 `hiltViewModel()`은 **activity 스코프**다(per-entry 데코레이터 미도입). 화면별 VM이 activity 생명주기 동안 유지된다.
- per-entry 스코프가 필요하면 `lifecycle-viewmodel-navigation3`의 `rememberViewModelStoreNavEntryDecorator()`를 `entryDecorators`에 추가한다 (AGP 9.1 / SDK 37 필요).

## 딥링크 설계 (목표)

`GenericNavKey(path, args)`가 URI와 동형(path + string map)이라 딥링크·앱링크 진입이 자연스럽다. (앱링크 = 검증된 https 딥링크. 차이는 매니페스트 intent-filter + `assetlinks.json` 호스팅뿐이고, 앱 내 `Uri → NavRoute` 파싱 코드는 동일)

1. Activity `onCreate`에서 `intent.data`(Uri) → `NavRoute`(path, query→args) 파싱.
2. 대상 `AppRoute`의 `syntheticStack(args)`로 부모 체인을 포함한 시작 백스택을 구성(일반 화면은 자기 자신만). → `rememberNavBackStack(*startStack)`로 시드.
3. 웜 스타트(실행 중 도착)는 기존 스택 보존 + 대상만 bring-to-front.

> `syntheticStack` 필드·URI 파서·`NavSignal.DeepLink`는 첫 딥링크가 정의될 때 도입한다(현재 미구현).

## 안티패턴

- ❌ 문자열 route / `NavController` / `NavHost`
- ❌ 화면별 typed `NavKey` 남발 (단일 `GenericNavKey`로 통일)
- ❌ domain/usecase가 Nav3·`NavController`를 직접 참조 (도메인은 `Page`/`NavRoute`까지만)
- ❌ Composable에 이동 람다 배선 (→ `NavigationHelper.navigateTo(Page)`)
- ❌ 동일 키 중복 push
- ❌ `GenericNavKey`에 `@Serializable` 누락(복원 깨짐)
