package com.soma369.laimory.feature.onboarding.model

import com.soma369.laimory.core.ui.permission.DataPermission

/**
 * 허용을 마친 장에서 다음 장으로 바로 넘길지.
 *
 * **이 장에서 사용자가 요청을 보냈고, 그 결과 허용으로 바뀐 경우에만** 넘긴다. 시스템 창의 결과든
 * 설정 화면에 다녀와 다시 조회한 결과든 같다.
 *
 * 이미 허용된 장에 **도착**했을 때는 넘기지 않는다. 뒤로 넘겨 지난 장을 다시 보는 사용자를 다음
 * 장으로 밀어내면 되돌아볼 수 없다.
 *
 * 마지막 장에서는 넘길 곳이 없다 — 동의와 시작 버튼이 있는 마무리 장이다.
 */
internal fun advancesAfterGrant(
    permission: DataPermission?,
    isPageDone: Boolean,
    wasRequestedHere: Boolean,
    isLastPage: Boolean,
): Boolean = permission != null && isPageDone && wasRequestedHere && !isLastPage
