---
name: layer-role-guide
description: Android 신규 API/기능 추가 시 Request/Response, Query, Params, Domain Model, VO, DataSource, Repository, UseCase, ViewModel, MVI, Hilt, Navigation, UI 역할과 생성 기준입니다.
---

# 레이어 역할 가이드

신규 API나 기능을 추가할 때는 외부 API 계약과 앱 내부 도메인, 화면 상태를 분리하되, 필요 없는 파일을 무조건 만들지 않습니다.

## 0. 기본 원칙

- 필요한 경우에만 타입을 분리합니다.
- 레이어 경계는 넘기지 않습니다.
- API 계약은 `core:data`, 도메인 계약은 `core:domain`, 화면 상태는 `feature`에 둡니다.
- `Request`, `Response`, `Query`, `Params`, `UiModel`, VO를 모두 기계적으로 만들지 않습니다.
- 새 제품/도메인 용어가 들어가는 이름은 Ubiquitous Language 확인이 필요한지 판단합니다.

## 1. 생성 흐름

API 하나를 화면까지 연결할 때는 아래 순서로 책임을 나눕니다.

1. API 요청/응답 계약 확인
2. `Request`, `Response`, 필요 시 `Query` 작성
3. `RemoteDataSource`에 Retrofit 호출 위임
4. `Repository` interface에 도메인 계약 추가
5. `RepositoryImpl`에서 DataSource 호출과 Mapper 적용
6. `Domain Model`, 필요 시 VO 작성
7. `UseCase`, 필요 시 `Params` 작성
8. `ViewModel`에서 Intent 처리, UseCase 호출, State 갱신
9. `UiState`, `UiIntent`, `UiSideEffect`, 필요 시 `UiModel` 작성
10. `Route`, `Content`, `Screen` 작성
11. Hilt binding/module 연결
12. Navigation route 연결
13. 변경 범위에 맞는 검증 실행

## 2. 레이어별 책임 요약

| 구성요소 | 위치 | 책임 | 금지 사항 |
|---|---|---|---|
| Request | `core:data` | HTTP body 표현 | domain/UI에서 직접 사용 금지 |
| Response | `core:data` | HTTP response 표현 | domain/UI에서 직접 사용 금지 |
| Query | `core:data` | 복잡한 HTTP query parameter 묶음 | UseCase 입력으로 재사용 금지 |
| RemoteDataSource | `core:data` | Retrofit API 호출 위임 | 도메인 규칙 처리 금지 |
| Repository | `core:domain` | 기능에 필요한 데이터 계약 정의 | Retrofit/Room/DTO 직접 참조 금지 |
| RepositoryImpl | `core:data` | DataSource 호출, Mapper 적용 | UI 상태 판단 금지 |
| Domain Model | `core:domain` | 앱 내부 핵심 개념 표현 | Serialization/Retrofit 의존 금지 |
| VO | `core:domain` | 의미 있는 값 타입 표현 | 모든 primitive에 무조건 적용 금지 |
| UseCase | `core:domain` | 하나의 비즈니스 액션 표현 | 화면 상태 직접 처리 금지 |
| Params | `core:domain` | 복잡한 UseCase 입력 묶음 | API Query/Request와 혼용 금지 |
| ViewModel | `feature` | Intent 처리, UseCase 호출, State 갱신 | Composable 직접 참조 금지 |
| UiState | `feature` | 화면 렌더링에 필요한 상태 | domain 규칙 처리 금지 |
| UiIntent | `feature` | 사용자 이벤트 표현 | 데이터 로딩 결과 표현 금지 |
| UiSideEffect | `feature` | 일회성 효과 표현 | 지속 UI 상태 저장 금지 |
| UiModel | `feature` | 화면 표현 전용 모델 | 불필요한 domain 복제 금지 |

## 3. Request

`Request`는 HTTP body가 있는 API 요청 DTO입니다.

```kotlin
@Serializable
data class CreateMomentRequest(
    val content: String,
    val occurredAt: String,
)
```

사용 기준:
- 생성/수정처럼 request body가 있으면 작성합니다.
- 조회 API처럼 body가 없는 경우 `GetXxxRequest`를 만들지 않습니다.
- API 필드명과 serialization 요구사항을 그대로 반영합니다.
- domain model이나 UiState에서 직접 사용하지 않습니다.

권장 네이밍:
- `CreateXxxRequest`
- `UpdateXxxRequest`
- `DeleteXxxRequest`는 body가 있을 때만 사용

## 4. Response

`Response`는 서버 응답 구조를 표현하는 DTO입니다.

```kotlin
@Serializable
data class MomentResponse(
    val id: Long,
    val content: String,
    val occurredAt: String,
)
```

사용 기준:
- API 응답은 `Response`로 받고 domain model로 변환합니다.
- 리스트와 pagination metadata가 함께 오면 별도 response를 둡니다.

```kotlin
@Serializable
data class MomentListResponse(
    val items: List<MomentResponse>,
    val nextCursor: String?,
)
```

금지 사항:
- `Response`를 ViewModel이나 UI에서 직접 사용하지 않습니다.
- 서버 필드명 변경을 domain model에 전파하지 않습니다.

## 5. Query

`Query`는 HTTP query parameter가 많거나 재사용될 때만 둡니다.

```kotlin
data class GetMomentsQuery(
    val from: String,
    val to: String,
    val emotion: String?,
)
```

사용 기준:
- query parameter가 1-2개면 Retrofit 함수 파라미터로 직접 받습니다.
- query parameter가 많거나 같은 묶음을 여러 DataSource 호출에서 재사용하면 `Query`로 묶습니다.
- `Query`는 API 호출 형식이므로 `core:data`에 둡니다.

```kotlin
@GET("moments")
suspend fun getMoments(
    @Query("from") from: String,
    @Query("to") to: String,
): ApiResponse<List<MomentResponse>>
```

## 6. Domain Model

Domain Model은 앱 내부에서 사용하는 핵심 개념입니다.

```kotlin
data class Moment(
    val id: MomentId,
    val content: String,
    val occurredAt: LocalDateTime,
)
```

사용 기준:
- Repository interface와 UseCase는 Domain Model을 기준으로 계약을 맺습니다.
- API 응답 구조, DB entity 구조, 화면 표시 구조와 분리합니다.
- `@Serializable`, Retrofit, Room annotation을 붙이지 않습니다.

## 7. VO

VO는 Value Object입니다. domain 전체를 뜻하지 않고, 의미 있는 값 타입을 뜻합니다.

```kotlin
@JvmInline
value class MomentId(
    val value: Long,
)
```

사용 기준:
- 식별자, 좌표, 날짜 범위, 점수처럼 primitive 혼동 위험이 큰 값에 사용합니다.
- validation이 필요하거나 의미가 강한 값에 사용합니다.
- 모든 `String`, `Long`, `Int`를 VO로 감싸지 않습니다.

## 8. DataSource

DataSource는 Retrofit, Room, DataStore 같은 실제 데이터 접근을 담당합니다.

```kotlin
interface MomentRemoteDataSource {
    suspend fun createMoment(request: CreateMomentRequest): MomentResponse
    suspend fun getMoments(from: String, to: String): List<MomentResponse>
}
```

사용 기준:
- RemoteDataSource는 API 호출 결과를 DTO/Response로 반환합니다.
- 도메인 모델 변환은 RepositoryImpl 또는 Mapper에서 처리합니다.
- 에러 매핑 정책은 프로젝트 공통 규칙에 맞춥니다.

## 9. Repository

Repository interface는 domain layer의 데이터 계약입니다.

```kotlin
interface MomentRepository {
    suspend fun createMoment(content: String, occurredAt: LocalDateTime): Moment
    suspend fun getMoments(from: LocalDate, to: LocalDate): List<Moment>
}
```

사용 기준:
- `core:domain`에 둡니다.
- Request/Response/Query를 노출하지 않습니다.
- ViewModel은 RepositoryImpl이 아니라 UseCase를 통해 접근합니다.

## 10. RepositoryImpl

RepositoryImpl은 data layer에서 domain Repository를 구현합니다.

```kotlin
class MomentRepositoryImpl(
    private val remoteDataSource: MomentRemoteDataSource,
) : MomentRepository {
    override suspend fun createMoment(content: String, occurredAt: LocalDateTime): Moment {
        val response = remoteDataSource.createMoment(
            CreateMomentRequest(
                content = content,
                occurredAt = occurredAt.toString(),
            ),
        )
        return response.toDomain()
    }
}
```

사용 기준:
- DataSource 호출과 Mapper 적용을 담당합니다.
- UI 상태, snackbar, navigation 판단을 하지 않습니다.
- 서버 응답을 domain model로 변환한 뒤 반환합니다.

## 11. Mapper

Mapper는 data DTO와 domain model 사이 변환을 담당합니다.

```kotlin
fun MomentResponse.toDomain(): Moment =
    Moment(
        id = MomentId(id),
        content = content,
        occurredAt = LocalDateTime.parse(occurredAt),
    )
```

사용 기준:
- DTO/Response extension 형태를 우선합니다.
- 변환 로직이 길거나 의존성이 필요하면 별도 Mapper 클래스를 고려합니다.
- domain model이 API 필드명을 알게 하지 않습니다.

## 12. UseCase

UseCase는 ViewModel이 호출하는 하나의 비즈니스 액션입니다.

```kotlin
class GetMomentsUseCase(
    private val repository: MomentRepository,
) {
    suspend operator fun invoke(params: GetMomentsParams): List<Moment> =
        repository.getMoments(
            from = params.from,
            to = params.to,
        )
}
```

사용 기준:
- 하나의 기능/행동을 이름 붙인 클래스로 둡니다.
- 여러 Repository 조합, validation, 정책 판단이 있으면 UseCase에 둡니다.
- Clean Architecture 일관성을 위해 얇은 UseCase도 허용합니다.

## 13. Params

`Params`는 UseCase 입력이 복잡하거나 의미 있는 묶음일 때만 둡니다.

```kotlin
data class GetMomentsParams(
    val from: LocalDate,
    val to: LocalDate,
)
```

사용 기준:
- 입력이 없으면 `invoke()`를 사용합니다.
- 입력이 1개면 `invoke(id: MomentId)`처럼 직접 받습니다.
- 입력이 2개 이상이거나 날짜 범위, 필터, 정렬처럼 의미 있는 묶음이면 `XxxParams`를 둡니다.
- `Params`는 domain/usecase 입력이고 API `Query`와 분리합니다.

## 14. ViewModel

ViewModel은 UI 이벤트를 처리하고 UseCase를 호출해 UiState를 갱신합니다.

```kotlin
class TimelineViewModel(
    private val getMomentsUseCase: GetMomentsUseCase,
) : ViewModel() {
    fun sendIntent(intent: TimelineUiIntent) {
        when (intent) {
            TimelineUiIntent.Load -> loadMoments()
        }
    }

    private fun loadMoments() {
        viewModelScope.launch {
            val moments = getMomentsUseCase(
                GetMomentsParams(
                    from = state.value.from,
                    to = state.value.to,
                ),
            )
            updateState { copy(moments = moments) }
        }
    }
}
```

사용 기준:
- UI 이벤트를 Intent로 받습니다.
- UseCase 호출 결과를 UiState로 변환합니다.
- Composable, NavController, Context를 직접 들고 있지 않습니다.

## 15. UiState / UiIntent / UiSideEffect

MVI 계약은 화면 상태, 사용자 이벤트, 일회성 효과를 분리합니다.

```kotlin
data class TimelineUiState(
    val isLoading: Boolean = false,
    val moments: List<MomentUiModel> = emptyList(),
)

sealed interface TimelineUiIntent {
    data object Load : TimelineUiIntent
    data class ClickMoment(val id: MomentId) : TimelineUiIntent
}

sealed interface TimelineUiSideEffect {
    data class NavigateToDetail(val id: MomentId) : TimelineUiSideEffect
}
```

사용 기준:
- `UiState`는 화면 렌더링에 필요한 지속 상태입니다.
- `UiIntent`는 사용자 이벤트입니다.
- `UiSideEffect`는 navigation, snackbar 같은 일회성 효과입니다.
- navigation 상태를 `UiState`에 저장하지 않습니다.

## 16. UiModel

UiModel은 화면 표현이 domain model과 달라질 때만 둡니다.

```kotlin
data class MomentUiModel(
    val id: MomentId,
    val title: String,
    val formattedTime: String,
    val isSelected: Boolean,
)
```

사용 기준:
- formatted text, 선택 상태, 확장 상태처럼 화면 전용 값이 있으면 둡니다.
- domain model 그대로 렌더링해도 충분하면 만들지 않습니다.
- domain model의 단순 복제는 피합니다.

## 17. Route / Content / Screen

화면 Composable은 Route, Content, Screen 3계층을 따릅니다.

- `Route`: ViewModel 주입, state 수집, navigation callback 연결
- `Content`: SideEffect collect, dialog/snackbar 오케스트레이션
- `Screen`: 순수 stateless UI

자세한 규칙은 Android 아키텍처 스킬의 화면 컴포저블 3계층 구조를 따릅니다.

## 18. Hilt

Hilt는 구현체 연결과 의존성 주입 경계를 담당합니다.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindMomentRepository(
        impl: MomentRepositoryImpl,
    ): MomentRepository
}
```

사용 기준:
- Repository interface와 Impl 연결은 `@Binds`를 우선합니다.
- Retrofit service, DataSource, Mapper 등 생성이 필요한 객체는 `@Provides`를 사용합니다.
- feature ViewModel에는 UseCase를 주입합니다.

## 19. Navigation

Navigation은 Route 함수만 진입점으로 사용합니다.

```kotlin
composable<TimelineRoute> {
    TimelineRoute(
        innerPadding = innerPadding,
        onNavigateToDetail = { id -> navController.navigate(MomentDetailRoute(id.value)) },
    )
}
```

사용 기준:
- NavGraph에서는 `XxxRoute`만 호출합니다.
- `Screen`에 NavController를 넘기지 않습니다.
- 화면 이동은 `UiSideEffect`를 Content/Route에서 처리합니다.

## 20. 파일 증가 억제 기준

파일을 많이 만드는 것이 목적이 아닙니다. 아래 기준으로 필요한 타입만 만듭니다.

| 타입 | 생성 기준 |
|---|---|
| Request | API body가 있을 때 |
| Response | API 응답 계약이 있을 때 |
| Query | query parameter가 많거나 재사용될 때 |
| Params | UseCase 입력이 2개 이상이거나 의미 있는 묶음일 때 |
| UiModel | 화면 표현이 domain model과 달라질 때 |
| VO | primitive 혼동 위험이나 validation 필요성이 있을 때 |
