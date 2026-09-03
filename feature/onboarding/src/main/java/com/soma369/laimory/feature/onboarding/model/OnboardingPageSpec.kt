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
    /**
     * 권한 라벨 대신 놓는 브랜드 라벨.
     *
     * [label] 과 나누는 이유는 규칙이 다르기 때문이다 — 권한 라벨은 허용되면 `PHOTO · 연결됨`
     * 으로 바뀌고 색도 강조로 바뀌는데, 브랜드 라벨에는 연결할 권한이 없어 그 규칙이 성립하지
     * 않는다. 같은 필드에 담으면 마지막 장이 `LAIMORY · 연결됨` 이 된다.
     */
    val brandLabel: String? = null,
    val title: String,
    /**
     * 제목 아래 설명. 없는 장도 있다 — 첫 장은 타임라인 예시가 설명을 대신한다.
     */
    val description: String? = null,
    @DrawableRes val image: Int? = null,
    /**
     * 이미지를 세로로 흘려 보여 줄지.
     *
     * 첫 장의 타임라인 예시처럼 화면보다 긴 그림에 쓴다. 한 번에 다 보여 줄 수 없는 그림을
     * 축소해 넣으면 글자가 뭉개져 무엇을 만드는 화면인지 되레 알 수 없다.
     */
    val scrollsImage: Boolean = false,
    /**
     * 이 장의 CTA 가 요청할 권한. `null` 이면 안내 전용 장이라 CTA 가 다음으로만 넘긴다.
     */
    val permission: DataPermission? = null,
    /**
     * 이 장에 초안 생성 필수 동의 목록을 함께 그릴지.
     *
     * 받을 것이 없으면(이미 동의했거나 catalog 가 아직 없으면) 목록만 비고 장은 그대로 남는다 —
     * 마지막 장은 동의 유무와 무관하게 온보딩을 끝내는 자리라 사라지면 안 된다.
     */
    val showsConsents: Boolean = false,
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
