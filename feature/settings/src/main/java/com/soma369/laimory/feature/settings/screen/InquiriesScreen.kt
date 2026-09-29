package com.soma369.laimory.feature.settings.screen

import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.domain.model.inquiry.InquiryStatus
import com.soma369.laimory.core.domain.model.inquiry.InquirySummary
import com.soma369.laimory.core.ui.component.LaimoryTopAppBar
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.settings.component.InquiryStatusChip
import com.soma369.laimory.feature.settings.state.InquiriesUiIntent
import com.soma369.laimory.feature.settings.state.InquiriesUiState
import com.soma369.laimory.feature.settings.state.InquiryListContent
import com.soma369.laimory.feature.settings.viewmodel.InquiriesViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun InquiriesRoute(
    innerPadding: PaddingValues,
    viewModel: InquiriesViewModel = hiltViewModel(),
) {
    // 처리 상태는 관리자가 바꾸는 값이다. 상세를 보고 돌아오거나 문의를 보낸 뒤에도 새로 받는다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.sendIntent(InquiriesUiIntent.Sync)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    InquiriesScreen(
        innerPadding = innerPadding,
        state = state,
        onIntent = viewModel::sendIntent,
    )
}

@Composable
private fun InquiriesScreen(
    innerPadding: PaddingValues,
    state: InquiriesUiState,
    onIntent: (InquiriesUiIntent) -> Unit,
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
                    text = "문의 내역",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            onBackClick = { onIntent(InquiriesUiIntent.NavigateBack) },
        )
        when (val content = state.content) {
            InquiryListContent.Loading -> InquiriesLoading()
            InquiryListContent.Empty ->
                InquiriesMessage(text = "아직 보낸 문의가 없어요.\n궁금한 점은 설정 > 문의하기에서 남겨 주세요.")
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
        InquiriesScreen(
            innerPadding = PaddingValues(),
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
        InquiriesScreen(innerPadding = PaddingValues(), state = InquiriesUiState(InquiryListContent.Empty), onIntent = {})
    }
}
