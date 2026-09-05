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

// 이름이 buildType 이 아니라 **환경**을 가리킨다. qa 와 release 가 같은 서버(prod)를 보므로
// "release = 운영" 이라는 종전 이름은 두 축을 한 이름에 묶어 오해를 만든다.
val devBaseUrl = providers.gradleProperty("laimory.devBaseUrl").get()
val prodBaseUrl = providers.gradleProperty("laimory.prodBaseUrl").get()

/** 스토어에 등재되는 applicationId. suffix 가 붙는 빌드도 스토어 주소는 이 값을 쓴다. */
val storeApplicationId = "com.soma369.laimory"

val localProperties =
    Properties().apply {
        rootProject.file("local.properties").takeIf(File::exists)?.inputStream()?.use(::load)
    }

/**
 * 저장소에 두지 않는 값. `local.properties`(gitignore) 를 먼저 보고 없으면 Gradle 속성을 본다.
 *
 * CI 는 `local.properties` 없이 `-Plaimory.xxx=` 나 `ORG_GRADLE_PROJECT_` 환경변수로 넘긴다.
 */
fun secret(name: String): String? = localProperties.getProperty(name) ?: providers.gradleProperty(name).orNull

/**
 * Google Maps Android API 키.
 *
 * 값이 없으면 빈 문자열로 두어 **빌드는 성공하고** 지도만 대체 안내 상태로 떨어진다 — 키 없는
 * 개발 환경이나 CI 에서 빌드가 깨지지 않아야 한다.
 */
val mapsApiKey: String = secret("laimory.mapsApiKey") ?: ""

/**
 * 업로드 키스토어. `qa` 와 `release` 가 함께 쓴다.
 *
 * 파일이 없으면 `null` 이고 서명 설정 자체를 만들지 않는다 — 자격증명 없는 환경에서는 unsigned
 * 로 떨어질 뿐 configuration 단계가 깨지지 않아야 한다(지도 키와 같은 방침).
 *
 * `~` 는 셸이 풀어 주는 것이라 properties 파일에 그대로 적히면 경로가 맞지 않는다. 여기서 푼다.
 *
 * 상대 경로는 `rootProject.file` 로 푼다. `File(path)` 는 JVM 작업 디렉터리를 기준으로 삼는데
 * 그것은 데몬이 어디서 떴는지에 달려 있어, 다른 디렉터리에서 `gradlew -p <저장소>` 로 돌리면
 * 저장소 안에 키가 있어도 못 찾고 **조용히 unsigned 로 떨어진다.**
 */
val uploadKeystore: File? =
    secret("laimory.uploadStoreFile")
        ?.replaceFirst(Regex("^~"), System.getProperty("user.home"))
        ?.let(rootProject::file)
        ?.takeIf(File::exists)

fun String.toAppLinkHost(): String =
    URI(this).host?.takeIf(String::isNotBlank)
        ?: error("App Link base URL must contain a valid host: $this")

android {
    namespace = "com.soma369.laimory"
    compileSdk = 36

    signingConfigs {
        if (uploadKeystore != null) {
            create("upload") {
                storeFile = uploadKeystore
                storePassword = secret("laimory.uploadStorePassword")
                keyAlias = secret("laimory.uploadKeyAlias")
                keyPassword = secret("laimory.uploadKeyPassword")
            }
        }
    }

    defaultConfig {
        applicationId = storeApplicationId
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 매니페스트의 지도 키와 게이트 판정이 같은 값을 보게 한 곳에서 함께 심는다.
        manifestPlaceholders["mapsApiKey"] = mapsApiKey
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")

        // 스토어를 여는 주소. debug 는 `.debug`, qa 는 `.qa` 가 붙으므로 자기 applicationId 를
        // 쓰면 스토어에 없는 앱을 열게 된다. 세 빌드가 모두 이 값을 본다.
        buildConfigField("String", "STORE_APPLICATION_ID", "\"$storeApplicationId\"")
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            buildConfigField("String", "AUTH_CALLBACK_HOST", "\"${devBaseUrl.toAppLinkHost()}\"")
            buildConfigField("int", "SOURCE_ITEM_RETENTION_DAYS", "365")
            buildConfigField("String", "APP_LOG_LEVEL", "\"VERBOSE\"")
            manifestPlaceholders["authCallbackHost"] = devBaseUrl.toAppLinkHost()
        }

        release {
            isMinifyEnabled = true
            // minify 와 짝이다. 코드만 줄이고 리소스를 그대로 두면 쓰지 않는 리소스가 남는다.
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("upload")
            buildConfigField("String", "AUTH_CALLBACK_HOST", "\"${prodBaseUrl.toAppLinkHost()}\"")
            buildConfigField("int", "SOURCE_ITEM_RETENTION_DAYS", "30")
            buildConfigField("String", "APP_LOG_LEVEL", "\"WARN\"")
            manifestPlaceholders["authCallbackHost"] = prodBaseUrl.toAppLinkHost()
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }

        /**
         * QA 검증 빌드. 운영 서버를 보고 R8 을 켠 채로 릴리즈와 같은 조건을 만든다.
         *
         * `initWith(release)` 로 만든다 — 복붙하면 이후 release 에 붙는 설정이 여기로 따라오지
         * 않아 반드시 어긋난다. 릴리즈와 **다른 것만** 아래에서 덮는다.
         *
         * `isMinifyEnabled` 를 끄지 않는다. R8 은 빌드가 아니라 런타임에 터지므로, 난독화 없는
         * QA 를 통과하고 출시본에서만 터질 수 있다. `isDebuggable` 도 켜지 않는다 — 성능 특성이
         * 달라져 QA 가 릴리즈를 대변하지 못한다.
         */
        create("qa") {
            initWith(getByName("release"))
            applicationIdSuffix = ".qa"
            // 이 모듈만 qa 를 안다. 나머지 모듈은 release 변종을 쓰게 한다.
            matchingFallbacks += "release"
            buildConfigField("String", "APP_LOG_LEVEL", "\"DEBUG\"")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    testOptions {
        // 내비게이션 로직이 Logger 를 거치는데 그 안이 android.util.Log 다. 단위 테스트에는
        // android.jar 구현이 없어 호출만으로 예외가 나므로, 로그 한 줄 때문에 검증 경로가
        // 막히지 않게 한다. core:data 가 같은 이유로 같은 설정을 쓴다.
        unitTests.isReturnDefaultValues = true
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
    implementation(project(":feature:terms"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:collection"))
    implementation(project(":core:domain"))
    implementation(project(":core:util"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
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
