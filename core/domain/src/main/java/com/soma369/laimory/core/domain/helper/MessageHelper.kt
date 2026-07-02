package com.soma369.laimory.core.domain.helper

import com.soma369.laimory.core.domain.message.UserMessage

/**
 * 도메인이 공통 정책성 메시지를 발행하는 의미 수준 포트.
 *
 * presentation 구현체가 [UserMessage]를 다이얼로그/스낵바/네비게이션 등 실제 UI 효과로 매핑한다.
 */
interface MessageHelper {
    fun send(message: UserMessage)
}
