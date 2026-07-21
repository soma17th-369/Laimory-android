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
    }

    buildTypes {
        release {
            buildConfigField("String", "BASE_URL", "\"$releaseBaseUrl\"")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation(project(":core:domain"))

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
