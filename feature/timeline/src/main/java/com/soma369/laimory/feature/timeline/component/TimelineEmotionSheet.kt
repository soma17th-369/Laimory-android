package com.soma369.laimory.feature.timeline.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import com.soma369.laimory.core.ui.component.EmotionIcon
import com.soma369.laimory.core.ui.component.sheet.LaimorySheetDragHandle
import com.soma369.laimory.core.ui.component.sheet.LaimorySheetHeader
import com.soma369.laimory.core.ui.model.displayLabel
import com.soma369.laimory.core.ui.model.toUiEmotionOrNull
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.timeline.state.TimelineEmotionSheetPurpose
import com.soma369.laimory.feature.timeline.state.TimelineEmotionSheetState

/**
 * 하루 기록을 확정하기 전에 그날의 감정을 고르는 바텀시트.
 *
 * 서버가 감정을 필수로 받으므로 시트는 항상 하나가 선택된 채로 열리고(`무덤덤`), `확인`은 언제나
 * 누를 수 있다. 저장 중에는 닫기·바깥 탭·뒤로가기를 모두 막는다 — 요청이 떠 있는 동안 시트가 사라지면
 * 완료 뒤의 안내와 화면 종결이 갈 곳을 잃는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimelineEmotionSheet(
    state: TimelineEmotionSheetState,
    isSaving: Boolean,
    onSelect: (TimelineEmotion) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    // 저장 확정과 감정 수정이 같은 시트를 쓴다. 확인을 누르면 서버 경로가 갈리므로(save 대
    // emotion) 헤더와 버튼만 갈라 무엇이 일어날지 알린다 — 수정인데 "확인" 만 보이면 다시
    // 저장되는 것처럼 읽힌다. 안내 문구는 묻는 것이 같아 그대로 둔다.
    val isEditing = state.purpose == TimelineEmotionSheetPurpose.EDIT_EMOTION
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius),
        dragHandle = { LaimorySheetDragHandle() },
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = !isSaving),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.large)
                    .padding(bottom = Spacing.extraLarge2),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge2),
        ) {
            LaimorySheetHeader(
                title = if (isEditing) "감정 바꾸기" else "감정 선택하기",
                onClose = onDismiss,
                closeEnabled = !isSaving,
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge3)) {
                Text(
                    // 안내 문구는 두 경우가 같다 — 묻는 것이 `그날의 기분` 으로 같고, 수정이라고
                    // 시제를 바꾸면 어색하다. 무엇이 일어날지는 헤더와 버튼이 말한다.
                    text = "${state.dateLabel} 하루, 어떤 기분이었나요?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                EmotionSelectorRow(
                    selected = state.selected,
                    enabled = !isSaving,
                    onSelect = onSelect,
                )
            }
            Button(
                onClick = onConfirm,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(ConfirmButtonHeight)
                        .semantics { if (isSaving) stateDescription = "저장 중" },
                enabled = !isSaving,
                shape = RoundedCornerShape(ConfirmButtonCornerRadius),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(ProgressIndicatorSize),
                        strokeWidth = ProgressIndicatorStroke,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = if (isEditing) "바꾸기" else "확인",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmotionSelectorRow(
    selected: TimelineEmotion,
    enabled: Boolean,
    onSelect: (TimelineEmotion) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimelineEmotion.SELECTABLE.forEach { emotion ->
            EmotionSelectorItem(
                emotion = emotion,
                isSelected = emotion == selected,
                enabled = enabled,
                onClick = { onSelect(emotion) },
            )
        }
    }
}

@Composable
private fun EmotionSelectorItem(
    emotion: TimelineEmotion,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val uiEmotion = emotion.toUiEmotionOrNull()
    Column(
        modifier =
            Modifier.selectable(
                selected = isSelected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ItemSpacing),
    ) {
        Box(
            modifier = Modifier.size(FaceSlotSize),
            contentAlignment = Alignment.Center,
        ) {
            // 고르지 않은 감정은 색을 빼지 않고 흐리게만 둔다 — 팔레트가 그대로 보여야 무엇을 고르는지 안다.
            EmotionIcon(
                emotion = uiEmotion,
                modifier = Modifier.alpha(if (isSelected) 1f else UNSELECTED_ALPHA),
            )
        }
        Text(
            text = uiEmotion.displayLabel(),
            style = MaterialTheme.typography.labelLarge,
            color =
                if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}

private val SheetCornerRadius = 20.dp
private val FaceSlotSize = 52.dp
private val ItemSpacing = 10.dp
private val ConfirmButtonHeight = 52.dp
private val ConfirmButtonCornerRadius = 16.dp
private val ProgressIndicatorSize = 18.dp
private val ProgressIndicatorStroke = 2.dp
private const val UNSELECTED_ALPHA = 0.4f

@Preview(name = "감정 선택 시트", showBackground = true)
@Composable
private fun TimelineEmotionSheetPreview() {
    LaimoryTheme {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge2),
        ) {
            LaimorySheetHeader(title = "감정 선택하기", onClose = {})
            Text(
                text = "오늘 하루, 어떤 기분이었나요?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EmotionSelectorRow(
                selected = TimelineEmotion.DEFAULT_SELECTION,
                enabled = true,
                onSelect = {},
            )
        }
    }
}
