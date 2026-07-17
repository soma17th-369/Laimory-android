plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.soma369.laimory.core.collection"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    kotlin {
        jvmToolchain(17)
    }

    // migration 계측 테스트가 버전별 스키마 JSON 을 assets 로 읽을 수 있게 한다
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
}

// Room 스키마 JSON 을 버전 관리해 migration 작성/검증의 기준으로 삼는다
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:util"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.serialization.json)

    implementation(libs.datastore.preferences)

    implementation(libs.exifinterface)

    implementation(libs.health.connect)

    implementation(libs.coroutines.android)

    implementation(libs.lifecycle.process)

    implementation(libs.play.services.location)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.coroutines.test)
}
