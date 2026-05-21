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

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.lifecycle)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.lifecycle.viewmodel)
    implementation(libs.coroutines.android)
}