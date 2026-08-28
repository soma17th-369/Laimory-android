plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.soma369.laimory.core.ui"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
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
    implementation(project(":core:util"))
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.lifecycle)
    implementation(libs.compose.activity)
    debugImplementation(libs.compose.ui.tooling)

    // 헬스는 다른 소스와 달리 자체 권한 모델을 쓴다. 설정·온보딩이 같은 권한 경계를 보므로
    // 판정도 여기에 둔다 — 화면마다 Health Connect 를 따로 물으면 답이 갈린다.
    implementation(libs.health.connect)

    implementation(libs.lifecycle.viewmodel)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
}
