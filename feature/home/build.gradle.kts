plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.soma369.laimory.feature.home"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
    }

    testOptions {
        // 예상하지 못한 실패는 BaseMviViewModel 이 Logger 로 남기고, 그 안이 android.util.Log 다.
        // 단위 테스트에는 android.jar 구현이 없어 실패 경로를 검증하는 것만으로 예외가 난다.
        unitTests.isReturnDefaultValues = true
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(project(":core:util"))

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
    implementation(libs.maps.compose)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
