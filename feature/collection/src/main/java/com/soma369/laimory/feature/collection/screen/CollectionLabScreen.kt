package com.soma369.laimory.feature.collection.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.soma369.laimory.feature.collection.screen.sleep.SleepInputTab

/**
 * 수집 디버그 진입 화면. 소스별 탭(실험실/Lab) 컨테이너다.
 *
 * 각 수집 소스(#91~#95)의 이질적인 디버그 UX 를 한 화면에 세로로 쌓지 않고 탭으로 분리한다.
 * 현재는 "사진" 탭만 실제 동작하고, 예정 소스는 준비 중 placeholder 로 자리만 잡아 둔다.
 * 화면 인셋(innerPadding)은 이 컨테이너가 소유하고, 각 탭은 남은 슬롯을 채운다.
 *
 * 새 소스 추가는 [CollectionLabTab] 에 항목을 넣고 아래 when 분기에 탭 화면을 연결하면 된다.
 */
@Composable
fun CollectionLabRoute(innerPadding: PaddingValues) {
    var selectedTab by rememberSaveable { mutableStateOf(CollectionLabTab.PHOTO) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
    ) {
        OnboardingResetAction()

        TabRow(selectedTabIndex = selectedTab.ordinal) {
            CollectionLabTab.entries.forEach { tab ->
                Tab(
                    selected = tab == selectedTab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.label) },
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                CollectionLabTab.PHOTO -> PhotoCollectionTab()
                CollectionLabTab.CALENDAR -> CalendarCollectionTab()
                CollectionLabTab.HEALTH -> HealthCollectionTab()
                CollectionLabTab.SLEEP -> SleepInputTab()
                CollectionLabTab.LOCATION -> LocationCollectionTab()
                CollectionLabTab.NOTIFICATION -> NotificationCollectionTab()
            }
        }
    }
}

/**
 * 수집 실험실 탭 목록. 수집 기능(이슈) 단위이며 선언 순서 = 탭 노출 순서.
 *
 * [LOCATION] 은 상위 개념 "위치" 로, #94 의 체류(`ItemType.STAY`)·이동(`ItemType.MOVEMENT`) 수집을
 * 한 탭에서 함께 다룬다(탭 라벨 "위치" ≠ 체류만 뜻하는 itemType `STAY`).
 * 이후 수집 소스는 여기에 항목만 추가한다.
 */
enum class CollectionLabTab(val label: String) {
    PHOTO("사진"),
    CALENDAR("일정"),
    HEALTH("헬스"),
    SLEEP("수면"),
    LOCATION("위치"),
    NOTIFICATION("알림"),
}
