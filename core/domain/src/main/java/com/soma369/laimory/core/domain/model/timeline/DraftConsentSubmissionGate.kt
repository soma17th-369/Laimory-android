package com.soma369.laimory.core.domain.model.timeline

/**
 * 동의 화면의 실제 초안 생성 제출 허용 여부.
 *
 * 동의 문구의 법적 지위(제3자 제공·국외 이전 계약 관계)가 확정되기 전에는 임시 문구로
 * 실제 전송이 시작되지 않도록 빌드 단위로 차단하는 배포 가드다.
 * 문구 확정 후에는 상시 허용으로 전환하거나 제거한다.
 */
fun interface DraftConsentSubmissionGate {
    fun isSubmissionAllowed(): Boolean
}
