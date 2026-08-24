package com.soma369.laimory.core.domain.model.collection

/**
 * 수집 실험실(수집 원문 목록·수동 수집·스테이징 삭제)에 들어가도 되는지.
 *
 * 개발 도구라 릴리즈 사용자에게 노출하지 않는다. 자동 수집 가능 여부와는 무관하다 —
 * 릴리즈에서도 권한이 있는 일정·건강 자동 수집은 그대로 돈다.
 *
 * `DraftConsentSubmissionGate` 처럼 빌드 판정을 앱 계층에 두기 위한 포트다. Domain·Feature 가
 * 앱의 `BuildConfig` 를 직접 참조하지 않게 한다.
 */
fun interface CollectionLabAccessGate {
    fun isCollectionLabAccessible(): Boolean
}
