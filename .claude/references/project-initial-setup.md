# Project Initial Setup

## 서비스 개요
- 서비스명: Laimory
- 설명: 모바일 기반 AI 라이프 로깅 앱 (사용자의 일상을 AI가 자동 수집·구조화·분석)
- 플랫폼: Native Android (Kotlin)

---

## 기술 스택

| 항목 | 기술 |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | Clean Architecture + MVI |
| DI | Hilt |
| 비동기 | Coroutines + Flow |
| 네트워크 | Retrofit + OkHttp |
| 로컬 DB | Room |
| 설정 저장 | DataStore (Proto) |
| 직렬화 | kotlinx.serialization |
| 이미지 | Coil |
| 코드 스타일 | KtLint |
| Build | Gradle Kotlin DSL (.kts) + Version Catalog |

---

## 프로젝트 기본 정보

```
Package name    : com.soma369.laimory
Minimum SDK     : API 28 (Android 9.0)
Target SDK      : 최신
Repository      : laimory-android
```

---

## 모듈 구조 (하이브리드)

```
:app
:feature:home          ← 초반에는 timeline/search/insight 전부 여기
:core:domain           ← 순수 Kotlin only, 외부 의존성 없음
:core:data             ← Repository 구현체, DataSource
:core:ui               ← 공통 Composable, Theme, DesignSystem
```

### 모듈별 역할

- `:app` → 진입점, Hilt Application, Navigation 루트
- `:feature:home` → UI(Screen) + ViewModel만 포함
- `:core:domain` → Model, Repository interface, UseCase
- `:core:data` → RepositoryImpl, RemoteDataSource, LocalDataSource, Mapper
- `:core:ui` → 공통 컴포넌트, MVI 베이스 클래스, Theme

### 나중에 분리 예정 (기능 완성 후)
```
:feature:timeline
:feature:search
:feature:insight
:feature:relationship
:core:network
:core:database
```

---

## 레이어 구조 및 역할

### Presentation Layer (:feature:xxx)
- Screen (Jetpack Compose)
- ViewModel (MVI + StateFlow)
- UiState / UiIntent / UiSideEffect 정의

### Domain Layer (:core:domain)
- 순수 Kotlin, Android 의존성 없음
- UseCase (단일 책임, operator fun invoke)
- Repository Interface (추상화)
- Domain Model

### Data Layer (:core:data)
- RepositoryImpl (Domain interface 구현)
- RemoteDataSource (Retrofit)
- LocalDataSource (Room, DataStore)
- DTO → Domain Model Mapper

---

## MVI 패턴

```kotlin
// 모든 Feature ViewModel이 상속받는 베이스 클래스
abstract class MviViewModel<S : UiState, I : UiIntent, E : UiSideEffect>(
    initialState: S
) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _sideEffect = Channel<E>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    fun sendIntent(intent: I) {
        viewModelScope.launch { handleIntent(intent) }
    }

    protected abstract suspend fun handleIntent(intent: I)

    protected fun updateState(block: S.() -> S) {
        _state.update { it.block() }
    }

    protected suspend fun sendEffect(effect: E) {
        _sideEffect.send(effect)
    }
}
```

---

## API 공통 응답 구조

백엔드와 협의된 공통 응답 포맷:

```kotlin
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?,
    val error: ApiError?
)

data class ApiError(
    val code: String,
    val message: String
)
```

### ApiException 구조

```kotlin
sealed class ApiException(override val message: String) : IOException(message) {
    class UnknownException(message: String? = null) : ApiException(message ?: "알 수 없는 에러 발생")
    class NetworkException : ApiException("네트워크 에러 발생")
    class UnauthorizedException(message: String? = null) : ApiException(message ?: "인증이 필요합니다")
    class ServerException(message: String? = null) : ApiException(message ?: "서버 에러 발생")
    class ClientException(message: String? = null) : ApiException(message ?: "잘못된 요청입니다")
    class ConflictException(message: String? = null) : ApiException(message ?: "중복된 요청입니다")

    companion object {
        fun fromCode(code: Int, message: String? = null): ApiException =
            when (code) {
                401, 403 -> UnauthorizedException(message)
                409 -> ConflictException(message)
                in 400..499 -> ClientException(message)
                in 500..599 -> ServerException(message)
                else -> UnknownException(message)
            }
    }
}
```

---

## 코드 컨벤션

### 파일 네이밍
```
Screen          → TimelineScreen.kt
ViewModel       → TimelineViewModel.kt
UiState         → TimelineUiState.kt
UseCase         → GetTimelineUseCase.kt
Repository      → TimelineRepository.kt (interface)
RepositoryImpl  → TimelineRepositoryImpl.kt
DTO             → TimelineDto.kt
Entity          → TimelineEntity.kt
Mapper          → TimelineMapper.kt
```

### 패키지 구조 (feature 모듈)
```
com.laimory.feature.home
├── screen/
├── viewmodel/
├── state/        ← UiState, UiIntent, UiSideEffect
└── component/    ← 이 feature에서만 쓰는 Composable
```

### 패키지 구조 (core:domain)
```
com.laimory.domain
├── model/
├── repository/
└── usecase/
    ├── timeline/
    ├── search/
    └── insight/
```

### 브랜치 네이밍
```
feat/#이슈번호-작업내용       feat/#12-timeline-location
fix/#이슈번호-작업내용        fix/#34-date-sort-bug
chore/#이슈번호-작업내용      chore/#5-ktlint-setup
hotfix/#이슈번호-작업내용     hotfix/#99-crash-fix
release/버전                 release/1.0.0
```

### 커밋 메시지
```
feat: 타임라인 위치 표시 구현 (#12)
fix: 날짜 정렬 오류 수정 (#34)
chore: KtLint 세팅 (#5)
refactor: Repository 레이어 분리 (#18)
```

---

## 브랜치 전략 (Git Flow)

```
main      → 스토어 배포 버전 (직접 push 금지)
develop   → 개발 통합 브랜치 (직접 push 금지)
release/x.x.x → 배포 준비 브랜치
feat/xx   → 기능 개발 (develop에서 분기)
fix/xx    → 버그 수정
hotfix/xx → main 긴급 버그 수정
chore/xx  → 기타 작업
```

### 버전 네이밍
```
v1.0.0   최초 출시
v1.1.0   새 기능 추가
v1.1.1   버그 수정
v2.0.0   대규모 변경
```

---

## 프로젝트 세팅 순서

아래 순서대로 세팅을 진행해주세요.

### 1단계: libs.versions.toml 작성
`gradle/libs.versions.toml`에 모든 의존성 버전을 정의합니다.

```toml
[versions]
kotlin = "2.0.0"
compose-bom = "2024.06.00"
hilt = "2.51"
retrofit = "2.11.0"
room = "2.6.1"
coroutines = "1.8.1"
coil = "2.7.0"
datastore = "1.1.1"
ktlint = "12.1.1"
ksp = "2.0.0-1.0.22"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-activity = { group = "androidx.activity", name = "activity-compose", version = "1.9.0" }
compose-navigation = { group = "androidx.navigation", name = "navigation-compose", version = "2.7.7" }
compose-lifecycle = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version = "2.7.0" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }

# Network
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-serialization = { group = "com.squareup.retrofit2", name = "converter-kotlinx-serialization", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version = "4.12.0" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version = "4.12.0" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Coroutines
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

# Coil
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }

# DataStore
datastore = { group = "androidx.datastore", name = "datastore", version.ref = "datastore" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# Serialization
serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version = "1.6.3" }

[plugins]
android-application = { id = "com.android.application", version = "8.5.0" }
android-library = { id = "com.android.library", version = "8.5.0" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
ktlint = { id = "org.jlleitschuh.gradle.ktlint", version.ref = "ktlint" }
```

### 2단계: settings.gradle.kts 작성
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "laimory"

include(":app")
include(":feature:home")
include(":core:domain")
include(":core:data")
include(":core:ui")
```

### 3단계: 루트 build.gradle.kts 작성
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint)
}

ktlint {
    version.set("1.3.0")
    android.set(true)
    outputToConsole.set(true)
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}
```

### 4단계: 각 모듈 build.gradle.kts 작성

**:app**
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.laimory"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.laimory"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":feature:home"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:domain"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.compose.navigation)
}
```

**:core:domain**
```kotlin
plugins {
    id("java-library")
    alias(libs.plugins.kotlin.android) // 순수 Kotlin 모듈
}

// Android 의존성 없음 - 순수 Kotlin only
dependencies {
    implementation(libs.coroutines.android)
}
```

### 5단계: MVI 베이스 클래스 작성
`:core:ui` 모듈에 아래 파일 생성:

```
core/ui/src/main/java/com/laimory/core/ui/base/
├── MviViewModel.kt
├── UiState.kt
├── UiIntent.kt
└── UiSideEffect.kt
```

### 6단계: KtLint Git Hook 연결
```bash
./gradlew addKtlintCheckGitPreCommitHook
```

---

## .gitignore 주요 항목

```
# Local secrets
local.properties
*.keystore
*.jks

# API Keys (절대 커밋 금지)
# local.properties에서 관리
```

## local.properties (커밋 금지)

```properties
BASE_URL=https://api.laimory.com
KAKAO_API_KEY=your_key_here
```

## BuildConfig에서 접근

```kotlin
// app/build.gradle.kts
defaultConfig {
    buildConfigField("String", "BASE_URL", "\"${properties["BASE_URL"]}\"")
}
```