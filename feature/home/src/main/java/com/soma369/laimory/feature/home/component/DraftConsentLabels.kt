package com.soma369.laimory.feature.home.component

import androidx.annotation.StringRes
import com.soma369.laimory.feature.home.R
import com.soma369.laimory.feature.home.state.DraftConsentTerm
import com.soma369.laimory.feature.home.state.DraftConsentTypeGroup

internal fun DraftConsentTypeGroup.label(): String =
    when (this) {
        DraftConsentTypeGroup.PHOTO -> "사진"
        DraftConsentTypeGroup.CALENDAR -> "일정"
        DraftConsentTypeGroup.LOCATION -> "위치"
        DraftConsentTypeGroup.HEALTH -> "건강"
        DraftConsentTypeGroup.NOTIFICATION -> "알림"
    }

/** 유형별로 실제 전송되는 필드 요약. 현재 서버 계약(projection) 기준으로 유지한다. */
internal fun DraftConsentTypeGroup.sentFieldsLabel(): String =
    when (this) {
        DraftConsentTypeGroup.PHOTO -> "선택한 사진 파일 · 촬영 시각 · 촬영 위치(EXIF)"
        DraftConsentTypeGroup.CALENDAR -> "제목 · 시간 · 장소 · 설명"
        DraftConsentTypeGroup.LOCATION -> "체류·이동 시간과 좌표 · 이동수단 · 거리"
        DraftConsentTypeGroup.HEALTH -> "걸음 수·수면 지표와 시간 범위"
        DraftConsentTypeGroup.NOTIFICATION -> "앱 이름 · 알림 제목 · 본문 · 시각"
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
