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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.domain.model.settings.AppThemeMode
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.core.ui.component.LaimoryTopAppBar
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.settings.state.ThemeSettingsUiIntent
import com.soma369.laimory.feature.settings.state.ThemeSettingsUiSideEffect
import com.soma369.laimory.feature.settings.state.ThemeSettingsUiState
import com.soma369.laimory.feature.settings.viewmodel.ThemeSettingsViewModel
import kotlinx.coroutines.flow.Flow
import com.soma369.laimory.core.ui.R as CoreUiR

@Composable
fun ThemeSettingsRoute(
    innerPadding: PaddingValues,
    viewModel: ThemeSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ThemeSettingsContent(
        innerPadding = innerPadding,
        state = state,
        onIntent = viewModel::sendIntent,
        sideEffectFlow = viewModel.sideEffect,
    )
}

@Composable
private fun ThemeSettingsContent(
    innerPadding: PaddingValues,
    state: ThemeSettingsUiState,
    onIntent: (ThemeSettingsUiIntent) -> Unit,
    sideEffectFlow: Flow<ThemeSettingsUiSideEffect>,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    LaunchedEffect(sideEffectFlow) {
        sideEffectFlow.collect { effect ->
            when (effect) {
                is ThemeSettingsUiSideEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }
    ThemeSettingsScreen(
        innerPadding = innerPadding,
        state = state,
        onIntent = onIntent,
    )
}

@Composable
private fun ThemeSettingsScreen(
    innerPadding: PaddingValues,
    state: ThemeSettingsUiState,
    onIntent: (ThemeSettingsUiIntent) -> Unit,
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
                    text = "테마",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            onBackClick = { onIntent(ThemeSettingsUiIntent.NavigateBack) },
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = LIST_TOP_PADDING, bottom = LIST_BOTTOM_PADDING)
                    // 셋 중 하나만 고르는 목록이라는 것을 접근성 서비스에 한 번에 알린다.
                    .selectableGroup(),
        ) {
            THEME_OPTIONS.forEach { option ->
                ThemeOptionRow(
                    label = option.label,
                    isSelected = state.selected == option.mode,
                    onClick = { onIntent(ThemeSettingsUiIntent.Select(option.mode)) },
                )
            }
        }
    }
}

/**
 * 고를 수 있는 한 줄.
 *
 * 라디오 버튼 대신 체크로 둔다 — 시스템의 같은 성격 화면과 결을 맞춘다. 접근성에는 라디오로
 * 알린다: 셋 중 하나만 켜지는 자리라, 체크박스로 읽히면 여러 개를 켤 수 있다고 오인된다.
 */
@Composable
private fun ThemeOptionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = ROW_MIN_HEIGHT)
                .selectable(
                    selected = isSelected,
                    role = Role.RadioButton,
                    onClick = onClick,
                    // 누를 수 있는 영역은 화면 폭 전체다. 여백은 글자 자리를 잡을 뿐이다.
                ).padding(horizontal = ROW_HORIZONTAL_PADDING, vertical = ROW_VERTICAL_PADDING),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Box(
            modifier = Modifier.size(CHECK_SIZE),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    painter = painterResource(CoreUiR.drawable.ico_default_check_filled),
                    contentDescription = null,
                    modifier = Modifier.size(CHECK_SIZE),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private data class ThemeOption(
    val mode: AppThemeMode,
    val label: String,
)

/**
 * 보여 주는 순서.
 *
 * 직접 고르는 둘을 앞에 두고 OS 를 따르는 항목을 끝에 둔다 — 시스템 화면 모드 설정과 같은 순서다.
 */
private val THEME_OPTIONS =
    listOf(
        ThemeOption(AppThemeMode.LIGHT, "라이트"),
        ThemeOption(AppThemeMode.DARK, "다크"),
        ThemeOption(AppThemeMode.SYSTEM, "시스템 설정과 같이"),
    )

private val ROW_MIN_HEIGHT = 56.dp
private val ROW_VERTICAL_PADDING = 4.dp
private val ROW_HORIZONTAL_PADDING = 24.dp
private val CHECK_SIZE = 24.dp
private val LIST_TOP_PADDING = 8.dp
private val LIST_BOTTOM_PADDING = 24.dp

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ThemeSettingsPreview() {
    LaimoryTheme {
        ThemeSettingsScreen(
            innerPadding = PaddingValues(),
            state = ThemeSettingsUiState(selected = AppThemeMode.SYSTEM),
            onIntent = {},
        )
    }
}

@Preview(name = "다크 선택", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ThemeSettingsDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        ThemeSettingsScreen(
            innerPadding = PaddingValues(),
            state = ThemeSettingsUiState(selected = AppThemeMode.DARK),
            onIntent = {},
        )
    }
}

/** 저장값을 아직 읽기 전. 어느 줄에도 체크가 없다. */
@Preview(name = "값 도착 전", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ThemeSettingsLoadingPreview() {
    LaimoryTheme {
        ThemeSettingsScreen(
            innerPadding = PaddingValues(),
            state = ThemeSettingsUiState(selected = null),
            onIntent = {},
        )
    }
}
