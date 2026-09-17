package com.soma369.laimory.feature.home.component

import com.soma369.laimory.feature.home.state.HomePhotoItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class PhotoGroupingTest {
    private val zone = ZoneId.of("Asia/Seoul")

    private fun photo(
        id: Long,
        at: LocalDateTime,
    ) = HomePhotoItem(mediaStoreId = id, uri = "content://photo/$id", capturedAt = at.atZone(zone).toInstant())

    @Test
    fun `날짜 섹션과 섹션 안 사진을 모두 최신순으로 묶는다`() {
        // 8/26 을 고르고 기록 범위를 익일 새벽까지 걸친 경우다. 입력 순서가 섞여 있어도 결과는 최신순이다.
        val photos =
            listOf(
                photo(1, LocalDateTime.of(2026, 8, 26, 9, 0)),
                photo(2, LocalDateTime.of(2026, 8, 27, 1, 30)),
                photo(3, LocalDateTime.of(2026, 8, 26, 21, 0)),
                photo(4, LocalDateTime.of(2026, 8, 27, 4, 10)),
            )

        val grouped = photos.groupByDateNewestFirst(zone)

        assertEquals(listOf(LocalDate.of(2026, 8, 27), LocalDate.of(2026, 8, 26)), grouped.keys.toList())
        assertEquals(listOf(4L, 2L), grouped.getValue(LocalDate.of(2026, 8, 27)).map(HomePhotoItem::mediaStoreId))
        assertEquals(listOf(3L, 1L), grouped.getValue(LocalDate.of(2026, 8, 26)).map(HomePhotoItem::mediaStoreId))
        // 크게 보기는 이 순서를 그대로 이어 붙인다.
        assertEquals(listOf(4L, 2L, 3L, 1L), grouped.values.flatten().map(HomePhotoItem::mediaStoreId))
    }
}
