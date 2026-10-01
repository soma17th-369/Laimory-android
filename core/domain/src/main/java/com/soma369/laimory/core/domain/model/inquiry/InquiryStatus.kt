package com.soma369.laimory.core.domain.model.inquiry

/**
 * 문의 처리 상태. 서버가 판정해 내린다 — 앱이 답변 시각의 유무로 따로 해석하지 않는다.
 *
 * 관리자가 처리됨 표시를 풀면 [ANSWERED] 에서 [RECEIVED] 로 돌아갈 수 있다. 한 방향으로만 간다고
 * 가정하지 않는다.
 */
enum class InquiryStatus {
    /** 접수됨 — 확인 중. */
    RECEIVED,

    /** 답변 완료 — 입력한 이메일로 답장을 보냈다. */
    ANSWERED,
    ;

    companion object {
        /** 모르는 값은 확인 중으로 본다. 답하지 않은 문의를 답했다고 말하는 쪽이 더 나쁘다. */
        fun fromName(name: String?): InquiryStatus = entries.firstOrNull { it.name == name } ?: RECEIVED
    }
}
