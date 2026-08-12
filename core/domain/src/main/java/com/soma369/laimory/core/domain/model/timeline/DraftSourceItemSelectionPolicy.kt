package com.soma369.laimory.core.domain.model.timeline

import com.soma369.laimory.core.domain.model.collection.ItemType
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
                ),
        )
    }
}

/**
 * 기록 창 필터 → 타입별 상한 → 최종 시간순 정렬을 수행하는 순수 정책.
 *
 * 타입별 선택은 `(startAt 내림차순, rawId 오름차순)`으로 최신 항목을 우선한다.
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
                inWindow
                    .asSequence()
                    .filter { it.itemType == itemType }
                    .sortedWith(NEWEST_FIRST)
                    .take(limits.limitFor(itemType))
                    .toList()
            }
        val ordered = typeLimited.sortedWith(OLDEST_FIRST)
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
