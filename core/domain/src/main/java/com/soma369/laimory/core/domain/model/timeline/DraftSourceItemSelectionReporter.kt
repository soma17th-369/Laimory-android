package com.soma369.laimory.core.domain.model.timeline

/**
 * 초안 원천 데이터 제한 효과를 payload 원문 없이 관찰하는 포트.
 *
 * debug 구현은 선택 전후 타입별 건수와 최종 직렬화 요청 byte만 기록하고,
 * release 및 단위 테스트에서는 [NONE]으로 비용 없이 비활성화할 수 있다.
 */
interface DraftSourceItemSelectionReporter {
    val isEnabled: Boolean

    fun reportSelection(report: DraftSourceItemSelectionReport)

    fun reportRequestSize(
        sourceItemCount: Int,
        utf8ByteCount: Int,
    )

    companion object {
        val NONE: DraftSourceItemSelectionReporter =
            object : DraftSourceItemSelectionReporter {
                override val isEnabled: Boolean = false

                override fun reportSelection(report: DraftSourceItemSelectionReport) = Unit

                override fun reportRequestSize(
                    sourceItemCount: Int,
                    utf8ByteCount: Int,
                ) = Unit
            }
    }
}
