package com.soma369.laimory.feature.home.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.ui.component.LaimoryTopAppBar
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.home.component.DraftLoadingPhotoPager
import com.soma369.laimory.feature.home.component.DraftLoadingProgressCard
import com.soma369.laimory.feature.home.component.DraftLoadingProgressRow
import com.soma369.laimory.feature.home.loading.DraftLoadingStage
import com.soma369.laimory.feature.home.loading.DraftLoadingStageState
import com.soma369.laimory.feature.home.loading.DraftLoadingUiIntent
import com.soma369.laimory.feature.home.loading.DraftLoadingUiState
import com.soma369.laimory.feature.home.viewmodel.DraftLoadingViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DraftLoadingRoute(
    innerPadding: PaddingValues,
    viewModel: DraftLoadingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // 뒤로가기는 작업을 취소하지 않는다. 추적은 그대로 두고 홈으로만 돌아간다.
    BackHandler { viewModel.sendIntent(DraftLoadingUiIntent.NavigateBack) }

    DraftLoadingScreen(
        innerPadding = innerPadding,
        state = state,
        onIntent = viewModel::sendIntent,
    )
}

@Composable
private fun DraftLoadingScreen(
    innerPadding: PaddingValues,
    state: DraftLoadingUiState,
    onIntent: (DraftLoadingUiIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
    ) {
        LaimoryTopAppBar(
            // 생성 중에는 다른 동작을 걸지 않는다 — Figma의 trailing 메뉴는 이 화면에서 숨긴다.
            title = { Text(text = state.recordDate?.format(TopBarDateFormatter).orEmpty()) },
            onBackClick = { onIntent(DraftLoadingUiIntent.NavigateBack) },
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.extraLarge2)
                    .padding(top = Spacing.medium, bottom = Spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge2),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
                Text(
                    text = state.recordDate.headline(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "보통 1분 정도 걸려요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.photoUris.isEmpty()) {
                DraftLoadingPlaceholder()
            } else {
                DraftLoadingPhotoPager(photoUris = state.photoUris, modifier = Modifier.fillMaxWidth())
            }
            DraftLoadingProgressCard(rows = state.progressRows())
            state.notice?.let { notice ->
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                    Text(
                        text = notice.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                        notice.primaryAction?.let { action ->
                            Button(onClick = { onIntent(action.intent) }) { Text(action.label) }
                        }
                        notice.secondaryAction?.let { action ->
                            OutlinedButton(onClick = { onIntent(action.intent) }) { Text(action.label) }
                        }
                    }
                }
            }
            FooterNote()
        }
    }
}

/** 사진이 없거나 URI를 읽지 못한 경우. 생성 자체는 계속 진행된다. */
@Composable
private fun DraftLoadingPlaceholder() {
    Box(
        modifier = Modifier.fillMaxWidth().height(PlaceholderHeight),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun FooterNote() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(FooterCornerRadius))
                .padding(Spacing.medium),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "모든 정보는 기기에만 저장되며,\n기록 작성 후 자동으로 정리돼요.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** 기록 날짜가 오늘인지에 따라 제목을 바꾼다. 상단 바가 날짜를 보여주므로 문구와 어긋나면 안 된다. */
private fun LocalDate?.headline(): String {
    val date = this ?: return "하루를 모으는 중이에요"
    val today = LocalDate.now()
    return when (date) {
        today -> "오늘 하루를 모으는 중이에요"
        today.minusDays(1) -> "어제 하루를 모으는 중이에요"
        else -> "${date.format(HeadlineDateFormatter)}의 기록을 모으는 중이에요"
    }
}

private fun DraftLoadingUiState.progressRows(): List<DraftLoadingProgressRow> =
    listOf(
        DraftLoadingProgressRow(
            stage = DraftLoadingStage.PHOTO,
            label = "사진 분석",
            state = stageStates.stateOf(DraftLoadingStage.PHOTO),
            doneLabel = "${photoCount}장 완료",
            progressLabel = "분석 중...",
        ),
        DraftLoadingProgressRow(
            stage = DraftLoadingStage.CALENDAR,
            label = "일정 확인",
            state = stageStates.stateOf(DraftLoadingStage.CALENDAR),
            doneLabel = "${calendarCount}건 완료",
            progressLabel = "확인 중...",
        ),
        DraftLoadingProgressRow(
            stage = DraftLoadingStage.STAY,
            label = "머무른 장소 정리",
            state = stageStates.stateOf(DraftLoadingStage.STAY),
            doneLabel = "${stayCount}곳 완료",
            progressLabel = "정리 중...",
        ),
        DraftLoadingProgressRow(
            stage = DraftLoadingStage.AI,
            label = "AI 초안 생성",
            state = stageStates.stateOf(DraftLoadingStage.AI),
            doneLabel = "완료",
            progressLabel = "분석 중...",
        ),
    )

private fun Map<DraftLoadingStage, DraftLoadingStageState>.stateOf(stage: DraftLoadingStage) = this[stage] ?: DraftLoadingStageState.PENDING

private val TopBarDateFormatter = DateTimeFormatter.ofPattern("M월 d일 EEEE")
private val HeadlineDateFormatter = DateTimeFormatter.ofPattern("M월 d일")

/** 사진이 없어도 뷰어와 같은 높이를 잡아 진행 카드 위치가 흔들리지 않게 한다. */
private val PlaceholderHeight = 216.dp
private val FooterCornerRadius = 12.dp

@Preview(name = "초안 생성 로딩", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun DraftLoadingScreenPreview() {
    LaimoryTheme {
        DraftLoadingScreen(
            innerPadding = PaddingValues(),
            state =
                DraftLoadingUiState(
                    recordDate = LocalDate.now(),
                    photoUris = List(5) { "content://photo/$it" },
                    photoCount = 13,
                    calendarCount = 4,
                    stayCount = 3,
                    stageStates =
                        mapOf(
                            DraftLoadingStage.PHOTO to DraftLoadingStageState.DONE,
                            DraftLoadingStage.CALENDAR to DraftLoadingStageState.DONE,
                            DraftLoadingStage.STAY to DraftLoadingStageState.IN_PROGRESS,
                            DraftLoadingStage.AI to DraftLoadingStageState.PENDING,
                        ),
                ),
            onIntent = {},
        )
    }
}
