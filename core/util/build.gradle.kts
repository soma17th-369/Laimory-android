plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.soma369.laimory.core.util"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
    }

    buildFeatures {
        buildConfig = true
    }

    testOptions {
        // Logger 안이 android.util.Log 다. 단위 테스트에는 android.jar 구현이 없어 호출만으로
        // 예외가 나므로, 로그 한 줄 때문에 검증 경로가 막히지 않게 한다(app · core:data 와 같은 설정).
        unitTests.isReturnDefaultValues = true
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    testImplementation(libs.junit)
}
