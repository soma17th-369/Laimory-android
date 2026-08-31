package com.soma369.laimory.feature.home.component

import androidx.annotation.DrawableRes
import com.soma369.laimory.feature.home.state.DraftConsentTypeGroup
import com.soma369.laimory.core.ui.R as UiR

internal fun DraftConsentTypeGroup.label(): String =
    when (this) {
        DraftConsentTypeGroup.PHOTO -> "사진"
        DraftConsentTypeGroup.CALENDAR -> "일정"
        DraftConsentTypeGroup.LOCATION -> "위치"
        DraftConsentTypeGroup.HEALTH -> "건강"
        DraftConsentTypeGroup.NOTIFICATION -> "알림"
    }

@DrawableRes
internal fun DraftConsentTypeGroup.iconRes(): Int =
    when (this) {
        DraftConsentTypeGroup.PHOTO -> UiR.drawable.ico_collection_photo
        DraftConsentTypeGroup.CALENDAR -> UiR.drawable.ico_collection_calendar
        DraftConsentTypeGroup.LOCATION -> UiR.drawable.ico_collection_location
        DraftConsentTypeGroup.HEALTH -> UiR.drawable.ico_collection_health
        DraftConsentTypeGroup.NOTIFICATION -> UiR.drawable.ico_collection_notification
    }
