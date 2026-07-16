plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

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
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
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
    implementation(project(":feature:settings"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:collection"))
    implementation(project(":core:domain"))
    implementation(project(":core:util"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)

    // Firebase — 초기 연동(#128). 제품 SDK 는 후속 이슈에서 추가; 지금은 초기화 확인용 common 만.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.common)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    // material3 1.4+ 는 material-icons 를 트랜지티브로 제공하지 않아 바텀탭 아이콘용으로 명시 추가.
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.activity)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.androidx.browser)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
