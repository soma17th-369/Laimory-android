package com.soma369.laimory.core.util.logging

/**
 * 로그 영역 분류 태그. [Logger]의 domain 파라미터(= Logcat tag)로 사용한다.
 */
object LogDomain {
    const val MVI = "Mvi"
    const val NAVIGATION = "Navigation"
    const val NETWORK = "Network"
    const val USE_CASE = "UseCase"
    const val REPOSITORY = "Repository"
    const val COLLECTION = "Collection"
    const val DRAFT_TASK = "DraftTask"
}
