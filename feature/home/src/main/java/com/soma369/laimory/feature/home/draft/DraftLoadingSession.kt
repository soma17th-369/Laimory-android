package com.soma369.laimory.feature.home.draft

import java.time.Instant
import java.time.LocalDate

/**
 * 생성 로딩 화면이 보여줄, 실제로 전송된 내용의 스냅샷.
 *
 * 동의 준비 상태([DraftConsentPreparation])는 제출 직후 폐기되므로 로딩 화면이 쓸 수 없다.
 * 그래서 `만들기` 를 누른 시점에 화면이 필요한 것만 따로 복사해 둔다 — 로딩 화면은 서버 응답을 기다리지 않고
 * 곧바로 뜨므로, 아직 작업 번호가 없을 때부터 사진과 건수를 보여 준다.
 *
 * 인메모리라 프로세스가 재시작되면 사라진다. 그때도 작업 추적은 계속되며, 화면은 사진과 건수 없이
 * 표시한다 — 화면 표시를 위해 서버를 다시 부르지 않는다.
 */
data class DraftLoadingSession(
    /** 서버가 발급한 작업 번호. 요청을 보내는 동안은 아직 없다. */
    val taskId: String?,
    val recordDate: LocalDate,
    /** 콜라주에 쓸 사진의 로컬 URI. 전송된 사진 전체이며 화면이 앞에서부터 필요한 만큼 쓴다. */
    val photoUris: List<String>,
    val photoCount: Int,
    val calendarCount: Int,
    val stayCount: Int,
    /**
     * `만들기` 를 누른 시각. 연출 경과의 하한이다 — 요청 중에도 연출이 흐르는데, 작업 번호를 받은 뒤 작업의
     * 요청 시각으로 다시 세면 이미 지나간 단계가 되돌아간다.
     */
    val submittedAt: Instant? = null,
)
