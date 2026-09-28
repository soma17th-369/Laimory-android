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
import androidx.compose.runtime.LaunchedEffect
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
import com.soma369.laimory.core.domain.model.notice.Notice
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.core.ui.component.LaimoryTopAppBar
import com.soma369.laimory.core.ui.terms.rememberTermContentLauncher
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.settings.component.NewNoticeDot
import com.soma369.laimory.feature.settings.state.NoticeListContent
import com.soma369.laimory.feature.settings.state.NoticesUiIntent
import com.soma369.laimory.feature.settings.state.NoticesUiSideEffect
import com.soma369.laimory.feature.settings.state.NoticesUiState
import com.soma369.laimory.feature.settings.viewmodel.NoticesViewModel
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun NoticesRoute(
    innerPadding: PaddingValues,
    viewModel: NoticesViewModel = hiltViewModel(),
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.sendIntent(NoticesUiIntent.Sync)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    NoticesContent(
        innerPadding = innerPadding,
        state = state,
        onIntent = viewModel::sendIntent,
        sideEffectFlow = viewModel.sideEffect,
    )
}

@Composable
private fun NoticesContent(
    innerPadding: PaddingValues,
    state: NoticesUiState,
    onIntent: (NoticesUiIntent) -> Unit,
    sideEffectFlow: Flow<NoticesUiSideEffect>,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    // 원문은 약관과 같은 방식으로 연다 — 게시된 정적 페이지라 앱 안 WebView 가 필요 없다.
    val contentLauncher = rememberTermContentLauncher()
    LaunchedEffect(sideEffectFlow) {
        sideEffectFlow.collect { effect ->
            when (effect) {
                is NoticesUiSideEffect.OpenContent ->
                    if (!contentLauncher.open(effect.url)) {
                        snackbarHostState.showSnackbar("공지를 열 브라우저가 없어요.")
                    }
            }
        }
    }
    NoticesScreen(
        innerPadding = innerPadding,
        state = state,
        onIntent = onIntent,
    )
}

@Composable
private fun NoticesScreen(
    innerPadding: PaddingValues,
    state: NoticesUiState,
    onIntent: (NoticesUiIntent) -> Unit,
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
                    text = "공지사항",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            onBackClick = { onIntent(NoticesUiIntent.NavigateBack) },
        )
        when (val content = state.content) {
            NoticeListContent.Loading -> NoticesLoading()
            NoticeListContent.Empty -> NoticesMessage(text = "아직 올라온 공지가 없어요.")
            NoticeListContent.LoadFailed -> NoticesLoadFailed(onRetryClick = { onIntent(NoticesUiIntent.Sync) })
            is NoticeListContent.Items ->
                LazyColumn(contentPadding = PaddingValues(bottom = LIST_BOTTOM_PADDING)) {
                    itemsIndexed(items = content.notices, key = { _, notice -> notice.id }) { index, notice ->
                        NoticeRow(
                            notice = notice,
                            isNew = notice.id in content.newIds,
                            onClick = { onIntent(NoticesUiIntent.NoticeClicked(notice)) },
                        )
                        if (index != content.notices.lastIndex) {
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
private fun NoticeRow(
    notice: Notice,
    isNew: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = ROW_HORIZONTAL_PADDING, vertical = Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
    ) {
        // 제목은 한 줄로 통일한다 — 전문은 원문 페이지가 보여 준다. 점은 설정의 `공지사항` 줄과
        // 같이 글자 바로 옆에 둔다. 오른쪽 끝은 설정에서 상태 값 자리라 뜻이 섞인다.
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f, fill = false),
                text = notice.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isNew) NewNoticeDot()
        }
        Text(
            text = PUBLISHED_DATE_FORMAT.format(notice.publishedAt),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoticesLoading() {
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
private fun NoticesMessage(text: String) {
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
private fun NoticesLoadFailed(onRetryClick: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.extraLarge2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "공지를 불러오지 못했어요.",
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

private val PUBLISHED_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

private val ROW_HORIZONTAL_PADDING = 24.dp
private val LIST_BOTTOM_PADDING = 24.dp

@Preview(name = "공지 - 목록", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun NoticesPreview() {
    LaimoryTheme {
        NoticesScreen(
            innerPadding = PaddingValues(),
            state =
                NoticesUiState(
                    content =
                        NoticeListContent.Items(
                            newIds = setOf(2),
                            notices =
                                listOf(
                                    Notice(
                                        id = 2,
                                        title = "개인정보 처리방침 개정 안내",
                                        contentUrl = "https://www.laimory.app/notices/2",
                                        publishedAt = LocalDateTime.of(2026, 9, 28, 10, 0),
                                    ),
                                    Notice(
                                        id = 1,
                                        title = "서비스 점검 안내 — 9월 30일 새벽 2시부터 4시까지 일부 기능을 쓸 수 없어요",
                                        contentUrl = "https://www.laimory.app/notices/1",
                                        publishedAt = LocalDateTime.of(2026, 9, 24, 10, 0),
                                    ),
                                ),
                        ),
                ),
            onIntent = {},
        )
    }
}

@Preview(name = "공지 - 비었음", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun NoticesEmptyPreview() {
    LaimoryTheme {
        NoticesScreen(
            innerPadding = PaddingValues(),
            state = NoticesUiState(content = NoticeListContent.Empty),
            onIntent = {},
        )
    }
}

@Preview(name = "공지 - 실패", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun NoticesLoadFailedPreview() {
    LaimoryTheme(darkTheme = true) {
        NoticesScreen(
            innerPadding = PaddingValues(),
            state = NoticesUiState(content = NoticeListContent.LoadFailed),
            onIntent = {},
        )
    }
}
