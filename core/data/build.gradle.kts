plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val debugBaseUrl = providers.gradleProperty("laimory.debugBaseUrl").get()
val releaseBaseUrl = providers.gradleProperty("laimory.releaseBaseUrl").get()

android {
    namespace = "com.soma369.laimory.core.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 28

        // 새 build type은 개발 환경을 기본으로 사용하며 release만 아래에서 운영 환경으로 덮어쓴다.
        buildConfigField("String", "BASE_URL", "\"$debugBaseUrl\"")
        // 서버 API 계약의 {applicationVersion} — versionName 처럼 빌드 설정 단일 지점에서 관리한다.
        buildConfigField("String", "API_APP_VERSION", "\"v1\"")

        // 게시된 약관 원문의 정본 위치. **열람 링크에만** 쓰는 임시 대체 경로다.
        //
        // 개발 서버 catalog 가 아직 비어 있어 debug 빌드에서는 약관 주소를 받을 수 없다. 원문은
        // 환경과 무관한 하나의 공개 문서이므로, 이 환경이 빈 응답을 주면 게시된 쪽에서 주소만
        // 가져온다. 동의 판정·등록은 절대 이 값을 쓰지 않는다 — 동의는 그 환경 DB 의 문서에
        // 기록되므로 다른 환경의 버전을 보내면 전부 거절된다.
        //
        // 개발 catalog 에 seed 가 들어가면 이 필드를 지운다.
        buildConfigField("String", "PUBLISHED_TERMS_BASE_URL", "\"$releaseBaseUrl\"")
    }

    buildTypes {
        release {
            buildConfigField("String", "BASE_URL", "\"$releaseBaseUrl\"")
            // 운영은 정본과 같은 서버라 대체할 곳이 없다. 빈 값이면 대체 경로가 아예 돌지 않는다.
            buildConfigField("String", "PUBLISHED_TERMS_BASE_URL", "\"\"")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    kotlin {
        jvmToolchain(17)
    }

    testOptions {
        // 저장소가 Logger 를 거치는데 그 안이 android.util.Log 다. 단위 테스트에는 android.jar
        // 구현이 없어 호출만으로 예외가 나므로, 로그 한 줄 때문에 검증 경로가 막히지 않게 한다.
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:util"))

    implementation(libs.androidx.core.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.serialization.json)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore)
    implementation(libs.datastore.preferences)

    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockwebserver)
}
