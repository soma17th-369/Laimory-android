package com.soma369.laimory.core.domain.model.timeline

import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem

/**
 * 초안 요청에 포함할 원천 데이터의 타입별 상한.
 *
 * v1 기본값은 2026-07-10 debug 요청(11,723 bytes, 약 40건, PHOTO 6건)을 초기 기준으로,
 * 최대 약 48시간의 기록 창에도 여유가 있도록 정한 제품값이다. 대표 분포를 실측한 통계값은 아니며,
 * [DraftSourceItemSelectionReporter]의 건수·byte 측정 결과로 조정한다.
 *
 * v1 권장 Maximum: STAY 30, MOVEMENT 30, CALENDAR 20, HEALTH 30, NOTIFICATION 100,
 * PHOTO 20.
 *
 * 각 타입은 자신의 상한까지 독립적으로 전송되며 다른 타입의 선택에 영향을 주지 않는다.
 * 전체 전송 상한은 두지 않으므로(#200 2026-07-30 개정) 이론상 최대 전송량은 타입별 상한의 합이다.
 */
data class DraftSourceItemLimits(
    val stay: Int = DEFAULT_STAY,
    val movement: Int = DEFAULT_MOVEMENT,
    val calendar: Int = DEFAULT_CALENDAR,
    val health: Int = DEFAULT_HEALTH,
    val notification: Int = DEFAULT_NOTIFICATION,
    val photo: Int = DEFAULT_PHOTO,
) {
    init {
        require(ItemType.entries.all { limitFor(it) > 0 }) { "타입별 상한은 모두 0보다 커야 합니다." }
    }

    fun limitFor(itemType: ItemType): Int =
        when (itemType) {
            ItemType.STAY -> stay
            ItemType.MOVEMENT -> movement
            ItemType.CALENDAR -> calendar
            ItemType.HEALTH -> health
            ItemType.NOTIFICATION -> notification
            ItemType.PHOTO -> photo
        }

    companion object {
        const val DEFAULT_STAY = 30
        const val DEFAULT_MOVEMENT = 30
        const val DEFAULT_CALENDAR = 20
        const val DEFAULT_HEALTH = 30
        const val DEFAULT_NOTIFICATION = 100
        const val DEFAULT_PHOTO = 20
    }
}

/** 사용자 선택 PHOTO가 조립 경계의 상한을 초과해 자동 절삭 대신 생성을 중단한 경우. */
class DraftPhotoLimitExceededException(
    val selectedCount: Int,
    val maxCount: Int,
) : Exception("사진은 최대 ${maxCount}장까지 선택할 수 있습니다. (현재 ${selectedCount}장)")

/**
 * payload 원문 없이 선택 전후 건수만 전달하는 측정 모델.
 *
 * NOTIFICATION 은 사유별 건수를 따로 남긴다. 타입 건수만으로는 `NOTIFICATION 300 → 100` 으로만
 * 보여서, 클릭 알림이 통째로 잘려도 드러나지 않는다.
 */
data class DraftSourceItemSelectionReport(
    val originalCounts: Map<ItemType, Int>,
    val selectedCounts: Map<ItemType, Int>,
    val notificationOriginalCountsByReason: Map<NotificationPayload.CollectReason, Int>,
    val notificationSelectedCountsByReason: Map<NotificationPayload.CollectReason, Int>,
) {
    val originalTotal: Int = originalCounts.values.sum()
    val selectedTotal: Int = selectedCounts.values.sum()
    val excludedCounts: Map<ItemType, Int> =
        ItemType.entries.associateWith { itemType ->
            originalCounts.getOrDefault(itemType, 0) - selectedCounts.getOrDefault(itemType, 0)
        }
}

data class DraftSourceItemSelection(
    val items: List<SourceItem>,
    val report: DraftSourceItemSelectionReport,
) {
    /**
     * 사용자가 동의 화면에서 제외한 항목을 뺀 제출용 선택을 만든다.
     *
     * 제외로 생긴 타입별 상한 여유는 다시 채우지 않는다 — 사용자가 화면에서 확인한
     * 항목만 전송된다는 계약을 유지한다. PHOTO 는 홈 사진 선택이 유일한 정본이므로
     * 사진 rawId 가 전달돼도 무시한다(호출부 방어와 이중 방어).
     * 원본 건수([DraftSourceItemSelectionReport.originalCounts])는 수집 기준 그대로 두고
     * selectedCounts 만 남은 항목 기준으로 갱신한다.
     */
    fun excluding(excludedRawIds: Set<String>): DraftSourceItemSelection {
        if (excludedRawIds.isEmpty()) return this
        val remaining = items.filterNot { it.itemType != ItemType.PHOTO && it.rawId in excludedRawIds }
        return DraftSourceItemSelection(
            items = remaining,
            report =
                DraftSourceItemSelectionReport(
                    originalCounts = report.originalCounts,
                    selectedCounts = remaining.countsByType(),
                    notificationOriginalCountsByReason = report.notificationOriginalCountsByReason,
                    notificationSelectedCountsByReason = remaining.notificationCountsByReason(),
                ),
        )
    }
}

/**
 * 기록 창 필터 → 타입별 상한 → 최종 시간순 정렬을 수행하는 순수 정책.
 *
 * 타입별 선택은 `(startAt 내림차순, rawId 오름차순)`으로 최신 항목을 우선한다.
 * NOTIFICATION 만 예외로 상한을 넘을 때 수집 사유 우선순위를 함께 본다([selectNotifications]).
 * 사용자 선택 PHOTO는 자동 절삭하지 않으며, 상한 초과 시 명시적으로 실패한다.
 * 최종 전송 목록은 `(startAt 오름차순, rawId 오름차순)`으로 안정 정렬한다.
 */
class DraftSourceItemSelectionPolicy(
    val limits: DraftSourceItemLimits = DraftSourceItemLimits(),
) {
    fun select(
        window: RecordDateWindow,
        items: List<SourceItem>,
    ): Result<DraftSourceItemSelection> {
        val inWindow = items.filter(window::contains)
        val originalCounts = inWindow.countsByType()
        val photoItems = inWindow.filter { it.itemType == ItemType.PHOTO }
        if (photoItems.size > limits.photo) {
            return Result.failure(
                DraftPhotoLimitExceededException(
                    selectedCount = photoItems.size,
                    maxCount = limits.photo,
                ),
            )
        }

        val typeLimited =
            ItemType.entries.flatMap { itemType ->
                val candidates = inWindow.filter { it.itemType == itemType }
                val limit = limits.limitFor(itemType)
                if (itemType == ItemType.NOTIFICATION) {
                    selectNotifications(candidates, limit)
                } else {
                    candidates.sortedWith(NEWEST_FIRST).take(limit)
                }
            }
        val ordered = typeLimited.sortedWith(OLDEST_FIRST)
        return Result.success(
            DraftSourceItemSelection(
                items = ordered,
                report =
                    DraftSourceItemSelectionReport(
                        originalCounts = originalCounts,
                        selectedCounts = ordered.countsByType(),
                        notificationOriginalCountsByReason = inWindow.notificationCountsByReason(),
                        notificationSelectedCountsByReason = ordered.notificationCountsByReason(),
                    ),
            ),
        )
    }

    /**
     * 알림을 상한만큼 고른다.
     *
     * 상한 이하이면 사유를 보지 않고 기존과 같은 결과를 낸다 — 절삭이 없으면 우선순위는 의미가 없다.
     *
     * 초과하면 먼저 클릭 알림에 [CLICK_MIN_QUOTA] 만큼 자리를 떼어 준다. 사용자가 직접 누른 알림은
     * 사유 우선순위만으로는 키워드 물량에 밀려 통째로 사라질 수 있기 때문이다. 클릭이 쿼터보다 적으면
     * 남는 자리는 반환하고, 쿼터에 들지 못한 클릭 알림도 뒤 경쟁에 그대로 남는다.
     *
     * 남은 자리는 `(사유 우선순위, startAt 내림차순, rawId 오름차순)`으로 채운다.
     */
    private fun selectNotifications(
        candidates: List<SourceItem>,
        limit: Int,
    ): List<SourceItem> {
        val newestFirst = candidates.sortedWith(NEWEST_FIRST)
        if (newestFirst.size <= limit) return newestFirst

        val guaranteedClicks =
            newestFirst
                .asSequence()
                .filter { it.collectReason == NotificationPayload.CollectReason.CLICK }
                .take(minOf(CLICK_MIN_QUOTA, limit))
                .toList()
        val guaranteedIds = guaranteedClicks.mapTo(mutableSetOf(), SourceItem::rawId)
        val filled =
            newestFirst
                .asSequence()
                .filterNot { it.rawId in guaranteedIds }
                .sortedWith(REASON_PRIORITY_FIRST)
                .take(limit - guaranteedClicks.size)
                .toList()
        return guaranteedClicks + filled
    }

    companion object {
        /**
         * NOTIFICATION 이 상한을 넘을 때 클릭 알림에 보장하는 자리.
         *
         * 사유 우선순위에서 `CLICK` 은 `KEYWORD` 뒤라, 쿼터가 없으면 키워드 물량이 많은 날 클릭 알림이
         * 한 건도 남지 않는다. 사용자가 직접 누른 기록은 최소한 남긴다.
         */
        const val CLICK_MIN_QUOTA = 20

        private val NEWEST_FIRST =
            compareByDescending<SourceItem>(SourceItem::startAt)
                .thenBy(SourceItem::rawId)
        private val OLDEST_FIRST =
            compareBy<SourceItem>(SourceItem::startAt)
                .thenBy(SourceItem::rawId)
        private val REASON_PRIORITY_FIRST =
            compareBy<SourceItem> { it.collectReason.priority() }.then(NEWEST_FIRST)
    }
}

private fun List<SourceItem>.countsByType(): Map<ItemType, Int> =
    ItemType.entries.associateWith { itemType -> count { it.itemType == itemType } }

private fun List<SourceItem>.notificationCountsByReason(): Map<NotificationPayload.CollectReason, Int> {
    val reasons = mapNotNull { it.collectReason }
    return NotificationPayload.CollectReason.entries.associateWith { reason -> reasons.count { it == reason } }
}

/** 알림이 아니거나 payload 를 읽을 수 없으면 null. 사유 정렬에서 최하위로 둔다. */
private val SourceItem.collectReason: NotificationPayload.CollectReason?
    get() = (payload as? NotificationPayload)?.collectReason

/**
 * 수집 사유 우선순위. 작을수록 먼저 남는다.
 *
 * `KEYWORD` 가 가장 앞이다 — 정해진 키워드에 걸린 생활 이벤트가 이번 정책의 정본이다.
 * `APP` 은 앱 선택 기능 확장이 보류돼 앞세우지 않고, `ALL` 은 과거 "모든 알림 수집" legacy 데이터다.
 */
private fun NotificationPayload.CollectReason?.priority(): Int =
    when (this) {
        NotificationPayload.CollectReason.KEYWORD -> 0
        NotificationPayload.CollectReason.CLICK -> 1
        NotificationPayload.CollectReason.APP -> 2
        NotificationPayload.CollectReason.ALL -> 3
        null -> 4
    }
