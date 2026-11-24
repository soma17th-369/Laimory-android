package com.soma369.laimory.feature.home.draft

/**
 * 생성 작업이 FAILED 로 끝났을 때의 안내 문구. 로딩 화면 안내와 홈 상태 문구가 같은 말을 쓴다.
 *
 * 작업 상태 조회 FAILED 응답에는 사용자 문구가 없어 실패 사유 코드로 앱이 고른다.
 */
internal object DraftTaskFailureMessages {
    /**
     * 기록에서 타임라인에 넣을 활동을 찾지 못했다(`-1022`). 앞 문장은 서버 실패 push 와 같다.
     *
     * 같은 기록으로 다시 만들어도 결과가 같아 바로 재시도를 권하지 않는다. 지난 날짜는 기록이 더 쌓이지 않으니
     * 사진을 더 고르는 길을 먼저 말한다.
     */
    const val NO_EVENTS = "타임라인을 만들 정보가 부족해요. 사진을 더 고르거나 기록이 더 쌓인 뒤 다시 만들어 주세요."
    const val OTHER = "초안을 만들지 못했어요. 다시 시도해주세요."
}
