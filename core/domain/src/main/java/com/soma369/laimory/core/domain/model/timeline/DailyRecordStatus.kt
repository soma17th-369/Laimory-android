package com.soma369.laimory.core.domain.model.timeline

/**
 * 하루 기록의 작성 상태.
 *
 * 서버 응답의 nullable `status`를 매핑한 값이다. 응답에 상태가 없거나 지원하지 않는 값이면
 * 도메인은 null로 두며, 화면은 SAVED로 확인된 기록만 읽기 전용으로 다루고 그 외는 작성 중으로 간주한다.
 */
enum class DailyRecordStatus {
    /** 작성 중 — 저장·편집·삭제가 가능하다. */
    DRAFT,

    /** 작성 완료 — 서버가 더 이상 수정·삭제를 허용하지 않는다. */
    SAVED,
}
