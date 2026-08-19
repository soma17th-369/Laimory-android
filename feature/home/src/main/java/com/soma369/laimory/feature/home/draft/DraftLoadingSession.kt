package com.soma369.laimory.feature.home.draft

import java.time.LocalDate

/**
 * 생성 로딩 화면이 보여줄, 실제로 전송된 내용의 스냅샷.
 *
 * 동의 준비 상태([DraftConsentPreparation])는 제출 직후 폐기되므로 로딩 화면이 쓸 수 없다.
 * 그래서 제출 성공 시점에 화면이 필요한 것만 따로 복사해 둔다.
 *
 * 인메모리라 프로세스가 재시작되면 사라진다. 그때도 작업 추적은 계속되며, 화면은 사진과 건수 없이
 * 표시한다 — 화면 표시를 위해 서버를 다시 부르지 않는다.
 */
data class DraftLoadingSession(
    val taskId: String,
    val recordDate: LocalDate,
    /** 콜라주에 쓸 사진의 로컬 URI. 전송된 사진 전체이며 화면이 앞에서부터 필요한 만큼 쓴다. */
    val photoUris: List<String>,
    val photoCount: Int,
    val calendarCount: Int,
    val stayCount: Int,
)
