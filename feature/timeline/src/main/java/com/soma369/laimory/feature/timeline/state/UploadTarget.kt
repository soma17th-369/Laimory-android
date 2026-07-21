package com.soma369.laimory.feature.timeline.state

/**
 * 타임라인 요약에서 실행하는 업로드 경로. 실제 동작은 각 Task 에서 연결한다.
 *
 * - [DRIVE_TEST]: 사람 확인용 Google Drive 테스트 내보내기(#121).
 */
enum class UploadTarget(val label: String) {
    DRIVE_TEST("Drive 테스트"),
}
