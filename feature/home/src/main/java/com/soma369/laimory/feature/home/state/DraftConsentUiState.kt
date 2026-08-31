package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermType
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
    /**
     * 아직 동의가 없거나 개정으로 버전이 어긋난 문서. **기본은 모두 해제**다.
     *
     * 정본은 서버 이력이다. 이미 동의한 것은 여기 오지 않는다 — 서버에 철회 API 가 없어
     * 화면에서 해제해도 실제로 철회되지 않으므로, 되돌릴 수 없는 것을 되돌릴 수 있게 보이지 않는다.
     */
    val pendingTerms: List<TermDocument> = emptyList(),
    /** 이미 동의한 문서. 확인은 끝났고 원문을 다시 열어 볼 길만 남긴다. */
    val agreedTerms: List<TermDocument> = emptyList(),
    val checkedTerms: Set<TermType> = emptySet(),
    /** 현재 생성 시도에서 사용자가 전송에서 제외한 항목의 rawId. 스냅샷 항목의 부분집합이다. */
    val excludedRawIds: Set<String> = emptySet(),
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    /**
     * 위치 지도를 그려도 되는지. false 면 `GoogleMap` 을 composition 에 넣지 않는다 —
     * 지도를 그리는 것 자체가 카메라 영역을 Google 로 보내는 일이라 동의·키 확인이 먼저다.
     */
    val isMapRenderAllowed: Boolean = false,
) : UiState {
    /** 남은 필수 동의를 전부 확인했는지. 받을 것이 없으면 이미 충족이다. */
    val isAllTermsChecked: Boolean
        get() = pendingTerms.all { it.termType in checkedTerms }

    /** 제외를 반영한 실제 전송 예정 건수. */
    val includedTotal: Int
        get() = (content?.sentTotal ?: 0) - excludedRawIds.size

    fun isIncluded(itemKey: String): Boolean = itemKey !in excludedRawIds

    /**
     * 위치정보 전송 Switch 의 상태.
     *
     * 위치 항목 하나라도 제외돼 있으면 OFF 로 본다 — 위치는 개별 토글을 제공하지 않으므로 중간
     * 상태가 생기지 않지만, 이전 시도에서 넘어온 값이나 다른 경로로 섞여도 켜짐으로 보이지 않게 한다.
     * 표시할 위치가 없으면 켜 둔 것으로 본다(끌 대상이 없다).
     */
    val isLocationIncluded: Boolean
        get() = content?.locationRawIds.orEmpty().none { it in excludedRawIds }

    /** 유형 안에서 사용자가 제외한 건수. */
    fun excludedCountOf(group: DraftConsentTypeGroup): Int {
        val summary = content?.summaryOf(group) ?: return 0
        return summary.sections.sumOf { section -> section.items.count { it.key in excludedRawIds } }
    }

    /** 필수 동의를 모두 완료하고 전송할 항목이 1건 이상 남아 있어야 생성 CTA 가 활성화된다. */
    val canSubmit: Boolean
        get() = content != null && isAllTermsChecked && !isSubmitting && includedTotal > 0
}
