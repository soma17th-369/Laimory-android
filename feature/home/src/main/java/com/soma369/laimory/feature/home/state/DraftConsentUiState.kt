package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.ui.base.UiState

/**
 * 데이터 전송 확인·동의 화면의 UI 상태.
 *
 * [content]가 null 이면 현재 생성 시도의 준비물이 없는 상태다(프로세스 재생성 등) —
 * 스냅샷을 복원하지 않고 홈에서 다시 준비하도록 안내한다.
 * 체크 상태는 현재 생성 시도에만 유효하며 새 스냅샷이 들어오면 초기화된다.
 */
@Immutable
data class DraftConsentUiState(
    val content: DraftConsentUiContent? = null,
    /** 현재 생성 시도에서 사용자가 전송에서 제외한 항목의 rawId. 스냅샷 항목의 부분집합이다. */
    val excludedRawIds: Set<String> = emptySet(),
    /**
     * 위치 전체 전송 여부. 제외 집합과 **따로** 소유한다.
     *
     * `위치 항목 중 하나라도 제외돼 있으면 OFF` 같은 파생 규칙을 쓰지 않는다 — 스위치를 끈 뒤
     * 새 위치가 수집되면 그 항목은 제외 집합에 없어 파생값이 ON 으로 되돌아간다.
     */
    val isLocationSendEnabled: Boolean = true,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    /**
     * 위치 지도를 그려도 되는지. false 면 `GoogleMap` 을 composition 에 넣지 않는다 —
     * 지도를 그리는 것 자체가 카메라 영역을 Google 로 보내는 일이라 동의·키 확인이 먼저다.
     *
     * 저장된 위치정보 약관 동의와 SDK 키 준비를 **둘 다** 만족해야 true 다. 판정 전에는 false 이며
     * 생성 시도마다 다시 판정한다.
     */
    val isMapRenderAllowed: Boolean = false,
) : UiState {
    /** 제외와 위치 전송 여부를 반영한 실제 전송 예정 건수. */
    val includedTotal: Int
        get() {
            val locationRawIds = content?.locationRawIds.orEmpty()
            val excluded =
                if (isLocationSendEnabled) excludedRawIds else excludedRawIds + locationRawIds
            return (content?.sentTotal ?: 0) - excluded.size
        }

    fun isIncluded(itemKey: String): Boolean = itemKey !in excludedRawIds

    /** 위치정보 전송 Switch 의 상태. 소유한 값을 그대로 보여 준다. */
    val isLocationIncluded: Boolean get() = isLocationSendEnabled

    /** 유형 안에서 사용자가 제외한 건수. */
    fun excludedCountOf(group: DraftConsentTypeGroup): Int {
        val summary = content?.summaryOf(group) ?: return 0
        return summary.sections.sumOf { section -> section.items.count { it.key in excludedRawIds } }
    }

    /**
     * 전송할 항목이 1건 이상 남아 있어야 생성 CTA 가 활성화된다.
     *
     * 필수 동의는 여기서 확인하지 않는다 — 온보딩이 받지 못하면 완료로 치지 않으므로 정상
     * 경로에는 남은 동의가 없고, 개정·구버전은 서버가 403 `-3001` 로 알려 준다.
     */
    val canSubmit: Boolean
        get() = content != null && !isSubmitting && includedTotal > 0
}
