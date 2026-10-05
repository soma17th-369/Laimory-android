package com.soma369.laimory.core.ui.permission

/**
 * 권한을 요청하기 직전에 보여 주는 수집 고지(Play 명시적 공개).
 *
 * Play 사용자 데이터 정책은 위치처럼 민감한 데이터에 **액세스하거나 수집하기 전에**, 메뉴를 찾아가지
 * 않아도 보이는 자리에서 무엇을 읽고 어디에 쓰는지 밝히라고 요구한다. 시스템 권한 창은 앱이 고칠 수
 * 없고, 사진 창은 촬영 위치를 읽는다는 말을 아예 하지 않는다 — 그래서 앱이 그 앞에서 말한다.
 *
 * 요청하는 자리(홈 카드 · 온보딩 장 · 설정 시트)가 모두 이 값을 쓴다. 자리마다 문구를 따로 두면
 * 한 곳만 고쳐지고 나머지가 다시 걸린다.
 */
enum class DataDisclosure(
    /** 고지를 읽은 뒤 요청할 권한. */
    val permission: DataPermission,
    val title: String,
    val body: String,
) {
    /**
     * 백그라운드 위치. Play 는 네 가지를 요구한다 — `위치` 라는 말, 앱이 닫혀 있거나 쓰지 않을 때도
     * 수집한다는 사실, 그 위치를 쓰는 기능, 쓰임새. 첫 문장은 Play 권장 문구 형식을 따른다.
     *
     * 신체 활동을 함께 적는 이유는 위치 요청이 이동수단 인식을 같은 창에 싣기 때문이다.
     */
    LOCATION(
        permission = DataPermission.LOCATION,
        title = "위치 정보 수집 안내",
        body =
            "라이모리는 앱이 종료되었거나 사용 중이 아닐 때도 위치 데이터를 수집하여, " +
                "머문 장소와 이동 경로를 하루 타임라인에 기록하는 기능을 지원합니다.\n\n" +
                "수집한 위치는 타임라인을 만들 때 서버로 전송되어 AI가 하루를 정리하는 데 쓰입니다. " +
                "이동 수단(걷기·차량)을 구분하려고 신체 활동 정보도 함께 사용합니다.",
    ),

    /**
     * 사진의 촬영 위치. 사진 권한과 함께 받는 `ACCESS_MEDIA_LOCATION` 은 창이 따로 뜨지 않아, 사용자가
     * 사진에서 위치가 읽힌다는 것을 알 수 있는 곳이 여기뿐이다.
     */
    PHOTO(
        permission = DataPermission.PHOTO,
        title = "사진 정보 이용 안내",
        body =
            "라이모리는 사진의 촬영 시각과 사진에 담긴 촬영 위치 정보를 읽어, " +
                "그날의 순간을 하루 타임라인에 놓습니다.\n\n" +
                "타임라인을 만들 때 고른 사진과 촬영 위치가 서버로 전송되어 " +
                "AI가 사진에 담긴 순간을 읽는 데 쓰입니다.",
    ),
}

/**
 * 이 권한을 지금 요청하기 전에 띄울 고지. 없으면 `null` 이다.
 *
 * - 위치: 받을 것이 남아 있으면 그 요청은 시스템 창이든 이 앱의 위치 권한 화면이든 런타임 요청이다.
 * - 사진: 아무것도 허용하지 않은 첫 요청만이다. `일부 선택` 에서 사진을 더 고르는 재요청은 촬영
 *   위치 권한까지 이미 받은 뒤라 새로 알릴 것이 없다.
 *
 * 순수 함수로 둬 조합을 테스트로 고정한다.
 */
fun dataDisclosureBeforeRequest(
    permission: DataPermission,
    locationStep: LocationPermissionStep,
    photoStatus: DataSourceStatus,
): DataDisclosure? =
    when (permission) {
        DataPermission.LOCATION -> DataDisclosure.LOCATION.takeIf { locationStep != LocationPermissionStep.GRANTED }
        DataPermission.PHOTO -> DataDisclosure.PHOTO.takeIf { photoStatus == DataSourceStatus.DENIED }
        else -> null
    }
