package com.soma369.laimory.feature.home.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.home.component.PastRecordCard
import com.soma369.laimory.feature.home.state.PastRecordMonthGroup
import com.soma369.laimory.feature.home.state.PastRecordsContent
import com.soma369.laimory.feature.home.state.PastRecordsUiIntent
import com.soma369.laimory.feature.home.viewmodel.PastRecordsViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.soma369.laimory.core.ui.R as UiR

@Composable
fun PastRecordsRoute(
    innerPadding: PaddingValues,
    viewModel: PastRecordsViewModel = hiltViewModel(),
) {
    // 기록을 열어 보고 돌아오거나 홈에서 초안을 만든 뒤 다시 들어오면 목록이 달라져 있다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.sendIntent(PastRecordsUiIntent.Sync)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler { viewModel.sendIntent(PastRecordsUiIntent.NavigateBack) }
    PastRecordsScreen(
        innerPadding = innerPadding,
        content = state.content,
        onIntent = viewModel::sendIntent,
    )
}

@Composable
private fun PastRecordsScreen(
    innerPadding: PaddingValues,
    content: PastRecordsContent,
    onIntent: (PastRecordsUiIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
    ) {
        // 시안은 앱바가 아니라 본문 안 헤더 행이다.
        PastRecordsHeader(
            onBackClick = { onIntent(PastRecordsUiIntent.NavigateBack) },
            modifier = Modifier.padding(horizontal = Spacing.extraLarge2, vertical = 0.dp),
        )
        Text(
            text = "기록해 둔 하루를 다시 만나보세요.",
            modifier = Modifier.padding(horizontal = Spacing.extraLarge2),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when (content) {
            PastRecordsContent.Loading -> PastRecordsLoading()
            PastRecordsContent.Empty -> PastRecordsEmpty()
            PastRecordsContent.LoadFailed ->
                PastRecordsLoadFailed(onRetryClick = { onIntent(PastRecordsUiIntent.Sync) })

            is PastRecordsContent.Groups ->
                LazyColumn(
                    contentPadding =
                        PaddingValues(
                            start = Spacing.extraLarge2,
                            end = Spacing.extraLarge2,
                            bottom = Spacing.extraLarge2,
                        ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                ) {
                    content.months.forEach { group ->
                        item(key = "month-${group.yearMonth}") { PastRecordMonthHeader(group = group) }
                        items(items = group.records, key = { it.dailyRecordId }) { record ->
                            PastRecordCard(
                                record = record,
                                onClick = { onIntent(PastRecordsUiIntent.SelectRecord(record.recordDate)) },
                            )
                        }
                    }
                }
        }
    }
}

@Composable
private fun PastRecordsHeader(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(44.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onBackClick,
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(UiR.drawable.ico_default_arrow_left),
                    contentDescription = "뒤로",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Text(
            text = "지난 기록",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PastRecordMonthHeader(group: PastRecordMonthGroup) {
    Row(
        modifier = Modifier.fillMaxWidth().height(44.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = MONTH_FORMAT.format(group.yearMonth),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "${group.records.size}개의 기록",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PastRecordsLoading() {
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
private fun PastRecordsEmpty() {
    Text(
        text = "아직 저장된 기록이 없어요.\n초안을 만들어 하루를 기록해보세요.",
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.extraLarge),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun PastRecordsLoadFailed(onRetryClick: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "지난 기록을 불러오지 못했어요.",
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

private val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREA)

@Preview(name = "지난 기록 - 목록", showBackground = true)
@Composable
private fun PastRecordsPreview() {
    LaimoryTheme {
        PastRecordsScreen(
            innerPadding = PaddingValues(0.dp),
            content = PastRecordsContent.Groups(emptyList()),
            onIntent = {},
        )
    }
}

@Preview(name = "지난 기록 - 비었음", showBackground = true)
@Composable
private fun PastRecordsEmptyPreview() {
    LaimoryTheme {
        PastRecordsScreen(
            innerPadding = PaddingValues(0.dp),
            content = PastRecordsContent.Empty,
            onIntent = {},
        )
    }
}
