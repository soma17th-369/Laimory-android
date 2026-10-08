package com.soma369.laimory.feature.home.component

import com.soma369.laimory.core.ui.permission.DataSourceStatus
import com.soma369.laimory.feature.home.state.HomeSourceCount
import com.soma369.laimory.feature.home.state.HomeSourceKind
import com.soma369.laimory.feature.home.state.HomeSourcePermissions
import com.soma369.laimory.feature.home.state.HomeSourceSummary
import com.soma369.laimory.feature.home.state.HomeUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeCardBodyTest {
    private val granted =
        HomeUiState(
            permissions =
                HomeSourcePermissions(
                    photo = DataSourceStatus.GRANTED,
                    calendar = DataSourceStatus.GRANTED,
                    location = DataSourceStatus.GRANTED,
                    notification = DataSourceStatus.GRANTED,
                ),
        )

    @Test
    fun `모인 것이 있으면 보낼 수와 후보 수를 싣고 낭독은 단위를 붙인다`() {
        val state = granted.copy(summary = HomeSourceSummary(photo = HomeSourceCount(candidate = 15, sending = 5)))

        assertEquals(
            HomeCardBody.Count(sending = 5, candidate = 15, spoken = "15장 중 5장"),
            state.cardBody(HomeSourceKind.PHOTO),
        )
    }

    @Test
    fun `낭독 단위는 유형마다 다르다`() {
        val state =
            granted.copy(
                summary =
                    HomeSourceSummary(
                        calendar = HomeSourceCount(candidate = 2, sending = 2),
                        location = HomeSourceCount(candidate = 8, sending = 8),
                        notification = HomeSourceCount(candidate = 17, sending = 3),
                    ),
            )

        assertEquals("2개 중 2개", (state.cardBody(HomeSourceKind.CALENDAR) as HomeCardBody.Count).spoken)
        assertEquals("8건 중 8건", (state.cardBody(HomeSourceKind.LOCATION) as HomeCardBody.Count).spoken)
        assertEquals("17건 중 3건", (state.cardBody(HomeSourceKind.NOTIFICATION) as HomeCardBody.Count).spoken)
    }

    @Test
    fun `모인 것이 없으면 상태 문구를 적는다`() {
        assertEquals(HomeCardBody.Message("아직 모인 것이 없어요"), granted.cardBody(HomeSourceKind.CALENDAR))
        assertEquals(
            HomeCardBody.Message("탭하여 허용"),
            granted.copy(permissions = HomeSourcePermissions(calendar = DataSourceStatus.DENIED)).cardBody(HomeSourceKind.CALENDAR),
        )
        assertEquals(
            HomeCardBody.Message("이 기기에서는 지원하지 않아요"),
            granted
                .copy(permissions = HomeSourcePermissions(notification = DataSourceStatus.UNSUPPORTED))
                .cardBody(HomeSourceKind.NOTIFICATION),
        )
    }
}
