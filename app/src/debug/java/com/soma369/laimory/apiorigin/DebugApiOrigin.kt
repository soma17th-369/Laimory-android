package com.soma369.laimory.apiorigin

import com.soma369.laimory.core.data.BuildConfig as DataBuildConfig

/**
 * debug 빌드에서 고를 수 있는 서버. 이 목록은 debug 소스셋에만 있어 release·qa 산출물에 실리지 않는다.
 *
 * 서버를 더할 때 그 도메인으로 **로그인**까지 하려면, 서버가 그 도메인의 assetlinks.json 에 debug 앱을
 * 등록해 두어야 하고 debug 매니페스트에 그 host 의 `/auth/app` App Link 도 함께 더해야 한다.
 */
enum class DebugApiOrigin(
    val label: String,
    val baseUrl: String,
) {
    /** 지금까지의 debug 기본값(`laimory.devBaseUrl`). */
    DEV("dev.laimory.app (기본)", DataBuildConfig.BASE_URL),
    API("api.laimory.app", "https://api.laimory.app/"),
    ;

    companion object {
        val DEFAULT = DEV

        fun fromNameOrDefault(name: String?): DebugApiOrigin = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
