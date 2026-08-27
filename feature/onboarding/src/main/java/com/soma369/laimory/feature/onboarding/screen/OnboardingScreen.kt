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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.ui.permission.DataPermission
import com.soma369.laimory.core.ui.permission.LocationPermissionStep
import com.soma369.laimory.core.ui.permission.rememberDataPermissionState
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.onboarding.component.OnboardingPageContent
import com.soma369.laimory.feature.onboarding.component.OnboardingProgress
import com.soma369.laimory.feature.onboarding.model.OnboardingPageSpec
import com.soma369.laimory.feature.onboarding.state.OnboardingUiIntent
import com.soma369.laimory.feature.onboarding.state.OnboardingUiSideEffect
import com.soma369.laimory.feature.onboarding.state.OnboardingUiState
import com.soma369.laimory.feature.onboarding.viewmodel.OnboardingViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun OnboardingRoute(
    innerPadding: PaddingValues,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnboardingContent(
        innerPadding = innerPadding,
        state = state,
        onIntent = viewModel::sendIntent,
        sideEffectFlow = viewModel.sideEffect,
    )
}

/**
 * 플랫폼 오케스트레이션을 맡는다 — 권한 요청 창구, 진행 복원 SideEffect, Pager 위치.
 *
 * [OnboardingScreen] 은 이 결과를 파라미터로만 받아 그린다. 권한 launcher 가 Screen 에 있으면
 * Preview 가 Android 요청 경로를 타서 미리보기로 배치를 확인할 수 없다.
 */
@Composable
private fun OnboardingContent(
    innerPadding: PaddingValues,
    state: OnboardingUiState,
    onIntent: (OnboardingUiIntent) -> Unit,
    sideEffectFlow: Flow<OnboardingUiSideEffect>,
) {
    val permissionState = rememberDataPermissionState()
    val pagerState = rememberPagerState(pageCount = { state.pages.size })
    val scope = rememberCoroutineScope()

    // 마지막으로 본 장으로 되돌린다. 상태의 인덱스를 pagerState 초기값으로 쓸 수 없다 —
    // pagerState 는 첫 컴포지션에 만들어지고 복원 값은 그 뒤에 도착한다.
    LaunchedEffect(Unit) {
        sideEffectFlow.collect { effect ->
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

    // 백그라운드 위치까지 받은 순간에만 추적을 켠다. 진입 시점에 이미 허용돼 있던 경우는 건드리지
    // 않는다 — 사용자가 일부러 꺼 둔 추적을 온보딩이 조용히 되살리면 안 된다.
    var wasLocationGranted by remember { mutableStateOf(permissionState.locationStep == LocationPermissionStep.GRANTED) }
    LaunchedEffect(permissionState.locationStep) {
        val isGranted = permissionState.locationStep == LocationPermissionStep.GRANTED
        if (isGranted && !wasLocationGranted) onIntent(OnboardingUiIntent.EnableLocationTracking)
        wasLocationGranted = isGranted
    }

    val currentPage = state.pages.getOrNull(pagerState.currentPage)
    val isLastPage = pagerState.currentPage == state.pages.lastIndex
    // 이미 허용된 권한은 다시 묻지 않는다. 시스템이 두 번째 요청을 조용히 무시해 아무 일도
    // 일어나지 않은 것처럼 보이기 때문이다.
    val needsRequest = currentPage?.permission != null && !permissionState.isGranted(currentPage.permission)
    val goNext: () -> Unit = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }

    OnboardingScreen(
        innerPadding = innerPadding,
        state = state,
        pagerState = pagerState,
        ctaLabel = ctaLabel(currentPage, needsRequest, isLastPage, permissionState.locationStep),
        // 건너뛰기는 요청이 남아 있을 때만 둔다. 이미 허용했거나 안내 전용 장에서는 건너뛸 것이
        // 없어, 버튼만 남으면 무엇을 건너뛰는지 알 수 없다.
        showsSkip = currentPage?.isSkippable == true && needsRequest && !isLastPage,
        isPageGranted = { page -> permissionState.isGranted(page.permission) },
        onPrimaryClick = {
            when {
                needsRequest -> currentPage?.permission?.let(permissionState::request)
                isLastPage -> onIntent(OnboardingUiIntent.Complete)
                else -> goNext()
            }
        },
        onSkipClick = goNext,
        onBack = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
    )
}

@Composable
private fun OnboardingScreen(
    innerPadding: PaddingValues,
    state: OnboardingUiState,
    pagerState: PagerState,
    ctaLabel: String,
    showsSkip: Boolean,
    isPageGranted: (OnboardingPageSpec) -> Boolean,
    onPrimaryClick: () -> Unit,
    onSkipClick: () -> Unit,
    onBack: () -> Unit,
) {
    // 첫 장에서는 뒤로 갈 곳이 없다. 앱 루트라 뒤로가기로 빠져나가면 빈 화면이 남는다.
    BackHandler(enabled = pagerState.currentPage > 0, onBack = onBack)

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
                    isGranted = isPageGranted(spec),
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

                    showsSkip ->
                        TextButton(onClick = onSkipClick, enabled = !state.isCompleting) {
                            Text(text = "나중에", style = MaterialTheme.typography.bodyMedium)
                        }
                }
            }

            Button(
                onClick = onPrimaryClick,
                enabled = !state.isCompleting,
                modifier = Modifier.fillMaxWidth().height(CTA_HEIGHT),
                shape = MaterialTheme.shapes.medium,
            ) {
                if (state.isCompleting) {
                    CircularProgressIndicator(modifier = Modifier.height(CTA_SPINNER_SIZE), strokeWidth = 2.dp)
                } else {
                    Text(text = ctaLabel, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

/**
 * 주 버튼 문구.
 *
 * 위치만 한 장 안에서 문구가 바뀐다 — 남은 단계가 무엇인지 버튼이 말하지 않으면, 눌렀는데 또
 * 눌러야 하는 화면이 된다.
 */
private fun ctaLabel(
    page: OnboardingPageSpec?,
    needsRequest: Boolean,
    isLastPage: Boolean,
    locationStep: LocationPermissionStep,
): String =
    when {
        page?.permission == DataPermission.LOCATION && locationStep != LocationPermissionStep.GRANTED ->
            when (locationStep) {
                LocationPermissionStep.BACKGROUND -> "'항상 허용'으로 바꾸기"
                LocationPermissionStep.ACTIVITY -> "이동수단 인식 켜기"
                else -> page.primaryCta
            }

        needsRequest -> page?.primaryCta.orEmpty()
        isLastPage -> page?.primaryCta.orEmpty()
        page?.permission != null -> "다음"
        else -> page?.primaryCta.orEmpty()
    }

/**
 * 진행 표시와 주 버튼 사이 보조 슬롯의 높이.
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
    OnboardingScreenPreview(pageIndex = 0)
}

@Preview(name = "온보딩 / 권한 장", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun OnboardingPermissionPreview() {
    OnboardingScreenPreview(pageIndex = 1, showsSkip = true, ctaLabel = "사진 연결하기")
}

@Preview(name = "온보딩 / 다크", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun OnboardingDarkPreview() {
    OnboardingScreenPreview(pageIndex = 0, darkTheme = true)
}

@Composable
private fun OnboardingScreenPreview(
    pageIndex: Int,
    showsSkip: Boolean = false,
    ctaLabel: String = "시작하기",
    darkTheme: Boolean = false,
) {
    val state = OnboardingUiState(nickname = "김소마")
    LaimoryTheme(darkTheme = darkTheme) {
        OnboardingScreen(
            innerPadding = PaddingValues(),
            state = state,
            pagerState = rememberPagerState(initialPage = pageIndex, pageCount = { state.pages.size }),
            ctaLabel = ctaLabel,
            showsSkip = showsSkip,
            isPageGranted = { false },
            onPrimaryClick = {},
            onSkipClick = {},
            onBack = {},
        )
    }
}
