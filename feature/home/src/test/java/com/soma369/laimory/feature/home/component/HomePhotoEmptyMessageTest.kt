package com.soma369.laimory.feature.home.component

import com.soma369.laimory.core.ui.permission.DataSourceStatus
import com.soma369.laimory.feature.home.state.HomePhotoItem
import com.soma369.laimory.feature.home.state.HomeSourcePermissions
import com.soma369.laimory.feature.home.state.HomeUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class HomePhotoEmptyMessageTest {
    private val loadedEmpty =
        HomeUiState(
            permissions = HomeSourcePermissions(photo = DataSourceStatus.GRANTED),
            hasLoadedPhotoCandidates = true,
        )

    @Test
    fun `전체 허용이고 불러왔는데 후보가 없으면 갤러리에 사진이 없다고 적는다`() {
        assertEquals("갤러리에 이 기간 사진이 없어요", loadedEmpty.photoEmptyMessage())
    }

    @Test
    fun `불러오기 전이나 불러오는 중에는 적지 않는다`() {
        // 앱을 켜자마자 후보가 아직 비어 있을 뿐인데 사진이 없다고 말하면 틀린 문구가 잠깐 뜬다.
        assertNull(loadedEmpty.copy(hasLoadedPhotoCandidates = false).photoEmptyMessage())
        assertNull(loadedEmpty.copy(isPhotoLoading = true).photoEmptyMessage())
    }

    @Test
    fun `권한이 없거나 일부만 허용이면 적지 않는다`() {
        // 본문이 이미 탭하여 허용을 말하고, 그때 갤러리에 없다고 하면 틀린 말이다.
        assertNull(loadedEmpty.copy(permissions = HomeSourcePermissions(photo = DataSourceStatus.DENIED)).photoEmptyMessage())
        assertNull(loadedEmpty.copy(permissions = HomeSourcePermissions(photo = DataSourceStatus.LIMITED)).photoEmptyMessage())
    }

    @Test
    fun `후보가 있으면 적지 않는다`() {
        val withPhoto = loadedEmpty.copy(availablePhotos = listOf(HomePhotoItem(1L, "content://photo/1", Instant.EPOCH)))

        assertNull(withPhoto.photoEmptyMessage())
    }
}
