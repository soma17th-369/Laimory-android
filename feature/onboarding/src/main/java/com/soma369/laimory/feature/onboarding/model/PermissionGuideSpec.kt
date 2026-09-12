package com.soma369.laimory.feature.onboarding.model

/**
 * 권한 창 안내에 그릴 내용.
 *
 * 시스템 창을 **흉내 낸 모형**이다. 실제 창이 아니라 무엇을 누를지 미리 보여 주는 그림이라,
 * 문구는 시스템 창과 같아야 알아볼 수 있다.
 */
internal data class PermissionGuideSpec(
    /** 창이 뜨기 전에 무엇을 누를지 말하는 한 줄. 그림을 못 보는 사용자는 이 문장만 듣는다. */
    val caption: String,
    /** 창 제목. */
    val title: String,
    /** 창의 선택지. 위에서부터 보이는 순서 그대로다. */
    val options: List<String>,
    /** 눌러야 하는 선택지의 자리. */
    val highlightedIndex: Int,
)
