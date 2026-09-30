package com.soma369.laimory.feature.settings.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.domain.model.inquiry.InquiryStatus
import com.soma369.laimory.core.domain.model.inquiry.InquirySummary
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.settings.component.InquiryStatusChip
import com.soma369.laimory.feature.settings.state.InquiriesUiIntent
import com.soma369.laimory.feature.settings.state.InquiriesUiSideEffect
import com.soma369.laimory.feature.settings.state.InquiriesUiState
import com.soma369.laimory.feature.settings.state.InquiryListContent
import com.soma369.laimory.feature.settings.viewmodel.InquiriesViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 문의 화면의 `문의 내역` 탭. 앱바·뒤로가기는 문의 화면이 갖는다.
 *
 * 처리 상태는 관리자가 바꾸는 값이라, 탭이 보일 때마다(탭 전환·상세에서 복귀·문의를 보낸 뒤) 새로 받는다.
 * 탭이 새로 그려지면 이미 켜져 있는 화면에서도 ON_RESUME 이 한 번 온다.
 */
@Composable
internal fun InquiryHistoryTab(
    viewModel: InquiriesViewModel,
    modifier: Modifier = Modifier,
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.sendIntent(InquiriesUiIntent.Sync)
    }
    val snackbarHostState = LocalSnackbarHostState.current
    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is InquiriesUiSideEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    InquiryHistoryContent(
        state = state,
        onIntent = viewModel::sendIntent,
        modifier = modifier,
    )
}

@Composable
private fun InquiryHistoryContent(
    state: InquiriesUiState,
    onIntent: (InquiriesUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        when (val content = state.content) {
            InquiryListContent.Loading -> InquiriesLoading()
            InquiryListContent.Empty ->
                InquiriesMessage(text = "아직 보낸 문의가 없어요.\n궁금한 점은 문의하기 탭에서 남겨 주세요.")
            InquiryListContent.LoadFailed -> InquiriesLoadFailed(onRetryClick = { onIntent(InquiriesUiIntent.Sync) })
            is InquiryListContent.Items ->
                LazyColumn(contentPadding = PaddingValues(bottom = LIST_BOTTOM_PADDING)) {
                    itemsIndexed(items = content.inquiries, key = { _, inquiry -> inquiry.id }) { index, inquiry ->
                        InquiryRow(
                            inquiry = inquiry,
                            onClick = { onIntent(InquiriesUiIntent.InquiryClicked(inquiry.id)) },
                        )
                        if (index != content.inquiries.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = ROW_HORIZONTAL_PADDING),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
        }
    }
}

@Composable
private fun InquiryRow(
    inquiry: InquirySummary,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = ROW_HORIZONTAL_PADDING, vertical = Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        Text(
            text = inquiry.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InquiryStatusChip(status = inquiry.status)
            Text(
                text = CREATED_DATE_FORMAT.format(inquiry.createdAt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InquiriesLoading() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.extraLarge2),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun InquiriesMessage(text: String) {
    Text(
        text = text,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ROW_HORIZONTAL_PADDING, vertical = Spacing.extraLarge2),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun InquiriesLoadFailed(onRetryClick: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.extraLarge2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "문의 내역을 불러오지 못했어요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(
            onClick = onRetryClick,
            modifier = Modifier.padding(top = Spacing.medium),
        ) {
            Text("다시 시도")
        }
    }
}

private val CREATED_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

private val ROW_HORIZONTAL_PADDING = 24.dp
private val LIST_BOTTOM_PADDING = 24.dp

@Preview(name = "문의 내역 - 목록", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun InquiriesPreview() {
    LaimoryTheme {
        InquiryHistoryContent(
            state =
                InquiriesUiState(
                    content =
                        InquiryListContent.Items(
                            listOf(
                                InquirySummary(2, "사진이 안 올라가요", InquiryStatus.RECEIVED, LocalDateTime.of(2026, 9, 29, 10, 0), null),
                                InquirySummary(
                                    1,
                                    "타임라인이 비어 있어요 — 어제 기록을 만들었는데 아무 이벤트도 보이지 않아요",
                                    InquiryStatus.ANSWERED,
                                    LocalDateTime.of(2026, 9, 20, 9, 0),
                                    LocalDateTime.of(2026, 9, 21, 14, 0),
                                ),
                            ),
                        ),
                ),
            onIntent = {},
        )
    }
}

@Preview(name = "문의 내역 - 비었음", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun InquiriesEmptyPreview() {
    LaimoryTheme(darkTheme = true) {
        InquiryHistoryContent(state = InquiriesUiState(InquiryListContent.Empty), onIntent = {})
    }
}
