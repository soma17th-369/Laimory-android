package com.soma369.laimory.feature.home.state

import androidx.annotation.DrawableRes
import com.soma369.laimory.core.ui.R as UiR

/**
 * 홈 원천 카드 4종.
 *
 * 아이콘은 설정 화면이 쓰는 것과 **같은 글리프**라 그대로 재사용한다(Phosphor 24dp). 원천을
 * 가리키는 그림이 화면마다 다르면 같은 것을 두고 다른 것처럼 읽힌다.
 */
enum class HomeSourceKind(
    val label: String,
    @param:DrawableRes val iconRes: Int,
) {
    PHOTO("사진", UiR.drawable.ico_setting_datasource_photo),
    CALENDAR("일정", UiR.drawable.ico_setting_datasource_calendar),
    LOCATION("위치", UiR.drawable.ico_setting_datasource_location),
    NOTIFICATION("알림", UiR.drawable.ico_setting_datasource_notification),
}
