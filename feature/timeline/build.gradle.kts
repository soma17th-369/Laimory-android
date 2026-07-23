plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.soma369.laimory.feature.timeline"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
    }

    kotlin {
        jvmToolchain(17)
    }

    // 임시 테스트 전용(삭제 예정) — BuildConfig.DEBUG 로 Drive 테스트 버튼/호출을 debug 로만 제한.
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.lifecycle)
    implementation(libs.compose.activity)
    implementation(libs.coil.compose)
    debugImplementation(libs.compose.ui.tooling)

    // 임시 테스트 전용(삭제 예정) — testexport JSON 절단면 단위테스트용. android.jar 의
    // org.json 은 JVM 단위테스트에서 스텁이라, 실제 구현을 test 클래스패스에 얹는다.
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation("org.json:json:20240303")
}
