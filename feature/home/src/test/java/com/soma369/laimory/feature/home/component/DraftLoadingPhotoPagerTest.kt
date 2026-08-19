package com.soma369.laimory.feature.home.component

import org.junit.Assert.assertEquals
import org.junit.Test

class DraftLoadingPhotoPagerTest {
    @Test
    fun `사진이 여러 장이면 페이지를 크게 열어 되감지 않고 순환한다`() {
        // 마지막에서 처음으로 갈 때 전체를 거꾸로 되감지 않으려면 페이지가 끝나면 안 된다.
        assertEquals(Int.MAX_VALUE, draftPhotoPageCount(photoCount = 3))
        assertEquals(Int.MAX_VALUE, draftPhotoPageCount(photoCount = 20))
    }

    @Test
    fun `사진이 한 장이면 넘길 것이 없어 페이지도 하나다`() {
        assertEquals(1, draftPhotoPageCount(photoCount = 1))
        assertEquals(0, draftPhotoInitialPage(photoCount = 1))
    }

    @Test
    fun `가운데에서 시작하되 첫 화면은 첫 사진이다`() {
        listOf(2, 3, 5, 20).forEach { photoCount ->
            val initial = draftPhotoInitialPage(photoCount)

            assertEquals(0, draftPhotoIndexFor(initial, photoCount))
            // 양쪽으로 넘길 여유가 남아 있어야 한다.
            assertEquals(true, initial > photoCount)
            assertEquals(true, initial < Int.MAX_VALUE - photoCount)
        }
    }

    @Test
    fun `페이지를 넘기면 사진이 차례로 돌아온다`() {
        val initial = draftPhotoInitialPage(photoCount = 3)

        assertEquals(listOf(0, 1, 2, 0, 1), (0..4).map { draftPhotoIndexFor(initial + it, 3) })
    }

    @Test
    fun `시작점보다 뒤로 넘기면 마지막 사진으로 이어진다`() {
        val initial = draftPhotoInitialPage(photoCount = 3)

        assertEquals(2, draftPhotoIndexFor(initial - 1, 3))
        assertEquals(1, draftPhotoIndexFor(initial - 2, 3))
    }

    @Test
    fun `페이지가 음수여도 사진 번호는 음수가 되지 않는다`() {
        // Pager 가 주는 페이지는 0 이상이지만, 계산이 나머지 부호에 기대지 않음을 고정한다.
        assertEquals(2, draftPhotoIndexFor(page = -1, photoCount = 3))
        assertEquals(0, draftPhotoIndexFor(page = -3, photoCount = 3))
    }
}
