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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.core.ui.component.LaimoryTopAppBar
import com.soma369.laimory.core.ui.terms.rememberTermContentLauncher
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.home.component.iconRes
import com.soma369.laimory.feature.home.component.label
import com.soma369.laimory.feature.home.state.ConsentLocationMarker
import com.soma369.laimory.feature.home.state.DraftConsentTypeGroup
import com.soma369.laimory.feature.home.state.DraftConsentTypeSummary
import com.soma369.laimory.feature.home.state.DraftConsentUiContent
import com.soma369.laimory.feature.home.state.DraftConsentUiIntent
import com.soma369.laimory.feature.home.state.DraftConsentUiState
import com.soma369.laimory.feature.home.viewmodel.DraftConsentViewModel
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
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

    // 원문은 게시된 HTML 이 정본이라 앱 안에 시트를 두지 않는다. 시트에 복사해 두면 실제
    // 동의한 내용과 화면이 갈리고, 개정될 때마다 앱을 새로 배포해야 한다.
    val termContentLauncher = rememberTermContentLauncher()

    DraftConsentScreen(
        innerPadding = innerPadding,
        state = state,
        onOpenTerm = { document -> termContentLauncher.open(document.contentUrl) },
        onIntent = onIntent,
    )
}

@Composable
private fun DraftConsentScreen(
    innerPadding: PaddingValues,
    state: DraftConsentUiState,
    onOpenTerm: (TermDocument) -> Unit,
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

            // 받을 것도 확인한 것도 없으면 제목만 남아 빈 자리가 된다.
            if (state.pendingTerms.isNotEmpty() || state.agreedTerms.isNotEmpty()) {
                Text(
                    text = "동의 항목",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(Spacing.medium))
                Column(verticalArrangement = Arrangement.spacedBy(TYPE_CARD_GAP)) {
                    state.pendingTerms.forEach { document ->
                        TermRow(
                            document = document,
                            checked = document.termType in state.checkedTerms,
                            enabled = !state.isSubmitting,
                            onToggle = { onIntent(DraftConsentUiIntent.ToggleTerm(document.termType)) },
                            onOpenDetail = { onOpenTerm(document) },
                        )
                    }
                    // 이미 동의한 것은 확인 대상이 아니다. 해제할 수 있게 두면 철회처럼 보이는데,
                    // 서버에는 철회 API 가 없어 실제로는 아무것도 되돌아가지 않는다.
                    state.agreedTerms.forEach { document ->
                        AgreedTermRow(document = document, onOpenDetail = { onOpenTerm(document) })
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.large))
            }

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
    document: TermDocument,
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
                    // 이름은 서버가 준 제목을 그대로 쓴다. 앱이 따로 들고 있으면 실제 동의한
                    // 문서와 화면의 이름이 갈린다.
                    text = "[필수] ${document.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = onOpenDetail) {
                Icon(
                    painter = painterResource(UiR.drawable.ico_default_caret_right),
                    contentDescription = "${document.title} 전문 보기",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * 이미 동의한 항목.
 *
 * 체크박스를 두지 않는다 — 해제할 수 있게 보이면 철회로 읽히는데, 서버에는 철회 API 가 없어
 * 실제로는 아무것도 되돌아가지 않는다. 원문을 다시 열어 볼 길만 남긴다.
 */
@Composable
private fun AgreedTermRow(
    document: TermDocument,
    onOpenDetail: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.heightIn(min = TERM_ROW_MIN_HEIGHT).padding(start = Spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.small + 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = "${document.title} · 동의함",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = onOpenDetail) {
                Icon(
                    painter = painterResource(UiR.drawable.ico_default_caret_right),
                    contentDescription = "${document.title} 전문 보기",
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
                        // 받을 동의가 없으면 확인 문구가 아니라 하려는 일을 말한다.
                        state.pendingTerms.isEmpty() -> "타임라인 만들기"
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
        locationMarkers = previewLocationMarkers(),
    )

internal fun previewLocationMarkers(): List<ConsentLocationMarker> =
    listOf(
        ConsentLocationMarker(
            key = "stay-1",
            sourceRawId = "stay-1",
            order = 1,
            kind = ConsentLocationMarker.Kind.STAY,
            latitude = 37.5665,
            longitude = 126.9780,
            title = "서울특별시 중구 세종대로 110",
            snippet = "8월 11일 09:10 ~ 12:40",
        ),
        ConsentLocationMarker(
            key = "move-1:start",
            sourceRawId = "move-1",
            order = 2,
            kind = ConsentLocationMarker.Kind.MOVEMENT_START,
            latitude = 37.5701,
            longitude = 126.9820,
            title = "서울특별시 종로구 종로 1",
            snippet = "이동 시작 · 8월 11일 12:40 ~ 13:05",
        ),
        ConsentLocationMarker(
            key = "move-1:end",
            sourceRawId = "move-1",
            order = 3,
            kind = ConsentLocationMarker.Kind.MOVEMENT_END,
            latitude = 37.5512,
            longitude = 126.9882,
            title = "서울특별시 중구 남대문로 81",
            snippet = "이동 도착 · 8월 11일 12:40 ~ 13:05",
        ),
    )

private fun previewPendingTerms() =
    listOf(
        TermType.SENSITIVE_INFORMATION_CONSENT to "민감정보 처리 동의",
        TermType.THIRD_PARTY_PROVISION_CONSENT to "개인정보 제3자 제공 동의",
        TermType.CROSS_BORDER_TRANSFER_CONSENT to "개인정보 국외 이전 동의",
    ).map { (type, title) ->
        TermDocument(
            termType = type,
            version = "1.0",
            title = title,
            contentUrl = "https://laimory.app/terms/preview/1.0",
            effectiveAt = LocalDateTime.of(2026, 8, 28, 0, 0),
        )
    }

@Preview(name = "DraftConsent / Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun DraftConsentScreenPreview() {
    LaimoryTheme {
        DraftConsentScreen(
            innerPadding = PaddingValues(),
            state =
                DraftConsentUiState(
                    content = previewConsentContent(),
                    pendingTerms = previewPendingTerms(),
                    checkedTerms = setOf(TermType.SENSITIVE_INFORMATION_CONSENT),
                ),
            onOpenTerm = {},
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
                    pendingTerms = previewPendingTerms().take(1),
                    agreedTerms = previewPendingTerms().drop(1),
                    checkedTerms = setOf(TermType.SENSITIVE_INFORMATION_CONSENT),
                ),
            onOpenTerm = {},
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
            onOpenTerm = {},
            onIntent = {},
        )
    }
}
