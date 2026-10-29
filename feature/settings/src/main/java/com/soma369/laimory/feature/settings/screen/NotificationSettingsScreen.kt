package com.soma369.laimory.feature.settings.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.domain.model.reminder.DailyReminderSettings
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.core.ui.component.LaimoryTopAppBar
import com.soma369.laimory.core.ui.component.timepicker.LaimoryTimePickerSheet
import com.soma369.laimory.core.ui.component.timepicker.LaimoryTimePickerValue
import com.soma369.laimory.core.ui.component.timepicker.TimePickerField
import com.soma369.laimory.core.ui.permission.openAppNotificationSettings
import com.soma369.laimory.core.ui.permission.rememberAppNotificationsEnabled
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.settings.state.NotificationSettingsUiContent
import com.soma369.laimory.feature.settings.state.NotificationSettingsUiIntent
import com.soma369.laimory.feature.settings.state.NotificationSettingsUiSideEffect
import com.soma369.laimory.feature.settings.state.NotificationSettingsUiState
import com.soma369.laimory.feature.settings.viewmodel.NotificationSettingsViewModel
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NotificationSettingsRoute(
    innerPadding: PaddingValues,
    viewModel: NotificationSettingsViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.sendIntent(NotificationSettingsUiIntent.Initialize)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    NotificationSettingsContent(
        innerPadding = innerPadding,
        state = state,
        onIntent = viewModel::sendIntent,
        sideEffectFlow = viewModel.sideEffect,
    )
}

@Composable
private fun NotificationSettingsContent(
    innerPadding: PaddingValues,
    state: NotificationSettingsUiState,
    onIntent: (NotificationSettingsUiIntent) -> Unit,
    sideEffectFlow: Flow<NotificationSettingsUiSideEffect>,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    LaunchedEffect(sideEffectFlow) {
        sideEffectFlow.collect { effect ->
            when (effect) {
                is NotificationSettingsUiSideEffect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    // 기기가 알림을 띄울 수 있는지는 런타임 권한이 아니라 시스템 상태로 본다. 권한만 보면
    // Android 12 이하에서는 늘 허용으로 나와, 설정에서 앱 알림을 끈 기기를 영영 알 수 없다.
    // 사용자가 시스템 설정에서 바꾸는 값이라 저장하지 않고 화면이 다시 보일 때마다 묻는다.
    val context = LocalContext.current
    val areNotificationsEnabled = rememberAppNotificationsEnabled()

    NotificationSettingsScreen(
        innerPadding = innerPadding,
        state = state,
        isDeviceNotificationBlocked = !areNotificationsEnabled,
        // 런타임 요청은 한 번 거부되면 다시 뜨지 않고 12 이하에는 요청 자체가 없다. 켤 수 있는
        // 자리는 알림 설정 화면뿐이라 그곳을 직접 연다.
        onOpenSystemSettings = { openAppNotificationSettings(context) },
        onIntent = onIntent,
    )
}

@Composable
private fun NotificationSettingsScreen(
    innerPadding: PaddingValues,
    state: NotificationSettingsUiState,
    isDeviceNotificationBlocked: Boolean,
    onOpenSystemSettings: () -> Unit,
    onIntent: (NotificationSettingsUiIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
    ) {
        LaimoryTopAppBar(
            title = {
                Text(
                    text = "알림",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            onBackClick = { onIntent(NotificationSettingsUiIntent.NavigateBack) },
        )
        when (val content = state.content) {
            NotificationSettingsUiContent.Loading -> NotificationSettingsLoading()
            NotificationSettingsUiContent.LoadFailed ->
                NotificationSettingsLoadFailed(
                    onRetryClick = { onIntent(NotificationSettingsUiIntent.RetryLoad) },
                )
            is NotificationSettingsUiContent.Settings ->
                NotificationToggleList(
                    isPushEnabled = content.isPushEnabled,
                    reminder = state.reminder,
                    isDeviceNotificationBlocked = isDeviceNotificationBlocked,
                    onOpenSystemSettings = onOpenSystemSettings,
                    onIntent = onIntent,
                )
        }
    }
    state.reminderTimeDraft?.let { draft ->
        ReminderTimeSheet(
            time = draft,
            onTimeChange = { time -> onIntent(NotificationSettingsUiIntent.ReminderTimeChanged(time)) },
            onConfirm = { onIntent(NotificationSettingsUiIntent.ConfirmReminderTime) },
            onDismiss = { onIntent(NotificationSettingsUiIntent.DismissReminderTimePicker) },
        )
    }
}

@Composable
private fun NotificationToggleList(
    isPushEnabled: Boolean,
    reminder: DailyReminderSettings,
    isDeviceNotificationBlocked: Boolean,
    onOpenSystemSettings: () -> Unit,
    onIntent: (NotificationSettingsUiIntent) -> Unit,
) {
    // 카드로 감싸지 않는다. 설정 화면은 여러 갈래를 나눠 보여 줘야 해서 구획이 필요하지만,
    // 여기는 한 갈래로 들어온 화면이라 목록 하나뿐이다. 나누는 것은 선 하나 — 모든 알림을 막는
    // 전체 알림과, 그 아래 알림 종류별 묶음은 성격이 달라서다.
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = LIST_TOP_PADDING, bottom = LIST_BOTTOM_PADDING),
    ) {
        NotificationToggleRow(
            title = "전체 알림",
            description = "끄면 아래 알림도 오지 않아요.",
            isChecked = isPushEnabled,
            isEnabled = true,
            onCheckedChange = { isEnabled -> onIntent(NotificationSettingsUiIntent.PushToggled(isEnabled)) },
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = ROW_HORIZONTAL_PADDING, vertical = Spacing.small),
            color = MaterialTheme.colorScheme.surfaceVariant,
        )
        NotificationToggleRow(
            title = "일일 리마인더",
            description = "하루를 기록할 시간에 알려드려요.",
            isChecked = reminder.isEnabled,
            // 전체가 꺼져 있으면 눌러도 오지 않는 알림이라 잠근다. 값은 그대로 둔다.
            isEnabled = isPushEnabled,
            onCheckedChange = { isEnabled -> onIntent(NotificationSettingsUiIntent.DailyReminderToggled(isEnabled)) },
        )
        // 시각은 리마인더에 딸린 값이라 켜져 있을 때만 바로 아래에 붙인다. 끈 동안은 의미가 없는 값이고, 숨겨야
        // 둘이 한 묶음이라는 것이 드러난다. 고른 시각은 남아 있다가 다시 켜면 그대로 나온다.
        AnimatedVisibility(
            visible = reminder.isEnabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            ReminderTimeRow(
                time = reminder.time,
                // 전체가 꺼져 있으면 리마인더 줄과 함께 잠근다.
                isEnabled = isPushEnabled,
                onClick = { onIntent(NotificationSettingsUiIntent.OpenReminderTimePicker) },
            )
        }
        // 알림이 오기로 돼 있는데 기기가 띄우지 못하는 상태. 여기만 앱에서 해결할 수 없어
        // 시스템 설정으로 보낸다.
        if (isPushEnabled && isDeviceNotificationBlocked) {
            DeviceNotificationNotice(onOpenSystemSettings = onOpenSystemSettings)
        }
    }
}

/**
 * 스위치 한 줄.
 *
 * 줄마다 서로 다른 알림을 각각 켜고 끄는 자리라 스위치로 둔다 — 체크는 여러 후보 중 하나를
 * 고르는 모양이라(테마 화면이 그렇다) 무엇 하나를 골라야 하는 것처럼 읽힌다. 값은 그 자리에서
 * 바로 적용되므로 저장 버튼도 없다.
 *
 * 조작은 줄 전체가 받는다. 스위치에는 [Switch] 의 `onCheckedChange` 를 주지 않아 별도 초점이
 * 생기지 않고, 접근성에는 줄 하나가 스위치로 읽힌다.
 *
 * 보내는 중이라는 표시는 두지 않는다. 스위치는 누른 즉시 움직이고, 서버가 거절한 경우에만
 * 되돌아가며 그때 안내가 뜬다.
 */
@Composable
private fun NotificationToggleRow(
    title: String,
    description: String,
    isChecked: Boolean,
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val contentColor =
        if (isEnabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = ROW_MIN_HEIGHT)
                // 누를 수 있는 영역은 화면 폭 전체다. 여백은 글자 자리를 잡을 뿐이다.
                .toggleable(
                    value = isChecked,
                    enabled = isEnabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                ).padding(horizontal = ROW_HORIZONTAL_PADDING, vertical = ROW_VERTICAL_PADDING),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LABEL_GAP),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = isChecked,
            // 줄 전체가 이미 조작을 받는다. 여기에 또 주면 초점이 둘로 갈린다.
            onCheckedChange = null,
            enabled = isEnabled,
        )
    }
}

/**
 * 리마인더 시각 줄. 누르면 시각 시트를 연다.
 *
 * 리마인더 줄의 하위 줄이다. 위 줄에 간격 없이 붙이고 높이와 글자를 한 단계 낮춰, 따로 선 설정이 아니라 위 줄에
 * 딸린 값으로 읽히게 한다. 값은 오른쪽에 둔다 — 무엇을 고른 상태인지 들어가 보지 않아도 읽힌다.
 */
@Composable
private fun ReminderTimeRow(
    time: LocalTime,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    val contentColor =
        if (isEnabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = SUB_ROW_MIN_HEIGHT)
                .clickable(enabled = isEnabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = ROW_HORIZONTAL_PADDING),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "알림 시각",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
        )
        Text(
            text = time.format(REMINDER_TIME_FORMAT),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 리마인더 시각 시트. 공용 타임 피커에 시각 한 줄만 둔다.
 *
 * 날짜는 다루지 않는다. 피커 값이 날짜를 요구해 아무 날짜나 얹지만 화면에는 나오지 않는다.
 */
@Composable
private fun ReminderTimeSheet(
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    // 줄이 하나라 처음부터 펼쳐 둔다. 접는 것은 화면의 일이 아니라 시트 안의 표시라 여기서 든다.
    var expandedFieldId by remember { mutableStateOf<String?>(REMINDER_TIME_FIELD_ID) }
    LaimoryTimePickerSheet(
        fields =
            listOf(
                TimePickerField(
                    id = REMINDER_TIME_FIELD_ID,
                    label = "알림 시각",
                    value = LaimoryTimePickerValue(date = PLACEHOLDER_DATE, time = time),
                ),
            ),
        expandedFieldId = expandedFieldId,
        onExpandedFieldChange = { id -> expandedFieldId = id },
        onValueChange = { _, value, _ -> onTimeChange(value.time) },
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        title = "일일 리마인더",
    )
}

@Composable
private fun DeviceNotificationNotice(onOpenSystemSettings: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ROW_HORIZONTAL_PADDING)
                .padding(top = Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        Text(
            text = "이 기기에서는 알림이 표시되지 않아요",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "알림은 켜져 있지만 기기의 알림 표시가 꺼져 있어요. 알림 설정에서 켤 수 있어요.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onOpenSystemSettings) {
            Text("알림 설정 열기")
        }
    }
}

@Composable
private fun NotificationSettingsLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/**
 * 조회 실패.
 *
 * 임의 기본값을 그리지 않는다 — 서버가 권위인 값이라, 켜 둔 적 없는 설정을 켜져 있다고 말하게 된다.
 */
@Composable
private fun NotificationSettingsLoadFailed(onRetryClick: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.extraLarge2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "알림 설정을 불러오지 못했어요",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "네트워크 상태를 확인한 뒤 다시 시도해주세요.",
            modifier = Modifier.padding(top = Spacing.small),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(
            onClick = onRetryClick,
            modifier = Modifier.padding(top = Spacing.extraLarge),
        ) {
            Text("다시 시도")
        }
    }
}

private const val REMINDER_TIME_FIELD_ID = "reminder_time"

/** 피커 값이 요구하는 날짜 자리. 날짜 열이 없어 화면에 나오지 않고 바뀌지도 않는다. */
private val PLACEHOLDER_DATE: LocalDate = LocalDate.of(2000, 1, 1)

/** `오후 9:00`. 기기 언어와 관계없이 앱 문구와 같은 한국어로 쓴다. */
private val REMINDER_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)

private val ROW_MIN_HEIGHT = 64.dp

/** 하위 줄 높이. 터치 최소 크기(48)는 지킨다. */
private val SUB_ROW_MIN_HEIGHT = 48.dp
private val ROW_VERTICAL_PADDING = 4.dp

/** 제목과 설명 사이. */
private val LABEL_GAP = 2.dp

/**
 * 줄의 가로 여백.
 *
 * 시안은 목록에 20, 줄에 4 를 나눠 뒀다. 코드에서는 둘을 합쳐 줄 안에 둔다 — 글자가 시작하는
 * 자리는 같고, 누를 수 있는 영역만 화면 폭 전체로 남는다. 목록 바깥에 여백을 두면 가장자리를
 * 눌렀을 때 아무 일도 일어나지 않는다.
 */
private val ROW_HORIZONTAL_PADDING = 24.dp

/** 목록 위·아래 여백. 시안 값. */
private val LIST_TOP_PADDING = 8.dp
private val LIST_BOTTOM_PADDING = 24.dp

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun NotificationSettingsPreview() {
    LaimoryTheme {
        NotificationSettingsScreen(
            innerPadding = PaddingValues(),
            state =
                NotificationSettingsUiState(
                    content =
                        NotificationSettingsUiContent.Settings(isPushEnabled = true),
                ),
            isDeviceNotificationBlocked = false,
            onOpenSystemSettings = {},
            onIntent = {},
        )
    }
}

@Preview(name = "전체 알림 OFF", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun NotificationSettingsPushOffPreview() {
    LaimoryTheme {
        NotificationSettingsScreen(
            innerPadding = PaddingValues(),
            state =
                NotificationSettingsUiState(
                    content =
                        NotificationSettingsUiContent.Settings(isPushEnabled = false),
                ),
            isDeviceNotificationBlocked = true,
            onOpenSystemSettings = {},
            onIntent = {},
        )
    }
}

@Preview(name = "기기 알림 꺼짐 안내", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun NotificationSettingsBlockedPreview() {
    LaimoryTheme {
        NotificationSettingsScreen(
            innerPadding = PaddingValues(),
            state =
                NotificationSettingsUiState(
                    content =
                        NotificationSettingsUiContent.Settings(isPushEnabled = true),
                    reminder = DailyReminderSettings(isEnabled = false, time = DailyReminderSettings.DEFAULT_TIME),
                ),
            isDeviceNotificationBlocked = true,
            onOpenSystemSettings = {},
            onIntent = {},
        )
    }
}

@Preview(name = "조회 실패", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun NotificationSettingsLoadFailedPreview() {
    LaimoryTheme {
        NotificationSettingsScreen(
            innerPadding = PaddingValues(),
            state = NotificationSettingsUiState(content = NotificationSettingsUiContent.LoadFailed),
            isDeviceNotificationBlocked = false,
            onOpenSystemSettings = {},
            onIntent = {},
        )
    }
}
