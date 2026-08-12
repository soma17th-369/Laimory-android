package com.soma369.laimory.feature.home.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.core.ui.component.LaimoryTopAppBar
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.home.component.DraftConsentTermsSheet
import com.soma369.laimory.feature.home.component.iconRes
import com.soma369.laimory.feature.home.component.label
import com.soma369.laimory.feature.home.component.subtitle
import com.soma369.laimory.feature.home.component.titleRes
import com.soma369.laimory.feature.home.state.DraftConsentTerm
import com.soma369.laimory.feature.home.state.DraftConsentTypeGroup
import com.soma369.laimory.feature.home.state.DraftConsentTypeSummary
import com.soma369.laimory.feature.home.state.DraftConsentUiContent
import com.soma369.laimory.feature.home.state.DraftConsentUiIntent
import com.soma369.laimory.feature.home.state.DraftConsentUiState
import com.soma369.laimory.feature.home.viewmodel.DraftConsentViewModel
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import com.soma369.laimory.core.ui.R as UiR

@Composable
fun DraftConsentRoute(
    innerPadding: PaddingValues,
    viewModel: DraftConsentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DraftConsentContent(
        innerPadding = innerPadding,
        state = state,
        onIntent = viewModel::sendIntent,
        snackbarFlow = viewModel.snackbar,
    )
}

@Composable
private fun DraftConsentContent(
    innerPadding: PaddingValues,
    state: DraftConsentUiState,
    onIntent: (DraftConsentUiIntent) -> Unit,
    snackbarFlow: Flow<String>,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    LaunchedEffect(Unit) {
        snackbarFlow.collect(snackbarHostState::showSnackbar)
    }
    // 시스템 back 도 동일한 폐기 정책을 타도록 인텐트로 수렴한다. 제출 중에는 ViewModel 이 무시한다.
    BackHandler {
        onIntent(DraftConsentUiIntent.NavigateBack)
    }

    DraftConsentScreen(innerPadding = innerPadding, state = state, onIntent = onIntent)

    state.openTermsDetail?.let { term ->
        DraftConsentTermsSheet(
            term = term,
            onDismiss = { onIntent(DraftConsentUiIntent.CloseTermsDetail) },
        )
    }
}

@Composable
private fun DraftConsentScreen(
    innerPadding: PaddingValues,
    state: DraftConsentUiState,
    onIntent: (DraftConsentUiIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
    ) {
        LaimoryTopAppBar(
            title = { Text("데이터 수집 동의") },
            onBackClick = { onIntent(DraftConsentUiIntent.NavigateBack) },
        )
        val content = state.content
        if (content == null) {
            MissingPreparationContent(onNavigateHome = { onIntent(DraftConsentUiIntent.NavigateBack) })
            return
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.extraLarge)
                    .padding(top = Spacing.medium, bottom = Spacing.extraLarge),
        ) {
            ConsentHeader(content = content, includedTotal = state.includedTotal)
            Spacer(modifier = Modifier.height(Spacing.extraLarge2))

            Column(verticalArrangement = Arrangement.spacedBy(TYPE_CARD_GAP)) {
                content.typeSummaries.forEach { summary ->
                    TypeSummaryRow(
                        summary = summary,
                        countText =
                            summary.countLabel(
                                includedCount = summary.sentCount - state.excludedCountOf(summary.group),
                            ),
                        onClick = { onIntent(DraftConsentUiIntent.OpenTypeDetail(summary.group)) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.extraLarge2))

            Text(
                text = "동의 항목",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(Spacing.medium))
            Column(verticalArrangement = Arrangement.spacedBy(TYPE_CARD_GAP)) {
                DraftConsentTerm.entries.forEach { term ->
                    TermRow(
                        term = term,
                        checked = term in state.checkedTerms,
                        enabled = !state.isSubmitting,
                        onToggle = { onIntent(DraftConsentUiIntent.ToggleTerm(term)) },
                        onOpenDetail = { onIntent(DraftConsentUiIntent.OpenTermsDetail(term)) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.large))

            Text(
                text = "알림·사진에는 다른 사람의 메시지, 얼굴 등 제3자의 개인정보가 포함될 수 있어요. 전송 전 상세 내용을 확인해주세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SubmitArea(state = state, onIntent = onIntent)
    }
}

@Composable
private fun ConsentHeader(
    content: DraftConsentUiContent,
    includedTotal: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Text(
            text = "타임라인 생성을 위해\n아래 데이터가 활용됩니다",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "안전하게 암호화된 연결로 전송된 데이터를 기반으로 일상의 순간을 타임라인으로 기록합니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "선택 구간 · ${content.windowText} (총 ${includedTotal}건 전송)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TypeSummaryRow(
    summary: DraftConsentTypeSummary,
    countText: String,
    onClick: () -> Unit,
) {
    val titleColor =
        if (summary.isSent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        enabled = summary.isSent,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(Spacing.medium)
                    .heightIn(min = TYPE_ROW_MIN_HEIGHT),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(TYPE_ICON_CONTAINER_SIZE)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(summary.group.iconRes()),
                    contentDescription = null,
                    modifier = Modifier.size(TYPE_ICON_SIZE),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = summary.group.label(),
                    style = MaterialTheme.typography.titleSmall,
                    color = titleColor,
                )
                Text(
                    text = countText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (summary.isSent) {
                Icon(
                    painter = painterResource(UiR.drawable.ico_default_caret_right),
                    contentDescription = "${summary.group.label()} 전송 상세 보기",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TermRow(
    term: DraftConsentTerm,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier =
                Modifier
                    .heightIn(min = TERM_ROW_MIN_HEIGHT)
                    .toggleable(
                        value = checked,
                        enabled = enabled,
                        role = Role.Checkbox,
                        onValueChange = { onToggle() },
                    ).padding(start = Spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.small + 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConsentCheckCircle(checked = checked)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "[필수] ${stringResource(term.titleRes())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = term.subtitle(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onOpenDetail) {
                Icon(
                    painter = painterResource(UiR.drawable.ico_default_caret_right),
                    contentDescription = "${stringResource(term.titleRes())} 상세 보기",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ConsentCheckCircle(checked: Boolean) {
    val background = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val checkColor = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline
    Box(
        modifier =
            Modifier
                .size(CHECK_CIRCLE_SIZE)
                .background(background, CircleShape)
                .border(
                    width = if (checked) 0.dp else 1.5.dp,
                    color = if (checked) background else MaterialTheme.colorScheme.outline,
                    shape = CircleShape,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "✓",
            style = MaterialTheme.typography.labelSmall,
            color = checkColor,
        )
    }
}

@Composable
private fun SubmitArea(
    state: DraftConsentUiState,
    onIntent: (DraftConsentUiIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.extraLarge, vertical = Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        if (!state.isSubmissionAllowed) {
            Text(
                text = "동의 문구의 법무 검토가 완료되기 전까지는 초안 생성을 시작할 수 없어요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.submitError?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = { onIntent(DraftConsentUiIntent.Submit) },
            enabled = state.canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text =
                    when {
                        state.isSubmitting -> "초안 생성 중…"
                        state.submitError != null -> "다시 시도"
                        else -> "모두 동의 후 시작하기"
                    },
            )
        }
    }
}

@Composable
private fun MissingPreparationContent(onNavigateHome: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(Spacing.extraLarge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "전송할 데이터를 다시 준비해야 해요",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "앱이 다시 시작되어 준비한 내용이 사라졌어요.\n홈에서 초안 만들기를 다시 시작해주세요.",
            modifier = Modifier.padding(top = Spacing.small),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onNavigateHome,
            modifier = Modifier.padding(top = Spacing.extraLarge),
        ) {
            Text("홈으로 돌아가기")
        }
    }
}

private val TYPE_CARD_GAP = 6.dp
private val TYPE_ROW_MIN_HEIGHT = 32.dp
private val TYPE_ICON_CONTAINER_SIZE = 32.dp
private val TYPE_ICON_SIZE = 20.dp
private val TERM_ROW_MIN_HEIGHT = 48.dp
private val CHECK_CIRCLE_SIZE = 20.dp

internal fun previewConsentContent(): DraftConsentUiContent =
    DraftConsentUiContent(
        attemptId = 1L,
        recordDate = LocalDate.of(2026, 8, 11),
        windowText = "8월 11일 00:00 ~ 8월 12일 00:00",
        sentTotal = 156,
        typeSummaries =
            listOf(
                DraftConsentTypeSummary(DraftConsentTypeGroup.PHOTO, 6, 6, emptyList()),
                DraftConsentTypeSummary(DraftConsentTypeGroup.CALENDAR, 3, 3, emptyList()),
                DraftConsentTypeSummary(DraftConsentTypeGroup.LOCATION, 12, 12, emptyList()),
                DraftConsentTypeSummary(DraftConsentTypeGroup.HEALTH, 0, 0, emptyList()),
                DraftConsentTypeSummary(DraftConsentTypeGroup.NOTIFICATION, 133, 100, emptyList()),
            ),
    )

@Preview(name = "DraftConsent / Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun DraftConsentScreenPreview() {
    LaimoryTheme {
        DraftConsentScreen(
            innerPadding = PaddingValues(),
            state =
                DraftConsentUiState(
                    content = previewConsentContent(),
                    checkedTerms = setOf(DraftConsentTerm.SENSITIVE_INFO),
                ),
            onIntent = {},
        )
    }
}

@Preview(name = "DraftConsent / Dark", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun DraftConsentScreenDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        DraftConsentScreen(
            innerPadding = PaddingValues(),
            state =
                DraftConsentUiState(
                    content = previewConsentContent(),
                    checkedTerms = DraftConsentTerm.entries.toSet(),
                ),
            onIntent = {},
        )
    }
}

@Preview(name = "DraftConsent / 준비물 없음", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun DraftConsentMissingPreparationPreview() {
    LaimoryTheme {
        DraftConsentScreen(
            innerPadding = PaddingValues(),
            state = DraftConsentUiState(),
            onIntent = {},
        )
    }
}
