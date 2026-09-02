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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.core.ui.permission.DataPermission
import com.soma369.laimory.core.ui.permission.DataSourceSheet
import com.soma369.laimory.core.ui.permission.DataSourceStatus
import com.soma369.laimory.core.ui.permission.DataSourceUiModel
import com.soma369.laimory.core.ui.permission.LocationPermissionStep
import com.soma369.laimory.core.ui.permission.needsAttention
import com.soma369.laimory.core.ui.permission.rememberDataPermissionState
import com.soma369.laimory.core.ui.terms.rememberTermContentLauncher
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.LocalLaimoryColors
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
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.sendIntent(SettingsUiIntent.RefreshProfile)
        viewModel.sendIntent(SettingsUiIntent.RefreshTermLinks)
    }
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

    // 권한 상태는 저장하지 않고 화면이 뜰 때마다 Android 에 묻는다. 시스템 설정에 다녀오면
    // ON_RESUME 재조회가 목록 문구를 바로 따라오게 한다.
    val permissionState = rememberDataPermissionState()
    val termContentLauncher = rememberTermContentLauncher()
    var sheetSource by remember { mutableStateOf<DataSourceUiModel?>(null) }

    SettingsScreen(
        innerPadding = innerPadding,
        appVersionName = appVersionName,
        state = state,
        statusOf = permissionState::statusOf,
        locationStep = permissionState.locationStep,
        onDataSourceClick = { sheetSource = it },
        onOpenTerm = { document -> termContentLauncher.open(document.contentUrl) },
        onIntent = onIntent,
    )

    sheetSource?.let { source ->
        DataSourceSheet(
            source = source,
            status = permissionState.statusOf(source.permission),
            locationStep = permissionState.locationStep,
            action = permissionState.actionFor(source.permission),
            onAction = { permissionState.act(source.permission) },
            onDismiss = { sheetSource = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    innerPadding: PaddingValues,
    appVersionName: String,
    state: SettingsUiState,
    statusOf: (DataPermission) -> DataSourceStatus,
    /** 위치만 단계가 있어 상태 문구가 하나 더 필요하다. */
    locationStep: LocationPermissionStep,
    onDataSourceClick: (DataSourceUiModel) -> Unit,
    onOpenTerm: (TermDocument) -> Unit,
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
            AccountSummaryCard(provider = state.accountProvider, nickname = state.nickname)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.large)) {
                SettingsSection(title = "데이터 소스") {
                    SettingsGroup(
                        items =
                            DataSourceUiModel.visible.map { source ->
                                val status = statusOf(source.permission)
                                SettingsItem(
                                    iconRes = source.iconRes,
                                    title = source.label,
                                    trailingText = source.statusLabel(status, locationStep),
                                    // 열린 줄은 브랜드색, 손볼 줄은 본문색 + 점으로 가른다. 브랜드색은
                                    // 텍스트용 진한 값을 쓴다 — colorScheme.primary 는 배경 대비가
                                    // 2.59:1 이라 이 크기 글씨로는 읽히지 않는다.
                                    trailingColor =
                                        if (status.needsAttention) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            LocalLaimoryColors.current.primaryText
                                        },
                                    // 색만으로 구분하지 않는다 — 색을 구별하지 못해도 점으로 알아볼 수 있어야 한다.
                                    showTrailingDot = status.needsAttention,
                                    showChevron = true,
                                    onClick = { onDataSourceClick(source) },
                                )
                            },
                    )
                }
                SettingsSection(title = "정보") {
                    SettingsGroup(
                        items =
                            listOf(
                                // 게시된 원문은 서버 catalog 가 주소를 정한다. 아직 못 받았으면
                                // 누를 수 없는 줄로 두는 편이 낫다 — 눌러도 안 열리는 줄은 고장이다.
                                termItem(
                                    iconRes = CoreUiR.drawable.ico_setting_keyhole,
                                    title = "개인정보 처리방침",
                                    document = state.termLinks.privacyPolicy,
                                    onOpenTerm = onOpenTerm,
                                    onRetry = { onIntent(SettingsUiIntent.RefreshTermLinks) },
                                ),
                                termItem(
                                    iconRes = CoreUiR.drawable.ico_setting_note,
                                    title = "서비스 이용약관",
                                    document = state.termLinks.termsOfService,
                                    onOpenTerm = onOpenTerm,
                                    onRetry = { onIntent(SettingsUiIntent.RefreshTermLinks) },
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
                                    isEnabled = !state.isAccountActionInProgress,
                                    showChevron = true,
                                    onClick = { onIntent(SettingsUiIntent.LogoutClicked) },
                                ),
                                SettingsItem(
                                    iconRes = CoreUiR.drawable.ico_setting_trash,
                                    title = "계정 삭제",
                                    isEnabled = !state.isAccountActionInProgress,
                                    showChevron = true,
                                    contentColor = MaterialTheme.colorScheme.error,
                                    onClick = { onIntent(SettingsUiIntent.AccountDeleteClicked) },
                                ),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountSummaryCard(
    provider: SocialLoginProvider?,
    nickname: String?,
) {
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
                    // 닉네임을 못 받았어도 카드가 비지 않도록 제공자 문구로 돌아간다.
                    text = nickname ?: provider.accountTitle,
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
            // 시안에서 제목만 카드보다 12dp 더 들어간다. 카드 안 아이콘 열과 눈으로 맞물린다.
            modifier = Modifier.padding(start = Spacing.medium),
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
            val trailingColor =
                item.trailingColor.takeUnless { it == Color.Unspecified }
                    ?: MaterialTheme.colorScheme.onSurfaceVariant
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (item.showTrailingDot) {
                    Box(
                        modifier =
                            Modifier
                                .size(5.dp)
                                .background(trailingColor, CircleShape),
                    )
                }
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.labelMedium,
                    color = trailingColor,
                )
            }
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

private data class SettingsItem(
    @DrawableRes val iconRes: Int,
    val title: String,
    val trailingText: String? = null,
    /** 후행 문구 색. 지정하지 않으면 보조색이다. 손볼 것이 있는 줄만 올린다. */
    val trailingColor: Color = Color.Unspecified,
    /** 색 말고도 알아볼 수 있게 후행 문구 앞에 점을 찍을지. */
    val showTrailingDot: Boolean = false,
    val contentColor: Color = Color.Unspecified,
    val isEnabled: Boolean = true,
    val showChevron: Boolean = false,
    val onClick: (() -> Unit)? = null,
)

/**
 * 약관 원문으로 가는 줄.
 *
 * 주소를 아직 못 받았을 때 줄을 조용히 잠그지 않는다. 왜 눌리지 않는지 알 수 없는 줄이 되고,
 * 처리방침은 설정에서 볼 수 있어야 하는 문서라 세션 내내 막혀 있으면 안 된다. 대신 무엇이
 * 안 됐는지 적고, 누르면 다시 물어보게 한다.
 */
private fun termItem(
    @DrawableRes iconRes: Int,
    title: String,
    document: TermDocument?,
    onOpenTerm: (TermDocument) -> Unit,
    onRetry: () -> Unit,
) = SettingsItem(
    iconRes = iconRes,
    title = title,
    trailingText = if (document == null) "불러오지 못함" else null,
    showChevron = document != null,
    onClick = if (document == null) onRetry else ({ onOpenTerm(document) }),
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

/**
 * Preview 용 권한 상태. 정상·제한·거부·미지원을 한 화면에 모아 색과 점 배지가 실제로 갈리는지 본다.
 */
private val PreviewDataSourceStatuses =
    mapOf(
        DataPermission.PHOTO to DataSourceStatus.LIMITED,
        DataPermission.CALENDAR to DataSourceStatus.GRANTED,
        DataPermission.LOCATION to DataSourceStatus.DENIED,
        DataPermission.NOTIFICATION_LISTENER to DataSourceStatus.UNSUPPORTED,
        DataPermission.APP_NOTIFICATION to DataSourceStatus.GRANTED,
        DataPermission.HEALTH to DataSourceStatus.GRANTED,
    )

@Preview(name = "Settings Default", apiLevel = 36, showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SettingsDefaultPreview() {
    LaimoryTheme {
        SettingsScreen(
            innerPadding = PaddingValues(),
            appVersionName = "1.0.0",
            state = SettingsUiState(accountProvider = SocialLoginProvider.GOOGLE),
            statusOf = { PreviewDataSourceStatuses.getValue(it) },
            locationStep = LocationPermissionStep.GRANTED,
            onOpenTerm = {},
            onDataSourceClick = {},
            onIntent = {},
        )
    }
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
            statusOf = { PreviewDataSourceStatuses.getValue(it) },
            locationStep = LocationPermissionStep.GRANTED,
            onOpenTerm = {},
            onDataSourceClick = {},
            onIntent = {},
        )
    }
}
