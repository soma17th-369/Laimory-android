package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.ui.permission.DataSourceStatus

/**
 * 원천별 권한 도트가 보는 값 — "우리가 희망하는 옵션이 어디까지 켜져 있는가".
 *
 * 허용/미허용 이분법을 쓰지 않는다. 사진은 일부만 고를 수 있고 위치는 전경만 열려 있을 수 있는데,
 * 그 둘을 `꺼짐` 으로 뭉치면 이미 쓸 수 있는 데이터가 있는 사용자에게 "허용해 주세요" 라고만
 * 말하게 된다. 판정 어휘는 설정·온보딩이 쓰는 [DataSourceStatus] 를 그대로 쓴다.
 *
 * 권한은 사용자가 언제든 바꾸므로 **캐시하지 않고 화면 복귀마다 다시 본다.** 그래서 판정은
 * 화면이 하고 여기에는 결과만 담는다.
 *
 * **도트는 카드 진입 가능 여부와 묶이지 않는다.** 사진 일부 선택·위치 전경만 허용인 사용자도
 * 이미 읽을 수 있는 데이터가 있으므로 카드는 열려야 한다.
 */
@Immutable
data class HomeSourcePermissions(
    val photo: DataSourceStatus = DataSourceStatus.DENIED,
    val calendar: DataSourceStatus = DataSourceStatus.DENIED,
    /**
     * 위치. `항상 허용` 이면 [DataSourceStatus.GRANTED], 앱 사용 중만이면 [DataSourceStatus.LIMITED] 다.
     *
     * 활동 인식은 보지 않는다 — 그것은 이동수단 추론에만 쓰이고 없으면 평균 속도로 보완하므로
     * 수집 자체는 멀쩡하다. 항상 허용을 다 해 준 사용자에게 회색을 띄우면 무엇을 더 해야 하는지
     * 알 수 없다.
     */
    val location: DataSourceStatus = DataSourceStatus.DENIED,
    /**
     * 알림 접근. 설정 화면이 없는 기기는 [DataSourceStatus.UNSUPPORTED] 다.
     *
     * 그 기기에는 허용할 방법이 아예 없으므로 화면이 `탭하여 허용` 대신 지원하지 않는다는 안내로
     * 갈라야 한다.
     */
    val notification: DataSourceStatus = DataSourceStatus.DENIED,
)
