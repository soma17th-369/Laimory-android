package com.soma369.laimory.core.domain.message

/**
 * 현재 화면에 표시해야 하는 활성 Dialog.
 *
 * [requestId]는 요청마다 고유하며, Root는 사용자 선택을 이 id와 함께 반환해
 * 오래된 Dialog의 결과가 새 요청에 전달되지 않도록 한다.
 */
data class ActiveDialog(
    val requestId: Long,
    val request: DialogRequest,
)
