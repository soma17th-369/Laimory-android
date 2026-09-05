plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// 이름이 buildType 이 아니라 **환경**을 가리킨다. qa 와 release 가 같은 서버를 본다.
val devBaseUrl = providers.gradleProperty("laimory.devBaseUrl").get()
val prodBaseUrl = providers.gradleProperty("laimory.prodBaseUrl").get()

android {
    namespace = "com.soma369.laimory.core.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 28

        // 새 build type은 개발 환경을 기본으로 사용하며 release만 아래에서 운영 환경으로 덮어쓴다.
        buildConfigField("String", "BASE_URL", "\"$devBaseUrl\"")
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
        buildConfigField("String", "PUBLISHED_TERMS_BASE_URL", "\"$prodBaseUrl\"")
    }

    buildTypes {
        debug {
            // 본문까지 봐야 하는 자리는 dev 를 겨눈 빌드뿐이다.
            buildConfigField("String", "HTTP_LOG_LEVEL", "\"BODY\"")
        }

        release {
            buildConfigField("String", "BASE_URL", "\"$prodBaseUrl\"")
            // 운영은 정본과 같은 서버라 대체할 곳이 없다. 빈 값이면 대체 경로가 아예 돌지 않는다.
            buildConfigField("String", "PUBLISHED_TERMS_BASE_URL", "\"\"")
            buildConfigField("String", "HTTP_LOG_LEVEL", "\"NONE\"")
        }

        /**
         * QA 는 release 를 상속받고 **다른 것만** 덮는다.
         *
         * 이 모듈은 defaultConfig 가 dev 를 기본으로 두고 release 만 운영으로 덮는 구조라,
         * qa 를 독립 선언해 `BASE_URL` 만 바꾸면 `PUBLISHED_TERMS_BASE_URL` 이 defaultConfig 의
         * 값으로 남아 release 와 다르게 동작한다. 앞으로 release 에 필드가 늘어도 같은 방식으로
         * 어긋나므로 상속이 유일하게 안전하다.
         *
         * `BASIC` 인 이유: QA 는 **운영 서버**를 본다. `BODY` 를 켜면 실사용자의 사진·위치·건강
         * 데이터가 logcat 에 통째로 남고, 응답을 버퍼링해 메모리·타이밍까지 릴리즈와 달라진다.
         * `HEADERS` 도 인증정보가 남을 여지가 있어 쓰지 않는다.
         */
        create("qa") {
            initWith(getByName("release"))
            // qa 를 아는 모듈은 app 과 여기뿐이다. 나머지 모듈은 release 변종을 쓰게 한다.
            matchingFallbacks += "release"
            buildConfigField("String", "HTTP_LOG_LEVEL", "\"BASIC\"")
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
