package com.soma369.laimory.core.domain.model.timeline

import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.SourceItem

/**
 * 초안 요청에 포함할 원천 데이터의 타입별·전체 상한.
 *
 * v1 기본값은 2026-07-10 debug 요청(11,723 bytes, 약 40건, PHOTO 6건)을 초기 기준으로,
 * 최대 약 48시간의 기록 창에도 여유가 있도록 정한 제품값이다. 대표 분포를 실측한 통계값은 아니며,
 * [DraftSourceItemSelectionReporter]의 건수·byte 측정 결과로 조정한다.
 *
 * v1 권장 Maximum: STAY 30, MOVEMENT 30, CALENDAR 20, HEALTH 30, NOTIFICATION 100,
 * PHOTO 20. 전체 전송 상한은 100.
 *
 * 타입별 값은 보장 건수가 아니라 각각의 최대치다. PHOTO는 사용자 선택을 보존하기 위해 전체 상한에서
 * 먼저 예약하므로, PHOTO 20장이 선택되면 NOTIFICATION은 타입 상한이 100이어도 최대 80건까지만
 * 전송될 수 있다.
 */
data class DraftSourceItemLimits(
    val stay: Int = DEFAULT_STAY,
    val movement: Int = DEFAULT_MOVEMENT,
    val calendar: Int = DEFAULT_CALENDAR,
    val health: Int = DEFAULT_HEALTH,
    val notification: Int = DEFAULT_NOTIFICATION,
    val photo: Int = DEFAULT_PHOTO,
    val total: Int = DEFAULT_TOTAL,
) {
    init {
        require(ItemType.entries.all { limitFor(it) > 0 }) { "타입별 상한은 모두 0보다 커야 합니다." }
        require(total >= photo) { "전체 상한은 PHOTO 상한보다 작을 수 없습니다." }
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
        const val DEFAULT_TOTAL = 100
    }
}

/** 사용자 선택 PHOTO가 조립 경계의 상한을 초과해 자동 절삭 대신 생성을 중단한 경우. */
class DraftPhotoLimitExceededException(
    val selectedCount: Int,
    val maxCount: Int,
) : Exception("사진은 최대 ${maxCount}장까지 선택할 수 있습니다. (현재 ${selectedCount}장)")

/** payload 원문 없이 선택 전후 건수만 전달하는 측정 모델. */
data class DraftSourceItemSelectionReport(
    val originalCounts: Map<ItemType, Int>,
    val selectedCounts: Map<ItemType, Int>,
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
)

/**
 * 기록 창 필터 → 타입별 상한 → 전체 상한 → 최종 시간순 정렬을 수행하는 순수 정책.
 *
 * 타입별·전체 선택은 `(startAt 내림차순, rawId 오름차순)`으로 최신 항목을 우선한다.
 * 사용자 선택 PHOTO는 먼저 예약하고 자동 절삭하지 않으며, 상한 초과 시 명시적으로 실패한다.
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
                inWindow
                    .asSequence()
                    .filter { it.itemType == itemType }
                    .sortedWith(NEWEST_FIRST)
                    .take(limits.limitFor(itemType))
                    .toList()
            }
        val reservedPhotos = typeLimited.filter { it.itemType == ItemType.PHOTO }
        val nonPhotoCapacity = limits.total - reservedPhotos.size
        val selected =
            reservedPhotos +
                typeLimited
                    .asSequence()
                    .filter { it.itemType != ItemType.PHOTO }
                    .sortedWith(NEWEST_FIRST)
                    .take(nonPhotoCapacity)
                    .toList()
        val ordered = selected.sortedWith(OLDEST_FIRST)
        return Result.success(
            DraftSourceItemSelection(
                items = ordered,
                report =
                    DraftSourceItemSelectionReport(
                        originalCounts = originalCounts,
                        selectedCounts = ordered.countsByType(),
                    ),
            ),
        )
    }

    private companion object {
        val NEWEST_FIRST =
            compareByDescending<SourceItem>(SourceItem::startAt)
                .thenBy(SourceItem::rawId)
        val OLDEST_FIRST =
            compareBy<SourceItem>(SourceItem::startAt)
                .thenBy(SourceItem::rawId)
    }
}

private fun List<SourceItem>.countsByType(): Map<ItemType, Int> =
    ItemType.entries.associateWith { itemType -> count { it.itemType == itemType } }
