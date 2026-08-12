package com.soma369.laimory.core.domain.exception

/**
 * 확정 스냅샷의 사진 원본에 더 이상 접근할 수 없는 상태(삭제·권한 변경·메타 확인 실패).
 *
 * 같은 스냅샷 재시도로는 복구되지 않으므로, 상위 레이어는 준비 상태를 폐기하고
 * 사진 재선택 흐름으로 복귀시켜야 한다.
 */
class DraftPhotoAccessException(
    message: String,
) : ApiException(message)
