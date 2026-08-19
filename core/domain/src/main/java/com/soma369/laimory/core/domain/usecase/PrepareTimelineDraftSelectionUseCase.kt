package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.NotificationContent
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.collection.NotificationPrivacyPolicy
import com.soma369.laimory.core.domain.model.collection.NotificationSignals
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelection
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionPolicy
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionReporter
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 서버 초안 생성에 실제로 전송될 아이템 목록을 네트워크 부작용 없이 확정한다.
 *
 * 기록 창 필터·타입별 상한·정렬을 적용한 [DraftSourceItemSelection]을 반환하며,
 * 동의 화면이 이 결과를 불변 스냅샷으로 삼아 표시·상세·제출에 동일하게 사용한다.
 * 사용자 선택 PHOTO가 상한을 초과하면 자동 절삭하지 않고 실패한다.
 * 선택 측정 리포트는 확정 시점에 한 번만 발행한다(제출 단계에서 재발행하지 않는다).
 *
 * 알림은 상한을 적용하기 전에 개인정보 정책을 다시 통과시킨다 — 정제된 목록 하나를
 * 동의 화면과 서버 전송이 함께 쓰므로, 전송 직전 projection 에는 같은 정책을 두지 않는다.
 */
@Singleton
class PrepareTimelineDraftSelectionUseCase
    @Inject
    constructor(
        private val selectionPolicy: DraftSourceItemSelectionPolicy,
        private val privacyPolicy: NotificationPrivacyPolicy,
        private val selectionReporter: DraftSourceItemSelectionReporter,
    ) {
        operator fun invoke(
            window: RecordDateWindow,
            items: List<SourceItem>,
        ): Result<DraftSourceItemSelection> =
            runCatching {
                selectionPolicy.select(window, items.sanitizeNotifications()).getOrThrow().also { selection ->
                    if (selectionReporter.isEnabled) selectionReporter.reportSelection(selection.report)
                }
            }

        /**
         * 개인정보 정책 도입 전에 저장된 알림을 메모리에서 다시 정제한다.
         *
         * 전체 제외 대상과 정책 실행이 실패한 알림은 해당 한 건만 빼고 나머지로 초안 생성을
         * 계속한다. 상한·우선순위 선택보다 먼저 수행해 동의 화면 표시 건수와 실제 전송 건수를
         * 일치시킨다. 구조 신호는 로컬에 저장하지 않으므로 텍스트 규칙만 다시 적용된다.
         *
         * 신규 수집분은 저장 시점에 이미 정제돼 있어 이 단계에서 값이 바뀌지 않는다.
         */
        private fun List<SourceItem>.sanitizeNotifications(): List<SourceItem> =
            mapNotNull { item ->
                val payload = item.payload as? NotificationPayload ?: return@mapNotNull item
                val sanitized =
                    runCatching {
                        privacyPolicy.sanitize(
                            content = NotificationContent(title = payload.title, text = payload.text),
                            signals = NotificationSignals.UNAVAILABLE,
                        )
                    }.getOrNull() ?: return@mapNotNull null
                item.copy(payload = payload.copy(title = sanitized.title, text = sanitized.text))
            }
    }
