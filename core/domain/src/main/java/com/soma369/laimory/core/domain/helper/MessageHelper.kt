package com.soma369.laimory.core.domain.helper

import com.soma369.laimory.core.domain.message.DialogRequest
import com.soma369.laimory.core.domain.message.DialogResult
import com.soma369.laimory.core.domain.message.UserMessage

/**
 * 도메인이 공통 정책성 메시지를 발행하는 의미 수준 포트.
 *
 * presentation 구현체가 [UserMessage]를 다이얼로그/스낵바/네비게이션 등 실제 UI 효과로 매핑한다.
 *
 * 공통 Dialog는 사용자 응답까지 유지되는 요청이다 — Root 호스트가 렌더링하고 선택 결과를
 * [DialogResult]로 호출자에게 값으로만 반환한다(비즈니스 실행은 호출 ViewModel 책임).
 * Dialog를 표시할 수 없는 환경(비UI 테스트 대역 등)에서는 확인을 받을 수 없으므로
 * 기본 구현이 즉시 [DialogResult.Dismissed]로 응답한다.
 */
interface MessageHelper {
    fun send(message: UserMessage)

    suspend fun showOneButtonDialog(request: DialogRequest.OneButton): DialogResult = DialogResult.Dismissed

    suspend fun showTwoButtonDialog(request: DialogRequest.TwoButton): DialogResult = DialogResult.Dismissed
}
