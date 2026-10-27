package com.soma369.laimory.feature.timeline.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.ui.base.UiState
import com.soma369.laimory.feature.timeline.model.TimelineRecordUiModel
import java.time.LocalDate

@Immutable
data class TimelineRecordUiState(
    val content: TimelineRecordUiContent = TimelineRecordUiContent.Loading,
    /** 기록 상태와 무관한 화면 모드. 최초 진입 시에만 기록 상태를 따라 정해진다. */
    val mode: TimelineRecordMode = TimelineRecordMode.READ,
    val memoEditor: TimelineMemoEditorState? = null,
    /**
     * 서버 응답을 아직 기다리는 메모. `이벤트 id → 보낸 값`.
     *
     * 커밋은 화면을 묶지 않으므로, 응답이 오기 전에 다른 이유로 세션이 다시 방출되면 방금 쓴 글이
     * 옛 값으로 되돌아간다. 그 위에 덧씌워 화면이 흔들리지 않게 한다. 성공하면 세션이 같은 값을
     * 들고 오므로 지우고, 실패하면 지우는 것이 곧 되돌리기다.
     */
    val pendingMemos: Map<Long, String?> = emptyMap(),
    val deleteTarget: TimelineRecordDeleteTarget? = null,
    val deleteDialogState: TimelineDeleteDialogState = TimelineDeleteDialogState.Hidden,
    /** 이벤트 삭제. 하루 기록 삭제와 대상도 성공 처리도 달라 상태를 나눠 둔다. */
    val eventDeleteDialogState: TimelineEventDeleteDialogState = TimelineEventDeleteDialogState.Hidden,
    val emotionSheet: TimelineEmotionSheetState? = null,
    val isSavingRecord: Boolean = false,
    /** 타임라인 첫 도착 코치마크가 짚고 있는 단계. null 이면 띄우지 않는다. */
    val coachmark: TimelineCoachmarkStep? = null,
    /** 이번 코치마크가 짚을 단계. 저장한 기록을 다시 볼 때는 `저장하기` 가 빠진다. */
    val coachmarkSteps: List<TimelineCoachmarkStep> = TimelineCoachmarkStep.entries,
) : UiState {
    val isDeleting: Boolean
        get() = deleteDialogState == TimelineDeleteDialogState.Deleting

    /**
     * 화면에 그릴 기록 — 아직 응답을 기다리는 메모를 덧씌운 값.
     *
     * 목록을 그리는 쪽은 [content] 대신 이 값을 본다. 커밋한 메모는 서버 응답을 기다리는 동안에도
     * 이미 쓴 글로 보여야 한다.
     */
    val displayedRecord: TimelineRecordUiModel?
        get() {
            val record = (content as? TimelineRecordUiContent.Record)?.value ?: return null
            if (pendingMemos.isEmpty()) return record
            return record.copy(
                events =
                    record.events.map { event ->
                        if (pendingMemos.containsKey(event.timelineEventId)) {
                            event.copy(memo = pendingMemos[event.timelineEventId])
                        } else {
                            event
                        }
                    },
            )
        }

    /**
     * 진행 중인 작업이 없어 화면 모드를 바꿔도 되는 상태인지.
     *
     * 편집 모드를 닫는 `X` 와 모드 전환에 함께 쓴다 — 저장·삭제가 도는 중에 모드를 끄면 결과를
     * 받을 화면이 사라진다.
     *
     * **메모 편집은 여기서 세지 않는다.** 메모는 포커스가 빠질 때 저장되므로, 무엇을 누르든 그
     * 동작이 먼저 커밋을 부르고 자기 일을 한다 — 메모 한 줄을 쓰는 동안 화면 전체가 굳지 않는다.
     *
     * 이벤트 삭제는 다이얼로그가 떠 있는 동안 전부 막는다. 삭제 중만 막으면 확인 창을 띄워 둔 채
     * 뒤에서 편집이 진행돼, 무엇을 지우는지 정한 화면과 확인을 누른 뒤의 화면이 달라진다.
     */
    val isModeSwitchable: Boolean
        get() =
            !isSavingRecord &&
                deleteDialogState == TimelineDeleteDialogState.Hidden &&
                eventDeleteDialogState == TimelineEventDeleteDialogState.Hidden

    /**
     * 타임라인 튜토리얼 코치마크를 시작해도 되는지 — 내용 있는 초안을 편집 화면으로 보고 있고, 메모를 쓰거나 다른 창이 떠 있지
     * 않을 때다.
     */
    val canStartCoachmark: Boolean
        get() {
            val record = (content as? TimelineRecordUiContent.Record)?.value ?: return false
            return !record.isSaved && mode.isEditing && canReplayCoachmark
        }

    /**
     * 설정 `사용 방법` 에서 다시 볼 때 코치마크를 시작해도 되는지 — 이벤트가 있는 기록이 보이고, 메모를 쓰거나 다른 창이 떠
     * 있지 않을 때다. 저장한 기록도 된다(편집 모드로 바꿔 짚는다).
     */
    val canReplayCoachmark: Boolean
        get() {
            val record = (content as? TimelineRecordUiContent.Record)?.value ?: return false
            return coachmark == null &&
                record.events.isNotEmpty() &&
                memoEditor == null &&
                emotionSheet == null &&
                isModeSwitchable
        }
}

/**
 * 열려 있는 메모 입력칸.
 *
 * 저장 중 상태를 들지 않는다 — 커밋은 포커스가 빠지는 즉시 편집기를 닫고 뒤에서 돈다. 기다리는
 * 동안 무엇이 보일지는 [TimelineRecordUiState.pendingMemos] 가 정한다.
 *
 * 글자수 상한은 입력칸이 직접 막으므로 여기서 다시 재지 않는다.
 */
@Immutable
data class TimelineMemoEditorState(
    val timelineEventId: Long,
    val originalMemo: String,
    val draftMemo: String,
) {
    val hasChanges: Boolean
        get() = draftMemo != originalMemo
}

@Immutable
data class TimelineRecordDeleteTarget(
    val recordDate: LocalDate,
)

@Immutable
sealed interface TimelineRecordUiContent {
    data object Loading : TimelineRecordUiContent

    /** 단건 조회 `-404` 등으로 기록이 이미 삭제됐거나 접근할 수 없는 경우. */
    data object Unavailable : TimelineRecordUiContent

    /** 네트워크 오류 등으로 단건 조회에 실패한 경우. 다시 시도할 수 있다. */
    data object LoadFailed : TimelineRecordUiContent

    data class Record(
        val value: TimelineRecordUiModel,
    ) : TimelineRecordUiContent
}
