package com.soma369.laimory.feature.home.component

import com.soma369.laimory.feature.home.state.HomeUiState
import com.soma369.laimory.feature.home.state.MAX_PHOTO_SELECTION
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoLimitNoticeTest {
    private val full = HomeUiState(pendingPhotoIds = (1L..MAX_PHOTO_SELECTION.toLong()).toSet())

    @Test
    fun `20장을 고른 동안 시트에 상한 안내를 둔다`() {
        assertEquals(
            "최대 20장까지 고를 수 있어요. 다른 사진을 고르려면 먼저 하나를 해제해 주세요.",
            full.photoLimitNotice(),
        )
    }

    @Test
    fun `상한 아래로 내려가면 안내를 거둔다`() {
        assertNull(full.copy(pendingPhotoIds = full.pendingPhotoIds - 1L).photoLimitNotice())
        assertNull(HomeUiState().photoLimitNotice())
    }

    @Test
    fun `고를 수 없는 날에는 상한 안내를 두지 않는다`() {
        // 읽기 전용 시트는 고르는 조작이 없어 상한을 말하면 틀린 안내다.
        assertNull(full.copy(isSubmitting = true).photoLimitNotice())
    }

    @Test
    fun `크게 보기는 체크가 먹지 않는 고르지 않은 사진에만 안내를 붙인다`() {
        // 크게 보기는 시트 위의 별도 창이라 시트 안내가 가려진다.
        assertEquals(full.photoLimitNotice(), full.photoLimitNoticeFor(mediaStoreId = 99L))
        // 이미 고른 사진은 해제가 되므로 안내하지 않는다.
        assertNull(full.photoLimitNoticeFor(mediaStoreId = 1L))
    }
}
