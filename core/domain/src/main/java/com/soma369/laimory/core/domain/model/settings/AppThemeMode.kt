package com.soma369.laimory.core.domain.model.settings

/**
 * 앱 화면 모드.
 *
 * 계정이 아니라 **설치 단위** 값이다 — 로그아웃이나 계정 전환으로 지우지 않는다. 이 기기를 쓰는
 * 사람이 정한 것이지, 그 계정이 정한 것이 아니다.
 */
enum class AppThemeMode {
    /** OS 설정을 따라간다. 실행 중 OS 가 바뀌면 함께 바뀐다. */
    SYSTEM,

    LIGHT,

    DARK,
    ;

    companion object {
        /** 저장값이 없거나 읽지 못했을 때. */
        val DEFAULT = SYSTEM

        /** 알 수 없는 값은 기본값으로 떨어뜨린다. 저장 형식이 바뀌어도 앱이 열리지 않는 일은 없다. */
        fun fromName(name: String?): AppThemeMode = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
