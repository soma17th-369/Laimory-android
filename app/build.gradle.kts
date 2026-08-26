import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

val debugBaseUrl = providers.gradleProperty("laimory.debugBaseUrl").get()
val releaseBaseUrl = providers.gradleProperty("laimory.releaseBaseUrl").get()

/**
 * Google Maps Android API 키.
 *
 * `local.properties` 는 gitignore 대상이라 키가 저장소에 올라가지 않는다. 값이 없으면 빈
 * 문자열로 두어 **빌드는 성공하고** 지도만 대체 안내 상태로 떨어진다 — 키 없는 개발 환경이나
 * CI 에서 빌드가 깨지지 않아야 한다.
 */
val mapsApiKey: String =
    Properties()
        .apply {
            rootProject.file("local.properties").takeIf(File::exists)?.inputStream()?.use(::load)
        }.getProperty("laimory.mapsApiKey")
        ?: providers.gradleProperty("laimory.mapsApiKey").orNull
        ?: ""

fun String.toAppLinkHost(): String =
    URI(this).host?.takeIf(String::isNotBlank)
        ?: error("App Link base URL must contain a valid host: $this")

android {
    namespace = "com.soma369.laimory"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.soma369.laimory"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 매니페스트의 지도 키와 게이트 판정이 같은 값을 보게 한 곳에서 함께 심는다.
        manifestPlaceholders["mapsApiKey"] = mapsApiKey
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            buildConfigField("String", "AUTH_CALLBACK_HOST", "\"${debugBaseUrl.toAppLinkHost()}\"")
            buildConfigField("int", "SOURCE_ITEM_RETENTION_DAYS", "365")
            manifestPlaceholders["authCallbackHost"] = debugBaseUrl.toAppLinkHost()
        }

        release {
            isMinifyEnabled = true
            buildConfigField("String", "AUTH_CALLBACK_HOST", "\"${releaseBaseUrl.toAppLinkHost()}\"")
            buildConfigField("int", "SOURCE_ITEM_RETENTION_DAYS", "30")
            manifestPlaceholders["authCallbackHost"] = releaseBaseUrl.toAppLinkHost()
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
    implementation(project(":feature:home"))
    implementation(project(":feature:feature1"))
    implementation(project(":feature:collection"))
    implementation(project(":feature:timeline"))
    implementation(project(":feature:login"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:settings"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:collection"))
    implementation(project(":core:domain"))
    implementation(project(":core:util"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.lifecycle.process)
    implementation(libs.coroutines.android)

    // Firebase — 초기 연동(#128). 제품 SDK 는 후속 이슈에서 추가; 지금은 초기화 확인용 common 만.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.common)
    implementation(libs.firebase.installations)
    implementation(libs.firebase.messaging)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    // material3 1.4+ 는 material-icons 를 트랜지티브로 제공하지 않아 바텀탭 아이콘용으로 명시 추가.
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.activity)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)
    implementation(libs.work.runtime.ktx)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.androidx.browser)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
