package com.soma369.laimory.feature.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.soma369.laimory.core.domain.model.inquiry.InquiryDetail
import com.soma369.laimory.core.domain.model.inquiry.InquiryStatus
import com.soma369.laimory.core.ui.component.LaimoryTopAppBar
import com.soma369.laimory.core.ui.component.photo.LaimoryPhotoTile
import com.soma369.laimory.core.ui.component.photo.LaimoryPhotoViewerDialog
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.settings.component.InquiryStatusChip
import com.soma369.laimory.feature.settings.state.InquiryDetailContent
import com.soma369.laimory.feature.settings.state.InquiryDetailUiIntent
import com.soma369.laimory.feature.settings.state.InquiryDetailUiState
import com.soma369.laimory.feature.settings.viewmodel.InquiryDetailViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun InquiryDetailRoute(
    innerPadding: PaddingValues,
    inquiryId: Long?,
    viewModel: InquiryDetailViewModel = hiltViewModel(),
) {
    // ViewModel 이 Activity 수명이라 새로 들어올 때마다 알려 줘야 이전 내용(다른 문의·다른 계정)을 지운다.
    // 저장 상태에 남기므로 회전으로는 다시 비우지 않는다.
    var openedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var opened by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(inquiryId) {
        if (!opened || openedId != inquiryId) {
            opened = true
            openedId = inquiryId
            viewModel.sendIntent(InquiryDetailUiIntent.Opened(inquiryId))
        } else {
            viewModel.sendIntent(InquiryDetailUiIntent.Load(inquiryId))
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    InquiryDetailScreen(
        innerPadding = innerPadding,
        state = state,
        onIntent = viewModel::sendIntent,
    )
}

@Composable
private fun InquiryDetailScreen(
    innerPadding: PaddingValues,
    state: InquiryDetailUiState,
    onIntent: (InquiryDetailUiIntent) -> Unit,
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
            onBackClick = { onIntent(InquiryDetailUiIntent.NavigateBack) },
        )
        when (val content = state.content) {
            InquiryDetailContent.Loading -> DetailLoading()
            InquiryDetailContent.NotFound -> DetailMessage(text = "찾을 수 없는 문의예요.\n이미 지워졌거나 볼 수 없는 문의일 수 있어요.")
            InquiryDetailContent.LoadFailed -> DetailLoadFailed(onRetryClick = { onIntent(InquiryDetailUiIntent.Retry) })
            is InquiryDetailContent.Loaded -> InquiryDetailBody(detail = content.detail)
        }
    }
}

@Composable
private fun InquiryDetailBody(detail: InquiryDetail) {
    var viewerIndex by rememberSaveable(detail.id) { mutableStateOf<Int?>(null) }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SCREEN_HORIZONTAL_PADDING, vertical = Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            InquiryStatusChip(status = detail.status)
            Text(
                text = detail.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text =
                    buildString {
                        append("접수 ${DATE_TIME_FORMAT.format(detail.createdAt)}")
                        detail.answeredAt?.let { append(" · 답변 완료 ${DATE_TIME_FORMAT.format(it)}") }
                    },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusNotice(status = detail.status)
        DetailSection(title = "답장 받을 이메일") {
            Text(
                text = detail.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        DetailSection(title = "내용") {
            Text(
                text = detail.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (detail.attachmentUrls.isNotEmpty()) {
            DetailSection(title = "첨부한 사진") {
                AttachmentRow(urls = detail.attachmentUrls, onOpen = { viewerIndex = it })
            }
        }
    }
    viewerIndex?.let { index ->
        LaimoryPhotoViewerDialog(
            photoCount = detail.attachmentUrls.size,
            initialIndex = index,
            onDismiss = { viewerIndex = null },
            photo = { page ->
                AsyncImage(
                    model = detail.attachmentUrls[page],
                    contentDescription = "첨부한 사진 ${page + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            },
        )
    }
}

/**
 * 답변은 앱에 오지 않는다 — 무엇을 확인하면 되는지 상태마다 한 줄로 말한다.
 */
@Composable
private fun StatusNotice(status: InquiryStatus) {
    Text(
        text =
            when (status) {
                InquiryStatus.RECEIVED -> "확인하고 있어요. 답변은 아래 이메일로 보내드려요."
                InquiryStatus.ANSWERED -> "아래 이메일로 답변을 보냈어요. 메일함을 확인해 주세요."
            },
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                .padding(Spacing.large),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun AttachmentRow(
    urls: List<String>,
    onOpen: (Int) -> Unit,
) {
    val isPreview = LocalInspectionMode.current
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
        urls.forEachIndexed { index, url ->
            LaimoryPhotoTile(
                onClick = { onOpen(index) },
                onClickLabel = "크게 보기",
            ) {
                // 미리보기는 실제 사진을 불러올 수 없다.
                if (!isPreview) {
                    AsyncImage(
                        model = url,
                        contentDescription = "첨부한 사진 ${index + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailLoading() {
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
private fun DetailMessage(text: String) {
    Text(
        text = text,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = SCREEN_HORIZONTAL_PADDING, vertical = Spacing.extraLarge2),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun DetailLoadFailed(onRetryClick: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.extraLarge2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "문의를 불러오지 못했어요.",
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

private val DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

private val SCREEN_HORIZONTAL_PADDING = 24.dp

@Preview(name = "문의 상세 - 답변 완료", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun InquiryDetailPreview() {
    LaimoryTheme {
        InquiryDetailScreen(
            innerPadding = PaddingValues(),
            state =
                InquiryDetailUiState(
                    inquiryId = 1,
                    content =
                        InquiryDetailContent.Loaded(
                            InquiryDetail(
                                id = 1,
                                title = "사진이 안 올라가요",
                                status = InquiryStatus.ANSWERED,
                                email = "user@example.com",
                                description = "타임라인을 만들 때 사진이 빠져요.\n어제도 같은 일이 있었어요.",
                                attachmentUrls = listOf("https://cdn/a.jpg", "https://cdn/b.jpg"),
                                createdAt = LocalDateTime.of(2026, 9, 29, 10, 0),
                                answeredAt = LocalDateTime.of(2026, 9, 30, 14, 0),
                            ),
                        ),
                ),
            onIntent = {},
        )
    }
}

@Preview(name = "문의 상세 - 찾을 수 없음", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun InquiryDetailNotFoundPreview() {
    LaimoryTheme(darkTheme = true) {
        InquiryDetailScreen(
            innerPadding = PaddingValues(),
            state = InquiryDetailUiState(inquiryId = 1, content = InquiryDetailContent.NotFound),
            onIntent = {},
        )
    }
}
