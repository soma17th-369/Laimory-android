package com.soma369.laimory.feature.onboarding.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.onboarding.component.OnboardingPageContent
import com.soma369.laimory.feature.onboarding.component.OnboardingProgress
import com.soma369.laimory.feature.onboarding.component.rememberOnboardingPermissionState
import com.soma369.laimory.feature.onboarding.state.OnboardingUiIntent
import com.soma369.laimory.feature.onboarding.state.OnboardingUiSideEffect
import com.soma369.laimory.feature.onboarding.state.OnboardingUiState
import com.soma369.laimory.feature.onboarding.viewmodel.OnboardingViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

@Composable
fun OnboardingRoute(
    innerPadding: PaddingValues,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnboardingScreen(
        innerPadding = innerPadding,
        state = state,
        sideEffects = viewModel.sideEffect,
        onIntent = viewModel::sendIntent,
    )
}

@Composable
internal fun OnboardingScreen(
    innerPadding: PaddingValues,
    state: OnboardingUiState,
    sideEffects: Flow<OnboardingUiSideEffect> = emptyFlow(),
    onIntent: (OnboardingUiIntent) -> Unit = {},
) {
    val pagerState = rememberPagerState(pageCount = { state.pages.size })
    val scope = rememberCoroutineScope()
    val permissionState = rememberOnboardingPermissionState()

    // 마지막으로 본 장으로 되돌린다. 상태에 인덱스를 두고 pagerState 초기값으로 쓸 수 없다 —
    // pagerState 는 첫 컴포지션에 만들어지고 복원 값은 그 뒤에 도착한다.
    LaunchedEffect(sideEffects) {
        sideEffects.collect { effect ->
            when (effect) {
                is OnboardingUiSideEffect.RestorePage -> pagerState.scrollToPage(effect.pageIndex)
            }
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page -> onIntent(OnboardingUiIntent.PageChanged(page)) }
    }

    // 첫 장에서는 뒤로 갈 곳이 없다. 앱 루트라 뒤로가기로 빠져나가면 빈 화면이 남는다.
    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    val currentPage = state.pages.getOrNull(pagerState.currentPage)
    val isLastPage = pagerState.currentPage == state.pages.lastIndex
    val isCurrentGranted = permissionState.isGranted(currentPage?.permission)
    // 이미 허용된 권한은 다시 묻지 않는다. 시스템이 두 번째 요청을 조용히 무시해 아무 일도
    // 일어나지 않은 것처럼 보이기 때문이다.
    val needsRequest = currentPage?.permission != null && !isCurrentGranted
    val goNext: () -> Unit = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { page ->
            state.pages.getOrNull(page)?.let { spec ->
                OnboardingPageContent(
                    page = spec,
                    nickname = state.nickname,
                    isGranted = permissionState.isGranted(spec.permission),
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.large),
        ) {
            OnboardingProgress(currentIndex = pagerState.currentPage, pageCount = state.pages.size)

            // 보조 슬롯. 내용이 없어도 높이를 그대로 차지한다 — 장마다 이 자리가 비었다 찼다 하면
            // 진행 표시와 본문의 y 가 흔들려, 넘길 때마다 화면 전체가 위아래로 튄다.
            Box(
                modifier = Modifier.fillMaxWidth().height(SECONDARY_SLOT_HEIGHT),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    state.hasCompletionFailed ->
                        Text(
                            text = "완료를 저장하지 못했어요. 다시 시도해 주세요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )

                    // 건너뛰기는 요청이 남아 있을 때만 둔다. 이미 허용했거나 안내 전용 장에서는
                    // 건너뛸 것이 없어, 버튼만 남으면 무엇을 건너뛰는지 알 수 없다.
                    currentPage?.isSkippable == true && needsRequest && !isLastPage ->
                        TextButton(onClick = goNext, enabled = !state.isCompleting) {
                            Text(text = "나중에", style = MaterialTheme.typography.bodyMedium)
                        }
                }
            }

            Button(
                onClick = {
                    when {
                        needsRequest -> currentPage?.permission?.let(permissionState::request)
                        isLastPage -> onIntent(OnboardingUiIntent.Complete)
                        else -> goNext()
                    }
                },
                enabled = !state.isCompleting,
                modifier = Modifier.fillMaxWidth().height(CTA_HEIGHT),
                shape = MaterialTheme.shapes.medium,
            ) {
                if (state.isCompleting) {
                    CircularProgressIndicator(modifier = Modifier.height(CTA_SPINNER_SIZE), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (needsRequest) currentPage?.primaryCta.orEmpty() else nextLabel(isLastPage, currentPage),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}

/** 요청이 끝난 장의 CTA. 마지막 장은 완료 문구를 그대로 쓴다. */
private fun nextLabel(
    isLastPage: Boolean,
    page: com.soma369.laimory.feature.onboarding.model.OnboardingPageSpec?,
): String =
    if (isLastPage) {
        page?.primaryCta.orEmpty()
    } else if (page?.permission != null) {
        "다음"
    } else {
        page?.primaryCta.orEmpty()
    }

/**
 * 진행 표시와 CTA 사이 보조 슬롯의 높이.
 *
 * `나중에` 와 완료 실패 문구가 이 자리를 나눠 쓴다. 둘은 같은 장에 함께 오지 않는다 — 완료
 * 실패는 마지막 장에서만 나고 그 장은 건너뛸 것이 없다.
 */
private val SECONDARY_SLOT_HEIGHT = 48.dp

private val CTA_HEIGHT = 52.dp
private val CTA_SPINNER_SIZE = 18.dp

@Preview(name = "온보딩 / 소개", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun OnboardingIntroPreview() {
    LaimoryTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            OnboardingScreen(innerPadding = PaddingValues(), state = OnboardingUiState(nickname = "김소마"))
        }
    }
}

@Preview(name = "온보딩 / 다크", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun OnboardingDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize()) {
            OnboardingScreen(innerPadding = PaddingValues(), state = OnboardingUiState(nickname = "김소마"))
        }
    }
}
