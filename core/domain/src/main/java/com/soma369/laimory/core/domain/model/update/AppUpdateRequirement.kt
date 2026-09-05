package com.soma369.laimory.core.domain.model.update

import com.soma369.laimory.core.domain.model.IntroInfo

/**
 * 지금 설치된 버전이 서버가 요구하는 선을 넘는지.
 *
 * 판단 근거는 `versionCode`(Int) 다. `versionName` 문자열 비교는 쓰지 않는다 — `1.10.0` 이
 * `1.9.0` 보다 작다고 읽히는 자리가 생긴다.
 */
sealed interface AppUpdateRequirement {
    /** 쓸 수 있다. */
    data object None : AppUpdateRequirement

    /** 하한선 미만. 업데이트 전에는 앱을 쓸 수 없다. */
    data object Forced : AppUpdateRequirement

    /**
     * 권장선 미만. 미루고 쓸 수 있다.
     *
     * @param version 서버가 권장한 버전. 보류를 이 값에 걸어야 다음 권장 버전이 함께 숨지 않는다.
     */
    data class Recommended(
        val version: Int,
    ) : AppUpdateRequirement

    companion object {
        /**
         * 서버 값과 설치 버전으로 판정한다.
         *
         * 강제가 권장을 이긴다. 서버가 `minAppVersion > recommendAppVersion` 처럼 어긋난 값을
         * 줘도 마찬가지다 — 두 선이 겹치면 더 강한 쪽을 따르는 것이 안전하다.
         *
         * 서버가 필드를 비우면 DTO 변환이 `0` 으로 낮추므로 어떤 버전도 이 선을 넘는다. 값이
         * 없을 때 막지 않는 쪽이 맞다.
         */
        fun of(
            info: IntroInfo,
            installedVersion: Int,
        ): AppUpdateRequirement =
            when {
                info.minAppVersion > installedVersion -> Forced
                info.recommendAppVersion > installedVersion -> Recommended(info.recommendAppVersion)
                else -> None
            }
    }
}
