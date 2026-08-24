package com.soma369.laimory.core.domain.model.collection

/**
 * 자동 수집 한 유형의 결과.
 *
 * 수집기 계약은 권한 없음도 실제 조회 실패도 모두 빈 목록으로 흡수한다([Collector] KDoc).
 * 그래서 "0건 수집" 과 "권한이 없어 못 했다" 와 "긁다가 실패했다" 가 저장소에서는 구분되지
 * 않는다. 자동 수집은 사용자가 시작하지 않은 배경 작업이라 무엇이 왜 비었는지 로그와 테스트로
 * 확인할 수 있어야 해서, 호출 경계에서 이 셋을 나눈다.
 */
sealed interface AutoCollectionOutcome {
    /** 수집해 저장까지 마쳤다. [savedCount] 는 새로 저장된 건수이며 0일 수 있다(이미 다 있음). */
    data class Collected(
        val savedCount: Int,
    ) : AutoCollectionOutcome

    /** 읽기 권한이 없어 건너뛰었다. 자동 수집은 권한 요청 화면을 띄우지 않는다. */
    data object PermissionDenied : AutoCollectionOutcome

    /** 플랫폼이 이 유형을 지원하지 않는다(Health Connect 미설치·업데이트 필요 등). */
    data object Unavailable : AutoCollectionOutcome

    /** 권한과 지원 여부는 문제없는데 조회·저장이 실패했다. 이 유형만 5분 캐시를 갱신하지 않는다. */
    data object Failed : AutoCollectionOutcome

    /** 다시 시도할 값어치가 있는 결과인지. 권한 없음·미지원은 재시도해도 같은 답이라 성공처럼 캐시한다. */
    val isRetryable: Boolean get() = this is Failed
}
