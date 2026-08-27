package com.soma369.laimory.feature.onboarding.model

import androidx.annotation.DrawableRes
import com.soma369.laimory.core.ui.permission.DataPermission

/**
 * 온보딩 한 장.
 *
 * 화면은 이 목록을 **그리기만** 한다. 문구·이미지를 바꾸거나 장을 더하고 빼는 일은 목록만
 * 고치면 되고 Pager 코드는 건드리지 않는다 — 온보딩은 제품이 계속 손보는 화면이라 그때마다
 * 화면 코드를 여는 구조면 금방 굳는다.
 */
data class OnboardingPageSpec(
    /**
     * 이 장을 가리키는 안정된 이름.
     *
     * 진행 상태를 순번이 아니라 이 값으로 저장한다. 목록이 바뀌어도 사용자가 보던 자리를
     * 잃지 않는다.
     */
    val key: String,
    /** 제목 위 작은 라벨(`PHOTO`). 없으면 그리지 않는다. */
    val label: String? = null,
    val title: String,
    val description: String,
    @DrawableRes val image: Int? = null,
    /**
     * 이 장의 CTA 가 요청할 권한. `null` 이면 안내 전용 장이라 CTA 가 다음으로만 넘긴다.
     */
    val permission: DataPermission? = null,
    val primaryCta: String,
    /** `나중에` 를 함께 둘지. 안내 전용 장에는 건너뛸 것이 없다. */
    val isSkippable: Boolean = permission != null,
    /**
     * 닉네임 인사말을 제목 위에 둘지.
     *
     * 첫 장만 해당한다. 닉네임 조회가 늦거나 실패해도 인사말만 빠지고 나머지는 그대로 보인다.
     */
    val showsGreeting: Boolean = false,
)
