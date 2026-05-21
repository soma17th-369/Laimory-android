plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.soma369.laimory.core.domain"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation(libs.coroutines.android)
}