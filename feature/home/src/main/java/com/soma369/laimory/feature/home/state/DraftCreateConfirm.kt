package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelection

/**
 * 타임라인 만들기 확인 다이얼로그 — 무엇을 몇 건 보내는지.
 *
 * [photoUris] 는 보낼 사진을 고른 차례대로 담는다. 비면 다이얼로그가 사진을 고르라는 안내를 띄운다.
 * [counts] 는 사진 아래 칸들이다.
 */
@Immutable
data class DraftCreateConfirm(
    val photoUris: List<String>,
    val counts: List<DraftCreateConfirmCount>,
)

/**
 * 제출할 스냅샷으로 확인 다이얼로그를 만든다.
 *
 * **건수는 최종 스냅샷에서 센다.** 카드가 보여 준 값을 재사용하지 않는다 — CTA 직전 자동 수집으로
 * 늘어난 것이 있어, 사용자가 마지막으로 확인하는 숫자가 실제 전송 건수여야 한다.
 *
 * 일정·위치·알림은 0건이어도 칸을 둔다 — 칸 수가 바뀌면 같은 다이얼로그가 날마다 다른 모양이 된다.
 * **건강은 보낼 때만 붙인다.** 홈 카드에는 없지만 전송되므로, 빠지면 "보여준 것 = 보내는 것" 이 깨지고
 * 사용자는 걸음 수가 나간 줄 모른다.
 */
internal fun DraftSourceItemSelection.toCreateConfirm(): DraftCreateConfirm {
    val countOf = { group: DraftConsentTypeGroup ->
        group.memberTypes.sumOf { report.selectedCounts.getOrDefault(it, 0) }
    }
    val health = countOf(DraftConsentTypeGroup.HEALTH)
    return DraftCreateConfirm(
        photoUris = items.mapNotNull { (it.payload as? PhotoPayload)?.clientPhotoUri },
        counts =
            FIXED_COUNT_GROUPS.map { DraftCreateConfirmCount(it, countOf(it)) } +
                listOfNotNull(DraftCreateConfirmCount(DraftConsentTypeGroup.HEALTH, health).takeIf { health > 0 }),
    )
}

/** 홈 카드와 같은 차례. */
private val FIXED_COUNT_GROUPS =
    listOf(
        DraftConsentTypeGroup.CALENDAR,
        DraftConsentTypeGroup.LOCATION,
        DraftConsentTypeGroup.NOTIFICATION,
    )
