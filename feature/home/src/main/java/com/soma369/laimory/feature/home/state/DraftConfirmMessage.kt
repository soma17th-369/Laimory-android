package com.soma369.laimory.feature.home.state

import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelection

/**
 * 확인 다이얼로그 본문 — 무엇을 몇 건 보내는지.
 *
 * **건수는 최종 스냅샷에서 센다.** 카드가 보여 준 값을 재사용하지 않는다 — CTA 직전 자동 수집으로
 * 늘어난 것이 있어, 사용자가 마지막으로 확인하는 숫자가 실제 전송 건수여야 한다.
 *
 * **건강도 적는다.** 홈 카드에는 없지만 전송되고, 릴리즈에서는 뺄 수단도 없다. 여기서 빠지면
 * "보여준 것 = 보내는 것" 이 깨지고 사용자는 걸음·수면이 나간 줄 모른다.
 *
 * 0건 유형은 적지 않는다 — 없는 것을 나열하면 있는 것이 묻힌다.
 */
internal fun DraftSourceItemSelection.confirmDialogBody(): String {
    val parts =
        DIALOG_GROUP_ORDER.mapNotNull { group ->
            val count = group.memberTypes.sumOf { report.selectedCounts.getOrDefault(it, 0) }
            if (count == 0) null else "${group.dialogLabel()} $count${group.countUnit}"
        }
    if (parts.isEmpty()) return "보낼 데이터가 없어요."
    return "${parts.joinToString(" · ")} — 총 ${items.size}건의 데이터로 만들어요."
}

/** 홈 카드와 같은 차례로 적고, 카드에 없는 건강을 뒤에 붙인다. */
private val DIALOG_GROUP_ORDER =
    listOf(
        DraftConsentTypeGroup.PHOTO,
        DraftConsentTypeGroup.CALENDAR,
        DraftConsentTypeGroup.LOCATION,
        DraftConsentTypeGroup.NOTIFICATION,
        DraftConsentTypeGroup.HEALTH,
    )

private fun DraftConsentTypeGroup.dialogLabel(): String =
    when (this) {
        DraftConsentTypeGroup.PHOTO -> "사진"
        DraftConsentTypeGroup.CALENDAR -> "일정"
        DraftConsentTypeGroup.LOCATION -> "위치"
        DraftConsentTypeGroup.HEALTH -> "건강"
        DraftConsentTypeGroup.NOTIFICATION -> "알림"
    }
