package com.soma369.laimory.core.domain.exception

/**
 * 동의를 보내는 사이에 약관이 개정돼 보낸 버전이 더 이상 현재 유효 버전이 아니다.
 *
 * **새 버전으로 자동 재시도하면 안 된다.** 사용자가 열람하지 않은 내용에 동의한 기록이 서버에
 * 남는다. 다시 조회해 바뀐 항목을 기본 해제로 되돌리고 새 원문으로 다시 받아야 한다.
 */
class StaleTermVersionException(
    cause: Throwable? = null,
) : Exception(MESSAGE, cause) {
    companion object {
        const val ERROR_CODE = -3002
        private const val MESSAGE = "약관이 개정되어 다시 확인이 필요합니다"
    }
}
