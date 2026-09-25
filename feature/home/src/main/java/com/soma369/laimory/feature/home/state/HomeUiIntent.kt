package com.soma369.laimory.feature.home.state

import com.soma369.laimory.core.ui.base.UiIntent
import com.soma369.laimory.core.ui.permission.DataPermissionEvent
import com.soma369.laimory.core.ui.permission.DataSourceStatus
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

sealed interface HomeUiIntent : UiIntent {
    /**
     * 화면 진입·복귀와 날짜가 바뀌는 시각([HomeDefaultDate.nextChangeAfter])에 오늘을 다시 계산한다.
     *
     * 날짜를 고르지 않았으면 기본 날짜로 옮긴다. 다른 갱신보다 먼저 보내야 뒤따르는 재조회가 옮긴
     * 날짜로 돈다.
     */
    data object RefreshToday : HomeUiIntent

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

    /** 고른 사진으로 확정하고 데이터 확인 화면으로 넘어간다. */
    data object ConfirmPhotoSelection : HomeUiIntent

    /**
     * 사진 없이 이어서 만든다.
     *
     * 0장 선택 후 확인과 나누는 이유는 의사가 다르기 때문이다 — 아무것도 고르지 않은 확인이
     * 실수인지 의도인지 구분되지 않는다.
     */
    data object ContinueWithoutPhotos : HomeUiIntent

    /** 날짜 피커를 연다. 지금 확정된 날짜·범위로 세션을 만든다. */
    data object ShowDatePicker : HomeUiIntent

    /** 날짜 피커를 닫는다. 열린 시간 시트까지 포함해 세션 전체를 버리고 아무것도 확정하지 않는다. */
    data object DismissDatePicker : HomeUiIntent

    /** 피커 안에서 날짜를 골랐다(격자·오늘·어제 칩). 세션 날짜만 바꾸고 그 날의 범위 잠금을 판정한다. */
    data class PickDate(
        val date: LocalDate,
    ) : HomeUiIntent

    /**
     * 피커의 확인. 세션의 날짜와 범위를 한 번에 확정한다.
     *
     * 범위를 바꿨으면 그 날의 판정이 [HomeRangeLock.EDITABLE] 로 확정될 때만 범위까지 반영한다 — 판정
     * 중이면 끝날 때까지 미루고, 잠겼거나 조회에 실패하면 날짜만 확정한다. 범위는 서버로 가는 요청이
     * 없어 뒤늦게 막아 줄 곳이 없다.
     */
    data object ConfirmDatePicker : HomeUiIntent

    /**
     * 날짜 피커가 보여 주는 달의 기록 상태를 받아 온다.
     *
     * 피커를 열 때와 달을 넘길 때마다 그 달을 요청한다 — 초안·저장 도트를 찍으려면 그 달에 무엇이
     * 있는지 알아야 한다. 월별 경량 조회라 달마다 불러도 부담이 적다.
     */
    data class LoadMonthlyRecords(
        val month: YearMonth,
    ) : HomeUiIntent

    /**
     * 고른 날짜의 서버 기록을 다시 본다. 화면 진입·복귀마다 보낸다.
     *
     * 다른 화면에서 초안을 저장하거나 지우고 돌아올 수 있고, 앱을 다시 켜면 완료 표시가 없어 이것만이
     * `타임라인 확인하기` 를 되찾는다.
     */
    data object RefreshRecordState : HomeUiIntent

    /** 날짜 피커의 범위 칩에서 시각 선택 시트를 열고 누른 줄을 펼친다. */
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

    /** 시트의 확인 — 날짜 피커 세션의 범위만 바꾼다. 확정은 피커의 확인이 한다. */
    data object ConfirmTimeSheet : HomeUiIntent

    /** 시트만 닫는다. 시트에서 바꾸던 값은 버리고 날짜 피커 세션은 그대로 둔다. */
    data object DismissTimePicker : HomeUiIntent

    /** 전송 스냅샷을 확정하고 데이터 전송 동의 화면으로 이동한다. 생성 API 는 동의 완료 후에만 호출된다. */
    data object CreateDraft : HomeUiIntent

    /** 확인 다이얼로그의 `만들기`. 확정해 둔 스냅샷을 그대로 제출한다. */
    data object ConfirmCreateDraft : HomeUiIntent

    /** 확인 다이얼로그의 `취소`·바깥 탭·뒤로가기. 제출용 스냅샷만 버린다. */
    data object DismissCreateConfirm : HomeUiIntent

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

    data object RetryDraft : HomeUiIntent

    data object ContinueWaiting : HomeUiIntent

    data object StartNewDraft : HomeUiIntent

    data object ViewDraft : HomeUiIntent

    /** 생성 중인 초안의 로딩 화면으로 들어간다. */
    data object OpenDraftLoading : HomeUiIntent

    /** 지난 기록 전용 화면을 연다. 목록·동기화는 그 화면이 소유한다. */
    data object OpenPastRecords : HomeUiIntent

    /**
     * 원천 카드를 눌렀다. 어디로 갈지는 ViewModel 이 데이터로 정한다.
     *
     * 사진은 선택 시트, 나머지는 유형 상세다. 볼 것도 권한도 없으면 화면이 권한 흐름을 대신
     * 태우므로 이 인텐트가 오지 않는다.
     */
    data class OpenSourceDetail(
        val kind: HomeSourceKind,
    ) : HomeUiIntent

    /**
     * 건강 상세를 연다. **debug 전용 진입점**이다.
     *
     * 건강은 홈 카드에 없고 릴리즈에서는 항목 단위로 뺄 수단도 없다(제품 결정). 개발 중 무엇이
     * 실리는지 확인할 자리가 필요해 수집 실험실과 같은 자리에 임시로 둔다.
     */
    data object OpenHealthDetail : HomeUiIntent

    /** 홈 카드에서 연 권한 요청과 그 결과. 분석에 기록한다. */
    data class PermissionEvent(
        val event: DataPermissionEvent,
    ) : HomeUiIntent
}
