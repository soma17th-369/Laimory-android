package com.soma369.laimory.feature.settings.model

import androidx.annotation.DrawableRes
import com.soma369.laimory.core.ui.permission.DataPermission
import com.soma369.laimory.core.ui.permission.DataPermissionAction
import com.soma369.laimory.core.ui.permission.DataSourceStatus
import com.soma369.laimory.core.ui.permission.HealthDataSource
import com.soma369.laimory.core.ui.permission.LocationPermissionStep
import com.soma369.laimory.core.ui.R as CoreUiR

/**
 * 설정 `데이터 소스` 목록에 놓이는 소스 하나.
 *
 * 목록 순서는 하루 기록에 쓰이는 비중 순이다. 권한 문자열이나 Android 판정은 들고 있지 않다 —
 * 그쪽은 [DataPermission] 과 `core:ui` 의 권한 경계가 맡는다.
 */
enum class DataSourceUiModel(
    val permission: DataPermission,
    val label: String,
    @DrawableRes val iconRes: Int,
    /** 시트에서 이 데이터를 무엇에 쓰는지 설명하는 한 줄. 온보딩 문구와 같은 말을 쓴다. */
    val purpose: String,
    /**
     * 허용 범위에 대한 설명.
     *
     * 무엇을 읽고 무엇을 읽지 않는지, 무엇이 더 필요한지를 여기서만 말한다 — 온보딩 장 문구는
     * 그 데이터가 하루의 무엇이 되는지를 말하는 자리라 범위까지 적으면 문단이 길어져 정작
     * 읽어야 할 사람이 넘긴다.
     */
    val details: List<String>,
) {
    PHOTO(
        permission = DataPermission.PHOTO,
        label = "사진",
        iconRes = CoreUiR.drawable.ico_setting_datasource_photo,
        purpose = "촬영 시각과 위치로 그날의 순간을 타임라인에 놓아요. 기기에 저장된 사진을 읽기만 합니다.",
        details =
            listOf(
                "전체 허용이 부담스러우면 고른 사진만 허용해도 돼요.",
                "허용해도 어떤 사진을 기록에 넣을지는 직접 고를 수 있어요.",
            ),
    ),
    CALENDAR(
        permission = DataPermission.CALENDAR,
        label = "캘린더",
        iconRes = CoreUiR.drawable.ico_setting_datasource_calendar,
        purpose = "쓰던 캘린더의 일정을 읽어 하루의 계획과 만남을 복원해요.",
        details =
            listOf(
                "일정을 읽기만 하고 새로 만들거나 바꾸지 않아요.",
                "기기에 등록된 캘린더만 봐요. 계정을 따로 연결하지 않아요.",
            ),
    ),
    LOCATION(
        permission = DataPermission.LOCATION,
        label = "위치",
        iconRes = CoreUiR.drawable.ico_setting_datasource_location,
        purpose = "오간 길과 머문 장소로 하루의 뼈대를 만들어요. 배경에서도 기록하려면 항상 허용이 필요합니다.",
        details =
            listOf(
                "앱을 보고 있지 않은 동안에도 이으려면 `항상 허용`이 필요해요.",
                "정확한 위치 대신 대략적인 위치만 허용해도 기록은 이어져요.",
            ),
    ),
    NOTIFICATION(
        permission = DataPermission.NOTIFICATION_LISTENER,
        label = "알림",
        iconRes = CoreUiR.drawable.ico_setting_datasource_notification,
        purpose = "결제·배송·예약처럼 생활 이벤트가 담긴 알림만 골라 후보로 씁니다. 대화 알림은 읽지 않아요.",
        details =
            listOf(
                "결제·배송·예약처럼 생활 이벤트를 알리는 알림만 골라요.",
                "개인 대화와 광고 알림은 읽지 않아요.",
                "켠 뒤에 오는 알림부터 읽어요. 지난 알림은 가져오지 않아요.",
            ),
    ),
    HEALTH(
        permission = DataPermission.HEALTH,
        label = "헬스",
        iconRes = CoreUiR.drawable.ico_setting_datasource_health,
        purpose = "걸음수와 수면으로 하루의 활동과 휴식을 채워요. Health Connect 를 거쳐 읽습니다.",
        details =
            listOf(
                "걸음수와 수면만 읽어요.",
                "Health Connect 앱을 거치며, 거기서 언제든 되돌릴 수 있어요.",
            ),
    ),
    ;

    /**
     * 목록 오른쪽에 붙는 상태 문구.
     *
     * 소스마다 말이 다르다 — 위치의 `앱 사용 중에만` 을 사진과 같은 `일부 허용` 으로 뭉치면
     * 사용자가 무엇을 더 열어야 하는지 알 수 없다.
     *
     * 위치는 [status] 만으로 부족하다. `전경만` 과 `신체 활동만 남음` 이 같은
     * [DataSourceStatus.LIMITED] 라, 단계를 함께 받지 않으면 항상 허용해 둔 사용자에게
     * `앱 사용 중에만` 이라는 반대말이 뜬다.
     */
    fun statusLabel(
        status: DataSourceStatus,
        locationStep: LocationPermissionStep,
    ): String =
        when (this) {
            PHOTO ->
                when (status) {
                    DataSourceStatus.GRANTED -> "전체 허용됨"
                    DataSourceStatus.LIMITED -> "일부 사진만"
                    else -> "허용 안 됨"
                }

            LOCATION ->
                when {
                    status == DataSourceStatus.GRANTED -> "항상 허용됨"
                    // 위치는 이미 항상 허용이고 이동 수단을 읽을 권한만 없다. 시스템 다이얼로그가
                    // 쓰는 이름을 그대로 써야 사용자가 무엇을 켜는지 알아본다.
                    locationStep == LocationPermissionStep.ACTIVITY -> "신체 활동 필요"
                    status == DataSourceStatus.LIMITED -> "앱 사용 중에만"
                    else -> "허용 안 됨"
                }

            NOTIFICATION ->
                when (status) {
                    DataSourceStatus.GRANTED -> "허용됨"
                    DataSourceStatus.UNSUPPORTED -> "이 기기에서 불가"
                    else -> "설정 필요"
                }

            HEALTH ->
                when (status) {
                    DataSourceStatus.GRANTED -> "허용됨"
                    // 앱이 없으면 허용/거부 이전의 문제라, 무엇을 해야 하는지를 문구가 말해야 한다.
                    DataSourceStatus.UNSUPPORTED -> "Health Connect 필요"
                    else -> "허용 안 됨"
                }

            CALENDAR -> if (status == DataSourceStatus.GRANTED) "허용됨" else "허용 안 됨"
        }

    companion object {
        /**
         * 목록에 실제로 그릴 소스.
         *
         * 헬스는 Play Console 의 Health Connect 데이터 유형 신고가 끝나기 전까지 release 에서
         * 뺀다 — 허용을 받아도 수집이 켜지지 않는 항목을 보여 주면 사용자가 한 일이 헛일이 된다.
         */
        val visible: List<DataSourceUiModel>
            get() = entries.filter { it != HEALTH || HealthDataSource.isEnabled }
    }
}

/**
 * 상태 문구를 눈에 띄게 할지 여부.
 *
 * 다 열린 소스는 사용자가 할 일이 없으므로 조용히 둔다. 손봐야 하는 줄만 올린다 — 모든 줄을
 * 강조하면 강조가 아니고, 정상 상태를 강조하면 정작 고칠 곳이 묻힌다.
 */
val DataSourceStatus.needsAttention: Boolean
    get() = this == DataSourceStatus.LIMITED || this == DataSourceStatus.DENIED

/**
 * 시트 기본 버튼 문구. 누를 것이 없으면 null 이다.
 *
 * 같은 `앱 설정 열기` 라도 이미 허용된 소스에서는 **끄러 가는 길**이라 문구를 나눈다.
 */
fun DataPermissionAction.buttonLabel(status: DataSourceStatus): String? =
    when (this) {
        DataPermissionAction.REQUEST -> "허용하기"
        DataPermissionAction.RESELECT_PHOTOS -> "사진 다시 선택"
        DataPermissionAction.APP_SETTINGS ->
            when (status) {
                DataSourceStatus.GRANTED -> "설정에서 변경"
                // 시스템이 더 이상 묻지 않아 설정에서 켜는 수밖에 없다.
                DataSourceStatus.DENIED -> "설정에서 허용"
                else -> "앱 설정 열기"
            }

        DataPermissionAction.HEALTH_SETTINGS -> "Health Connect 에서 변경"
        DataPermissionAction.LISTENER_SETTINGS -> "알림 접근 설정 열기"
        DataPermissionAction.NONE -> null
    }
