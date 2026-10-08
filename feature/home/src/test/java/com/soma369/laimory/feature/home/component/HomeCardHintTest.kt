package com.soma369.laimory.feature.home.component

import com.soma369.laimory.feature.home.state.DraftCreationStatus
import com.soma369.laimory.feature.home.state.HomeRecordState
import com.soma369.laimory.feature.home.state.HomeSourceCount
import com.soma369.laimory.feature.home.state.HomeSourceKind
import com.soma369.laimory.feature.home.state.HomeSourceSummary
import com.soma369.laimory.feature.home.state.HomeUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeCardHintTest {
    private val unpicked = HomeUiState(summary = HomeSourceSummary(photo = HomeSourceCount(candidate = 15, sending = 0)))

    @Test
    fun `고를 사진이 있는데 하나도 고르지 않았으면 사진 고르기를 적는다`() {
        assertEquals("사진 고르기", unpicked.cardHint(HomeSourceKind.PHOTO))
    }

    @Test
    fun `한 장이라도 골랐으면 적지 않는다`() {
        assertNull(unpicked.copy(selectedPhotoIds = setOf(1L)).cardHint(HomeSourceKind.PHOTO))
    }

    @Test
    fun `고를 사진이 없으면 적지 않는다`() {
        val empty = unpicked.copy(summary = HomeSourceSummary(photo = HomeSourceCount(candidate = 0)))

        assertNull(empty.cardHint(HomeSourceKind.PHOTO))
    }

    @Test
    fun `바꿀 수 없는 날에는 적지 않는다`() {
        // 생성 중·완성된 날은 눌러도 고를 수 없다.
        assertNull(unpicked.copy(draftStatus = DraftCreationStatus.PROCESSING).cardHint(HomeSourceKind.PHOTO))
        assertNull(unpicked.copy(selectedRecord = HomeRecordState.SAVED).cardHint(HomeSourceKind.PHOTO))
    }

    @Test
    fun `사진 카드에만 적는다`() {
        val calendar = unpicked.copy(summary = HomeSourceSummary(calendar = HomeSourceCount(candidate = 3)))

        assertNull(calendar.cardHint(HomeSourceKind.CALENDAR))
    }
}
