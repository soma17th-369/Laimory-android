package com.soma369.laimory.feature.home.state

import com.soma369.laimory.core.ui.base.UiIntent
import com.soma369.laimory.core.ui.permission.DataSourceStatus
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

sealed interface HomeUiIntent : UiIntent {
    /** 화면 진입·복귀. 아직 못 받은 닉네임을 다시 요청한다. */
    data object RefreshProfile : HomeUiIntent

    data object NavigateToCollection : HomeUiIntent

    /**
     * 초안 만들기의 시작. 사진 선택 시트를 연다.
     *
     * 사진은 고른 것만 초안에 실리므로 만들기 흐름의 첫 단계다 — 홈에 따로 떨어져 있으면
     * 옆길처럼 보여 사진 없는 초안이 만들어진다.
     */
    data object OpenPhotoSheet : HomeUiIntent

    data object RequestAdditionalPhotoAccess : HomeUiIntent

    data class ResolvePhotoAccess(
        val granted: Boolean,
        val limited: Boolean = false,
    ) : HomeUiIntent

    data class RefreshPhotos(
        val hasAccess: Boolean,
        val limited: Boolean = false,
    ) : HomeUiIntent

    data object DismissPhotoSheet : HomeUiIntent

    data class TogglePhoto(
        val mediaStoreId: Long,
    ) : HomeUiIntent

    data class TogglePhotoDate(
        val date: LocalDate,
    ) : HomeUiIntent

    data object ToggleAllPhotos : HomeUiIntent

    /** 고른 사진으로 확정하고 데이터 확인 화면으로 넘어간다. */
    data object ConfirmPhotoSelection : HomeUiIntent

    /**
     * 사진 없이 이어서 만든다.
     *
     * 0장 선택 후 확인과 나누는 이유는 의사가 다르기 때문이다 — 아무것도 고르지 않은 확인이
     * 실수인지 의도인지 구분되지 않는다.
     */
    data object ContinueWithoutPhotos : HomeUiIntent

    data object ShowDatePicker : HomeUiIntent

    data object DismissDatePicker : HomeUiIntent

    /**
     * 날짜 피커가 보여 주는 달의 기록 상태를 받아 온다.
     *
     * 피커를 열 때와 달을 넘길 때마다 그 달을 요청한다 — 어느 날짜가 이미 저장됐는지 알아야
     * 고를 수 없게 만들 수 있다. 월별 경량 조회라 달마다 불러도 부담이 적다.
     */
    data class LoadMonthlyRecords(
        val month: YearMonth,
    ) : HomeUiIntent

    data class SelectDate(
        val date: LocalDate,
    ) : HomeUiIntent

    /** 시각 선택 시트를 열고 누른 줄을 펼친다. */
    data class ShowTimePicker(
        val field: HomeTimeField,
    ) : HomeUiIntent

    /** 시트 안에서 펼친 줄을 바꾼다. null 이면 모두 접는다. */
    data class ExpandTimeField(
        val field: HomeTimeField?,
    ) : HomeUiIntent

    /**
     * 시트 롤러를 굴린 결과. 확인 전까지는 시트 임시 값만 바뀐다.
     *
     * 종료 줄의 [date]는 날짜 롤러가 가리키는 날이라 당일·익일 선택을 겸한다.
     */
    data class ChangeSheetTime(
        val field: HomeTimeField,
        val date: LocalDate,
        val time: LocalTime,
    ) : HomeUiIntent

    /** 시트의 확인 — 임시 값을 기록 범위로 확정한다. */
    data object ConfirmTimeSheet : HomeUiIntent

    data object DismissTimePicker : HomeUiIntent

    /** 전송 스냅샷을 확정하고 데이터 전송 동의 화면으로 이동한다. 생성 API 는 동의 완료 후에만 호출된다. */
    data object CreateDraft : HomeUiIntent

    /**
     * 원천별 권한 도트를 다시 본다. 판정은 화면이 하고 결과만 싣는다.
     *
     * 권한은 사용자가 언제든 바꾸므로 캐시하지 않고 홈 재진입(ON_RESUME)마다 다시 본다.
     */
    data class RefreshSourcePermissions(
        val photo: DataSourceStatus,
        val calendar: DataSourceStatus,
        val location: DataSourceStatus,
        val notification: DataSourceStatus,
    ) : HomeUiIntent

    /**
     * 저장된 위치정보 약관 동의를 다시 판정한다.
     *
     * 이 ViewModel 은 계정 경계를 넘어 살아남고, 약관 화면에 다녀와 동의하고 돌아오는 경로도
     * 있으므로 진입·복귀마다 다시 본다.
     */
    data object RefreshLocationConsent : HomeUiIntent

    /** 동의 화면에서 제출을 마치고 복귀했는지 확인한다. 홈 재진입(ON_RESUME)마다 1회 소비한다. */
    data object ConsumeDraftConsentResult : HomeUiIntent

    data object RetryDraft : HomeUiIntent

    data object ContinueWaiting : HomeUiIntent

    data object StartNewDraft : HomeUiIntent

    data object ViewDraft : HomeUiIntent

    /** 생성 중인 초안의 로딩 화면으로 들어간다. */
    data object OpenDraftLoading : HomeUiIntent

    /** 지난 기록 전용 화면을 연다. 목록·동기화는 그 화면이 소유한다. */
    data object OpenPastRecords : HomeUiIntent
}
