package com.soma369.laimory.feature.home.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.core.ui.component.LaimoryTopAppBar
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.home.component.DraftConsentTermsSheet
import com.soma369.laimory.feature.home.component.DraftConsentTypeDetailSheet
import com.soma369.laimory.feature.home.component.label
import com.soma369.laimory.feature.home.component.sentFieldsLabel
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
import java.time.format.DateTimeFormatter
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

    state.openTypeDetail?.let { group ->
        state.content
            ?.typeSummaries
            ?.firstOrNull { it.group == group }
            ?.let { summary ->
                DraftConsentTypeDetailSheet(
                    summary = summary,
                    onDismiss = { onIntent(DraftConsentUiIntent.CloseTypeDetail) },
                )
            }
    }

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
            title = { Text("데이터 전송 동의") },
            onBackClick = { onIntent(DraftConsentUiIntent.NavigateBack) },
        )
        val content = state.content
        if (content == null) {
            MissingPreparationContent(onNavigateHome = { onIntent(DraftConsentUiIntent.NavigateBack) })
            return
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = Spacing.extraLarge, vertical = Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            item(key = "header") { ConsentHeader(content) }

            items(items = content.typeSummaries, key = { it.group.name }) { summary ->
                TypeSummaryRow(
                    summary = summary,
                    photoPreviewUris =
                        if (summary.group == DraftConsentTypeGroup.PHOTO) content.photoPreviewUris else emptyList(),
                    onClick = { onIntent(DraftConsentUiIntent.OpenTypeDetail(summary.group)) },
                )
            }

            item(key = "thirdPartyNotice") {
                Text(
                    text = "알림·사진에는 다른 사람의 메시지, 얼굴 등 제3자의 개인정보가 포함될 수 있어요. 전송 전 상세 내용을 확인해주세요.",
                    modifier = Modifier.padding(top = Spacing.small),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item(key = "termsHeader") {
                Text(
                    text = "필수 동의 항목",
                    modifier = Modifier.padding(top = Spacing.large),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            items(items = DraftConsentTerm.entries, key = DraftConsentTerm::name) { term ->
                TermRow(
                    term = term,
                    checked = term in state.checkedTerms,
                    enabled = !state.isSubmitting,
                    onToggle = { onIntent(DraftConsentUiIntent.ToggleTerm(term)) },
                    onOpenDetail = { onIntent(DraftConsentUiIntent.OpenTermsDetail(term)) },
                )
            }
        }

        SubmitArea(state = state, onIntent = onIntent)
    }
}

@Composable
private fun ConsentHeader(content: DraftConsentUiContent) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
        Text(
            text = content.recordDate.format(CONSENT_RECORD_DATE_FORMAT),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "선택 구간 ${content.windowText}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "아래 ${content.sentTotal}건의 데이터를 서버로 전송해 AI 타임라인 초안을 만들어요. 전송 내용을 확인하고 동의해주세요.",
            modifier = Modifier.padding(top = Spacing.small),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TypeSummaryRow(
    summary: DraftConsentTypeSummary,
    photoPreviewUris: List<String>,
    onClick: () -> Unit,
) {
    val contentColor =
        if (summary.isSent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        enabled = summary.isSent,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.large, vertical = Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = TYPE_ROW_MIN_HEIGHT),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                ) {
                    Text(
                        text = summary.group.label(),
                        style = MaterialTheme.typography.titleSmall,
                        color = contentColor,
                    )
                    Text(
                        text = summary.group.sentFieldsLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = summary.countLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                )
                if (summary.isSent) {
                    Icon(
                        painter = painterResource(UiR.drawable.ico_default_caret_right),
                        contentDescription = "${summary.group.label()} 전송 상세 보기",
                        modifier =
                            Modifier
                                .padding(start = Spacing.extraSmall)
                                .size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (photoPreviewUris.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    photoPreviewUris.forEach { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = "전송할 사진 미리보기",
                            modifier =
                                Modifier
                                    .size(PHOTO_PREVIEW_SIZE)
                                    .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
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
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = TERM_ROW_MIN_HEIGHT)
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = { onToggle() },
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
        )
        Text(
            text = "${stringResource(term.titleRes())} (필수)",
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = Spacing.small),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        TextButton(onClick = onOpenDetail) {
            Text("보기")
        }
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
                        else -> "동의하고 초안 만들기"
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

private val CONSENT_RECORD_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
private val TYPE_ROW_MIN_HEIGHT = 48.dp
private val TERM_ROW_MIN_HEIGHT = 48.dp
private val PHOTO_PREVIEW_SIZE = 56.dp

private fun previewContent(): DraftConsentUiContent =
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
                DraftConsentTypeSummary(DraftConsentTypeGroup.HEALTH, 2, 2, emptyList()),
                DraftConsentTypeSummary(DraftConsentTypeGroup.NOTIFICATION, 133, 100, emptyList()),
            ),
        photoPreviewUris = emptyList(),
    )

@Preview(name = "DraftConsent / Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun DraftConsentScreenPreview() {
    LaimoryTheme {
        DraftConsentScreen(
            innerPadding = PaddingValues(),
            state =
                DraftConsentUiState(
                    content = previewContent(),
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
                    content = previewContent(),
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
