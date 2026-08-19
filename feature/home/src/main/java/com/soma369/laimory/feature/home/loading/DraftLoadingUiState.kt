package com.soma369.laimory.feature.home.loading

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.ui.base.UiState
import java.time.LocalDate

/**
 * 생성 로딩 화면 상태.
 *
 * [recordDate]와 진행 여부는 활성 작업이 정본이고, [photoUris]와 건수는 제출 시점 스냅샷에서 온다.
 * 프로세스가 재시작돼 스냅샷이 사라지면 사진과 건수만 비고 작업 추적은 그대로다.
 */
@Immutable
data class DraftLoadingUiState(
    val recordDate: LocalDate? = null,
    val photoUris: List<String> = emptyList(),
    val photoCount: Int = 0,
    val calendarCount: Int = 0,
    val stayCount: Int = 0,
    /** 기기에 모인 수집 데이터의 보존 일수. 빌드마다 달라 안내 문구에 숫자를 박지 않는다. */
    val retentionDays: Int = 0,
    val stageStates: Map<DraftLoadingStage, DraftLoadingStageState> =
        DraftLoadingStage.entries.associateWith { DraftLoadingStageState.PENDING },
    val notice: DraftLoadingNotice? = null,
) : UiState

/** 진행 중이 아닐 때 화면이 대신 보여줄 안내와 선택지. */
@Immutable
data class DraftLoadingNotice(
    val message: String,
    val primaryAction: DraftLoadingAction?,
    val secondaryAction: DraftLoadingAction?,
)

/** 안내에 딸린 동작. [label]은 버튼 문구다. */
@Immutable
data class DraftLoadingAction(
    val label: String,
    val intent: DraftLoadingUiIntent,
)
