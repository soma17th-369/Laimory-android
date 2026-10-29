package com.soma369.laimory.feature.timeline.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.domain.usecase.UpdateTimelineEventPlaceUseCase
import com.soma369.laimory.core.ui.component.LaimoryTextField
import com.soma369.laimory.core.ui.component.sheet.LaimorySheetDragHandle
import com.soma369.laimory.core.ui.component.sheet.LaimorySheetHeader
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.timeline.state.TimelinePlaceSheetState
import com.soma369.laimory.core.ui.R as UiR

/**
 * 장소 고르기 시트(Figma B 시트 3438:6615, 직접 입력 C 3438:6958).
 *
 * 목록과 직접 입력이 한 시트 안에서 오간다. 후보는 이름뿐이라(서버가 거리·업종을 주지 않는다) 시안의
 * 설명 줄과 `지도에서 선택` 은 두지 않는다. 확인하면 시트는 바로 닫히고, 바뀐 장소는 칩이 먼저
 * 보여 준다 — 응답을 기다리며 시트를 붙잡아 두지 않는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimelinePlaceSheet(
    state: TimelinePlaceSheetState,
    onSelect: (String) -> Unit,
    onOpenDirectInput: () -> Unit,
    onCloseDirectInput: () -> Unit,
    onDirectInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius),
        dragHandle = { LaimorySheetDragHandle() },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.large)
                    .padding(bottom = Spacing.extraLarge2),
            verticalArrangement = Arrangement.spacedBy(Spacing.large),
        ) {
            when (state.step) {
                TimelinePlaceSheetState.Step.LIST -> PlaceListStep(state, onSelect, onOpenDirectInput, onDismiss)
                TimelinePlaceSheetState.Step.DIRECT_INPUT -> DirectInputStep(state, onCloseDirectInput, onDirectInputChange)
            }
            ConfirmButton(
                text = if (state.step == TimelinePlaceSheetState.Step.LIST) "이 장소로 할게요" else "확인",
                enabled = state.chosenPlace != null,
                onClick = onConfirm,
            )
        }
    }
}

@Composable
private fun PlaceListStep(
    state: TimelinePlaceSheetState,
    onSelect: (String) -> Unit,
    onOpenDirectInput: () -> Unit,
    onDismiss: () -> Unit,
) {
    LaimorySheetHeader(title = "어디에 계셨나요?", onClose = onDismiss)
    EventContext(state)
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        state.options.forEach { place ->
            PlaceOption(place = place, isSelected = place == state.selected, onClick = { onSelect(place) })
        }
    }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onOpenDirectInput)
                .padding(horizontal = 4.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(UiR.drawable.ico_default_pencil),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "직접 입력",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(UiR.drawable.ico_default_caret_right),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DirectInputStep(
    state: TimelinePlaceSheetState,
    onBack: () -> Unit,
    onValueChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(
                painter = painterResource(UiR.drawable.ico_default_caret_left),
                contentDescription = "목록으로",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = "장소 이름 입력",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
    Text(
        text = state.eventLabel,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    LaimoryTextField(
        value = state.directInput,
        onValueChange = onValueChange,
        label = "장소 이름",
        placeholder = "예: 성수 골목 국밥집",
        supportingText = "타임라인에 이 이름으로 보여요",
        counterText = "${state.directInput.length}/${UpdateTimelineEventPlaceUseCase.MAX_PLACE_LENGTH}",
        focusRequester = focusRequester,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun EventContext(state: TimelinePlaceSheetState) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(state.eventType.iconResource()),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = state.eventLabel,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Figma `PlaceSuggestionItem` — 핀 원 + 장소 이름 + 선택 원. 고른 줄은 연한 바탕과 굵은 테두리. */
@Composable
private fun PlaceOption(
    place: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(if (isSelected) colors.primaryContainer else colors.surface, shape)
                .border(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) colors.primary else colors.outlineVariant,
                    shape = shape,
                ).selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .background(if (isSelected) colors.surface else colors.surfaceContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(UiR.drawable.ico_setting_datasource_location),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = colors.onSurfaceVariant,
            )
        }
        Text(
            text = place,
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier =
                Modifier
                    .size(22.dp)
                    .then(
                        if (isSelected) {
                            Modifier.background(colors.primary, CircleShape)
                        } else {
                            Modifier.border(1.5.dp, colors.outline, CircleShape)
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    painter = painterResource(UiR.drawable.ico_default_check),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = colors.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun ConfirmButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
    }
}

private val SheetCornerRadius = 20.dp
