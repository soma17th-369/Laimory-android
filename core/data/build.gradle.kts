import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val localProperties =
    Properties().apply {
        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
    }

fun Properties.requireBaseUrl(name: String): String =
    requireNotNull(getProperty(name)) { "Define $name in local.properties" }
        .trim()
        .removeSurrounding("\"")

val debugBaseUrl = localProperties.requireBaseUrl("DEV_BASE_URL")
val releaseBaseUrl = localProperties.requireBaseUrl("RELEASE_BASE_URL")

android {
    namespace = "com.soma369.laimory.core.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 28

        // 서버 API 계약의 {applicationVersion} — versionName 처럼 빌드 설정 단일 지점에서 관리한다.
        buildConfigField("String", "API_APP_VERSION", "\"v1\"")
    }

    buildTypes {
        debug {
            // 도메인 루트만 담고 API prefix/버전 조합은 ApiPrefix가 담당한다.
            buildConfigField("String", "BASE_URL", "\"$debugBaseUrl\"")
        }
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
