package com.soma369.laimory.feature.settings.screen

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.settings.state.SettingsUiIntent
import com.soma369.laimory.feature.settings.state.SettingsUiState
import com.soma369.laimory.feature.settings.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import com.soma369.laimory.core.ui.R as CoreUiR

@Composable
fun SettingsRoute(
    innerPadding: PaddingValues,
    appVersionName: String,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsContent(
        innerPadding = innerPadding,
        appVersionName = appVersionName,
        state = state,
        onIntent = viewModel::sendIntent,
        snackbarFlow = viewModel.snackbar,
    )
}

@Composable
private fun SettingsContent(
    innerPadding: PaddingValues,
    appVersionName: String,
    state: SettingsUiState,
    onIntent: (SettingsUiIntent) -> Unit,
    snackbarFlow: Flow<String>,
) {
    val snackbarHostState = LocalSnackbarHostState.current

    LaunchedEffect(snackbarFlow) {
        snackbarFlow.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    SettingsScreen(
        innerPadding = innerPadding,
        appVersionName = appVersionName,
        state = state,
        onIntent = onIntent,
    )

    if (state.isLogoutDialogVisible) {
        LogoutConfirmDialog(
            isLoggingOut = state.isLoggingOut,
            onConfirm = { onIntent(SettingsUiIntent.LogoutConfirmed) },
            onDismiss = { onIntent(SettingsUiIntent.LogoutDismissed) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    innerPadding: PaddingValues,
    appVersionName: String,
    state: SettingsUiState,
    onIntent: (SettingsUiIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
    ) {
        CenterAlignedTopAppBar(
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            title = {
                Text(
                    text = "설정",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.extraLarge)
                    .padding(top = Spacing.small, bottom = Spacing.extraLarge2),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            AccountSummaryCard(provider = state.accountProvider)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                SettingsSection(title = "정보") {
                    SettingsGroup(
                        items =
                            listOf(
                                SettingsItem(
                                    iconRes = CoreUiR.drawable.ico_setting_keyhole,
                                    title = "개인정보 처리방침",
                                ),
                                SettingsItem(
                                    iconRes = CoreUiR.drawable.ico_setting_note,
                                    title = "서비스 이용약관",
                                ),
                                SettingsItem(
                                    iconRes = CoreUiR.drawable.ico_setting_info,
                                    title = "버전 정보",
                                    trailingText = "v$appVersionName",
                                ),
                            ),
                    )
                }
                SettingsSection(title = "계정") {
                    SettingsGroup(
                        items =
                            listOf(
                                SettingsItem(
                                    iconRes = CoreUiR.drawable.ico_setting_signout,
                                    title = "로그아웃",
                                    isEnabled = !state.isLoggingOut,
                                    showChevron = true,
                                    onClick = { onIntent(SettingsUiIntent.LogoutClicked) },
                                ),
                                SettingsItem(
                                    iconRes = CoreUiR.drawable.ico_setting_trash,
                                    title = "계정 삭제",
                                    trailingText = "준비 중",
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountSummaryCard(provider: SocialLoginProvider?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.large),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProviderAvatar(provider = provider)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
            ) {
                Text(
                    text = provider.accountTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = provider.accountSubtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProviderAvatar(provider: SocialLoginProvider?) {
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        val iconRes = provider.iconRes
        if (iconRes != null) {
            Image(
                modifier = Modifier.size(30.dp),
                painter = painterResource(iconRes),
                contentDescription = provider.accountTitle,
            )
        } else {
            Text(
                text = "L",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun SettingsGroup(items: List<SettingsItem>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            items.forEachIndexed { index, item ->
                SettingsRow(item)
                if (index != items.lastIndex) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(item: SettingsItem) {
    val contentColor =
        item.contentColor.takeUnless { it == Color.Unspecified }
            ?: MaterialTheme.colorScheme.onSurface
    val rowModifier =
        if (item.onClick == null) {
            Modifier
        } else {
            Modifier.clickable(enabled = item.isEnabled, onClick = item.onClick)
        }
    Row(
        modifier =
            rowModifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = Spacing.large, vertical = Spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(item.iconRes),
            contentDescription = null,
            tint = contentColor,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        item.trailingText?.let { trailingText ->
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (item.showChevron) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(CoreUiR.drawable.ico_default_caret_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LogoutConfirmDialog(
    isLoggingOut: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = { if (!isLoggingOut) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(Spacing.extraLarge2),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = 320.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier =
                        Modifier.padding(
                            start = Spacing.extraLarge2,
                            top = Spacing.extraLarge2,
                            end = Spacing.extraLarge2,
                            bottom = Spacing.extraLarge,
                        ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge),
                ) {
                    Text(
                        text = "로그아웃할까요?",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "이 기기에서 로그아웃하고 로그인 화면으로 이동합니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled = !isLoggingOut,
                            onClick = onDismiss,
                            shape = MaterialTheme.shapes.large,
                            contentPadding = PaddingValues(vertical = Spacing.medium),
                        ) {
                            Text("취소")
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = !isLoggingOut,
                            onClick = onConfirm,
                            shape = MaterialTheme.shapes.large,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(vertical = Spacing.medium),
                        ) {
                            if (isLoggingOut) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Text("로그아웃")
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class SettingsItem(
    @DrawableRes val iconRes: Int,
    val title: String,
    val trailingText: String? = null,
    val contentColor: Color = Color.Unspecified,
    val isEnabled: Boolean = true,
    val showChevron: Boolean = false,
    val onClick: (() -> Unit)? = null,
)

private val SocialLoginProvider?.accountTitle: String
    get() =
        when (this) {
            SocialLoginProvider.GOOGLE -> "Google 계정"
            SocialLoginProvider.KAKAO -> "Kakao 계정"
            null -> "로그인된 계정"
        }

private val SocialLoginProvider?.accountSubtitle: String
    get() =
        when (this) {
            SocialLoginProvider.GOOGLE -> "Google로 로그인됨"
            SocialLoginProvider.KAKAO -> "Kakao로 로그인됨"
            null -> "인증된 세션으로 로그인됨"
        }

private val SocialLoginProvider?.iconRes: Int?
    get() =
        when (this) {
            SocialLoginProvider.GOOGLE -> CoreUiR.drawable.ico_social_google_logo
            SocialLoginProvider.KAKAO -> CoreUiR.drawable.ico_social_kakao_logo
            null -> null
        }

@Preview(name = "Settings Default", apiLevel = 36, showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SettingsDefaultPreview() {
    LaimoryTheme {
        SettingsScreen(
            innerPadding = PaddingValues(),
            appVersionName = "1.0.0",
            state = SettingsUiState(accountProvider = SocialLoginProvider.GOOGLE),
            onIntent = {},
        )
    }
}

@Preview(name = "Settings Logout Dialog", apiLevel = 36, showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SettingsDialogPreview() {
    SettingsContentPreview(
        state =
            SettingsUiState(
                accountProvider = SocialLoginProvider.KAKAO,
                isLogoutDialogVisible = true,
            ),
    )
}

@Preview(name = "Settings Logout Progress", apiLevel = 36, showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SettingsProgressPreview() {
    SettingsContentPreview(
        state =
            SettingsUiState(
                accountProvider = SocialLoginProvider.KAKAO,
                isLogoutDialogVisible = true,
                isLoggingOut = true,
            ),
    )
}

@Composable
private fun SettingsContentPreview(state: SettingsUiState) {
    LaimoryTheme {
        CompositionLocalProvider(
            LocalSnackbarHostState provides remember { SnackbarHostState() },
        ) {
            SettingsContent(
                innerPadding = PaddingValues(),
                appVersionName = "1.0.0",
                state = state,
                onIntent = {},
                snackbarFlow = emptyFlow(),
            )
        }
    }
}

@Preview(name = "Settings Dark", apiLevel = 36, showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SettingsDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        SettingsScreen(
            innerPadding = PaddingValues(),
            appVersionName = "1.0.0",
            state = SettingsUiState(accountProvider = SocialLoginProvider.KAKAO),
            onIntent = {},
        )
    }
}
