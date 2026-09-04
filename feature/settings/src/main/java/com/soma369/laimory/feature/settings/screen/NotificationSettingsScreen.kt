package com.soma369.laimory.feature.settings.screen

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.domain.model.push.PushSettings
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.core.ui.component.LaimoryTopAppBar
import com.soma369.laimory.core.ui.permission.DataPermission
import com.soma369.laimory.core.ui.permission.DataSourceStatus
import com.soma369.laimory.core.ui.permission.rememberDataPermissionState
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.settings.state.NotificationSettingsUiContent
import com.soma369.laimory.feature.settings.state.NotificationSettingsUiIntent
import com.soma369.laimory.feature.settings.state.NotificationSettingsUiSideEffect
import com.soma369.laimory.feature.settings.state.NotificationSettingsUiState
import com.soma369.laimory.feature.settings.state.NotificationToggle
import com.soma369.laimory.feature.settings.viewmodel.NotificationSettingsViewModel
import kotlinx.coroutines.flow.Flow

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

    // 권한은 사용자가 시스템 설정에서 바꾸는 값이라 저장하지 않고 화면이 뜰 때마다 다시 묻는다.
    val permissionState = rememberDataPermissionState()

    NotificationSettingsScreen(
        innerPadding = innerPadding,
        state = state,
        isDeviceNotificationBlocked =
            permissionState.statusOf(DataPermission.APP_NOTIFICATION) != DataSourceStatus.GRANTED,
        onOpenSystemSettings = { permissionState.act(DataPermission.APP_NOTIFICATION) },
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
                    settings = content.value,
                    isDeviceNotificationBlocked = isDeviceNotificationBlocked,
                    onOpenSystemSettings = onOpenSystemSettings,
                    onToggle = { toggle, isEnabled ->
                        onIntent(NotificationSettingsUiIntent.ToggleChanged(toggle, isEnabled))
                    },
                )
        }
    }
}

@Composable
private fun NotificationToggleList(
    settings: PushSettings,
    isDeviceNotificationBlocked: Boolean,
    onOpenSystemSettings: () -> Unit,
    onToggle: (NotificationToggle, Boolean) -> Unit,
) {
    // 카드로 감싸지 않는다. 설정 화면은 여러 갈래를 나눠 보여 줘야 해서 구획이 필요하지만,
    // 여기는 한 갈래로 들어온 화면이라 목록 하나뿐이다 — 나눌 것이 없는데 테를 두르면 무엇과
    // 무엇을 가르는 선인지 알 수 없다.
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = LIST_TOP_PADDING, bottom = LIST_BOTTOM_PADDING),
    ) {
        NotificationToggleRow(
            title = "전체 알림",
            description = "끄면 아래 알림도 오지 않아요.",
            isChecked = settings.isPushEnabled,
            isEnabled = true,
            onCheckedChange = { isEnabled -> onToggle(NotificationToggle.PUSH, isEnabled) },
        )
        NotificationToggleRow(
            title = "일일 리마인더",
            description = "하루를 기록할 시간에 알려드려요. (21시)",
            isChecked = settings.isDailyReminderEnabled,
            // 전체가 꺼져 있으면 눌러도 오지 않는 알림이라 잠근다. 서버 값은 그대로 둔다.
            isEnabled = settings.isPushEnabled,
            onCheckedChange = { isEnabled -> onToggle(NotificationToggle.DAILY_REMINDER, isEnabled) },
        )
        // 서버가 보내기로 돼 있는데 기기가 띄우지 못하는 상태. 여기만 앱에서 해결할 수 없어
        // 시스템 설정으로 보낸다.
        if (settings.isPushEnabled && isDeviceNotificationBlocked) {
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
            text = "알림은 켜져 있지만 기기의 알림 표시가 꺼져 있어요. 시스템 설정에서 켤 수 있어요.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onOpenSystemSettings) {
            Text("시스템 설정 열기")
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

private val ROW_MIN_HEIGHT = 64.dp
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
                        NotificationSettingsUiContent.Settings(
                            PushSettings(isPushEnabled = true, isDailyReminderEnabled = true),
                        ),
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
                        NotificationSettingsUiContent.Settings(
                            PushSettings(isPushEnabled = false, isDailyReminderEnabled = true),
                        ),
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
                        NotificationSettingsUiContent.Settings(
                            PushSettings(isPushEnabled = true, isDailyReminderEnabled = false),
                        ),
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
