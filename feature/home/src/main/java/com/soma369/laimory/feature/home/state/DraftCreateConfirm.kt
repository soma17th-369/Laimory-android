package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.credit.TimelineCredit
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelection

/**
 * 타임라인 만들기 확인 다이얼로그 — 무엇을 몇 건 보내는지.
 *
 * [photoUris] 는 보낼 사진을 고른 차례대로 담는다. 비면 다이얼로그가 사진을 고르라는 안내를 띄운다.
 * [counts] 는 사진 아래 칸들이다.
 * [credit] 은 칸들 아래 크레딧 줄이다. 못 받았으면 `null` 이고 줄을 숨긴다 — 만들기는 막지 않는다(판정은 서버).
 *
 * [emptyReason] 이 있으면 만들 수 없다. 다이얼로그는 그대로 뜨되 `만들기` 를 끄고 그 이유와 함께 사진을 고르라고
 * 안내한다. 보낼 것이 하나도 없으면 건강 칸은 붙지 않아 모든 칸이 0 이고, 기록이 너무 적으면 칸은 실제 건수 그대로다.
 */
@Immutable
data class DraftCreateConfirm(
    val photoUris: List<String>,
    val counts: List<DraftCreateConfirmCount>,
    val credit: TimelineCredit? = null,
    val emptyReason: DraftCreateEmptyReason? = null,
) {
    /**
     * 잔액이 비용보다 적다고 **알고 있을 때만** 참. 모르면(조회 실패) 만들기를 그대로 둔다.
     *
     * 만들 수 없는 날은 거짓이다 — `충전하러 가기` 로 보내도 충전한 뒤 여전히 만들 수 없다. 크레딧 줄은 그대로 보인다.
     */
    val isCreditShort: Boolean get() = credit?.isEnough == false && emptyReason == null

    val isEmpty: Boolean get() = emptyReason != null
}

/**
 * 타임라인을 만들 거리가 있는지 — 사진·일정·위치·알림 중 한 건이라도.
 *
 * **건강(걸음)만으로는 만들지 않는다.** 홈 카드에 건강이 없어, 카드가 모두 비었는데 걸음만으로 크레딧을 쓰고
 * 생성까지 가게 된다. 사용자는 무엇이 나가는지도 모른다.
 */
internal fun DraftSourceItemSelection.hasCreatableItems(): Boolean = items.any { it.itemType != ItemType.HEALTH }

/**
 * 제출할 스냅샷으로 확인 다이얼로그를 만든다.
 *
 * **건수는 최종 스냅샷에서 센다.** 카드가 보여 준 값을 재사용하지 않는다 — CTA 직전 자동 수집으로
 * 늘어난 것이 있어, 사용자가 마지막으로 확인하는 숫자가 실제 전송 건수여야 한다.
 *
 * 일정·위치·알림은 0건이어도 칸을 둔다 — 칸 수가 바뀌면 같은 다이얼로그가 날마다 다른 모양이 된다.
 * **건강은 보낼 때만 붙인다.** 홈 카드에는 없지만 전송되므로, 빠지면 "보여준 것 = 보내는 것" 이 깨지고
 * 사용자는 걸음 수가 나간 줄 모른다. 만들 거리가 없으면([hasCreatableItems]) 아무것도 보내지 않으므로 붙이지 않는다 —
 * 안내는 "모인 것이 없다" 는데 `걸음 1건` 이 보이면 어긋난다.
 */
internal fun DraftSourceItemSelection.toCreateConfirm(
    credit: TimelineCredit? = null,
    emptyReason: DraftCreateEmptyReason? = null,
): DraftCreateConfirm {
    val countOf = { group: DraftConsentTypeGroup ->
        group.memberTypes.sumOf { report.selectedCounts.getOrDefault(it, 0) }
    }
    val health = countOf(DraftConsentTypeGroup.HEALTH)
    return DraftCreateConfirm(
        photoUris = items.mapNotNull { (it.payload as? PhotoPayload)?.clientPhotoUri },
        counts =
            FIXED_COUNT_GROUPS.map { DraftCreateConfirmCount(it, countOf(it)) } +
                listOfNotNull(
                    DraftCreateConfirmCount(DraftConsentTypeGroup.HEALTH, health).takeIf { health > 0 && hasCreatableItems() },
                ),
        credit = credit,
        emptyReason = emptyReason,
    )
}

/** 홈 카드와 같은 차례. */
private val FIXED_COUNT_GROUPS =
    listOf(
        DraftConsentTypeGroup.CALENDAR,
        DraftConsentTypeGroup.LOCATION,
        DraftConsentTypeGroup.NOTIFICATION,
    )
