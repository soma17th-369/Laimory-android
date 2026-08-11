package com.soma369.laimory.feature.home.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.soma369.laimory.feature.home.R
import com.soma369.laimory.feature.home.state.DraftConsentTerm
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

/** 유형 행 아이콘. 기존 아이콘을 우선 재사용하고, 없던 map-pin·bell 은 디자인 에셋으로 추가했다. */
@DrawableRes
internal fun DraftConsentTypeGroup.iconRes(): Int =
    when (this) {
        DraftConsentTypeGroup.PHOTO -> UiR.drawable.ico_timeline_photo
        DraftConsentTypeGroup.CALENDAR -> UiR.drawable.ico_timeline_event_calendar_event
        DraftConsentTypeGroup.LOCATION -> UiR.drawable.ico_default_map_pin
        DraftConsentTypeGroup.HEALTH -> UiR.drawable.ico_timeline_event_exercise
        DraftConsentTypeGroup.NOTIFICATION -> UiR.drawable.ico_default_bell
    }

/**
 * 동의 행 부제. 디자인의 추상 표현("데이터 시각화 기술 파트너사") 대신 실제 처리 사실로 기재한다.
 * 법무 검토 전 임시 문구 — 상세 본문과 함께 확정 시 교체한다.
 */
internal fun DraftConsentTerm.subtitle(): String =
    when (this) {
        DraftConsentTerm.SENSITIVE_INFO -> "걸음 수·수면 등 건강정보 처리"
        DraftConsentTerm.THIRD_PARTY_PROVISION -> "AI 초안 생성을 위한 OpenAI 전송"
        DraftConsentTerm.OVERSEAS_TRANSFER -> "미국 소재 서버로 데이터 이전"
    }

@StringRes
internal fun DraftConsentTerm.titleRes(): Int =
    when (this) {
        DraftConsentTerm.SENSITIVE_INFO -> R.string.home_draft_consent_term_sensitive_title
        DraftConsentTerm.THIRD_PARTY_PROVISION -> R.string.home_draft_consent_term_third_party_title
        DraftConsentTerm.OVERSEAS_TRANSFER -> R.string.home_draft_consent_term_overseas_title
    }

@StringRes
internal fun DraftConsentTerm.bodyRes(): Int =
    when (this) {
        DraftConsentTerm.SENSITIVE_INFO -> R.string.home_draft_consent_term_sensitive_body
        DraftConsentTerm.THIRD_PARTY_PROVISION -> R.string.home_draft_consent_term_third_party_body
        DraftConsentTerm.OVERSEAS_TRANSFER -> R.string.home_draft_consent_term_overseas_body
    }
