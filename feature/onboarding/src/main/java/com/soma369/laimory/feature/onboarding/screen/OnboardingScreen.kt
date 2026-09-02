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
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.ui.permission.DataPermission
import com.soma369.laimory.core.ui.permission.DataSourceSheet
import com.soma369.laimory.core.ui.permission.DataSourceUiModel
import com.soma369.laimory.core.ui.permission.LocationPermissionStep
import com.soma369.laimory.core.ui.permission.rememberDataPermissionState
import com.soma369.laimory.core.ui.terms.rememberTermContentLauncher
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.onboarding.component.OnboardingConsentChecklist
import com.soma369.laimory.feature.onboarding.component.OnboardingPageContent
import com.soma369.laimory.feature.onboarding.component.OnboardingProgress
import com.soma369.laimory.feature.onboarding.model.OnboardingPageSpec
import com.soma369.laimory.feature.onboarding.state.OnboardingUiIntent
import com.soma369.laimory.feature.onboarding.state.OnboardingUiState
import com.soma369.laimory.feature.onboarding.viewmodel.OnboardingViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.LocalDateTime

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
) {
    val permissionState = rememberDataPermissionState()
    // 복원 인덱스가 정해진 뒤에 Pager 를 만든다. 먼저 만들고 나중에 스크롤시키면 첫 장이 한 번
    // 보였다 튀고, 컴포지션이 다시 만들어질 때마다 그 튐이 되풀이된다.
    val initialPage = state.initialPageIndex ?: return
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { state.pages.size })
    val scope = rememberCoroutineScope()

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
    // 아직 받을 동의가 남아 있는 장인지. 이미 다 동의한 사용자에게는 목록이 체크된 채로 보이되
    // 받을 것이 없으므로 마지막 장은 평범한 마무리 장이다.
    val needsConsent =
        currentPage?.showsConsents == true &&
            state.consentDocuments.any { it.termType !in state.lockedConsents }
    val termContentLauncher = rememberTermContentLauncher()
    // 무엇을 읽는지 묻는 사용자에게만 여는 시트. 장 문구는 값을 말하고, 이 시트가 범위를 말한다.
    var detailsSource by remember { mutableStateOf<DataSourceUiModel?>(null) }
    val goNext: () -> Unit = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }

    OnboardingScreen(
        innerPadding = innerPadding,
        state = state,
        pagerState = pagerState,
        ctaLabel = ctaLabel(currentPage, needsRequest, isLastPage, needsConsent, permissionState.locationStep),
        isPrimaryEnabled = !state.isCompleting,
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
        onConsentToggle = { termType -> onIntent(OnboardingUiIntent.ConsentToggled(termType)) },
        onOpenTerm = { document -> termContentLauncher.open(document.contentUrl) },
        onDetailsClick = { source -> detailsSource = source },
        onSkipClick = goNext,
        onBack = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
    )

    detailsSource?.let { source ->
        DataSourceSheet(
            source = source,
            status = permissionState.statusOf(source.permission),
            locationStep = permissionState.locationStep,
            action = permissionState.actionFor(source.permission),
            // 시트에서 바로 허용할 수 있게 둔다. 읽고 나서 닫고 다시 CTA 를 찾게 하면 한 번 더 묻는 셈이다.
            onAction = { permissionState.act(source.permission) },
            onDismiss = { detailsSource = null },
        )
    }
}

@Composable
private fun OnboardingScreen(
    innerPadding: PaddingValues,
    state: OnboardingUiState,
    pagerState: PagerState,
    ctaLabel: String,
    isPrimaryEnabled: Boolean,
    showsSkip: Boolean,
    isPageGranted: (OnboardingPageSpec) -> Boolean,
    onPrimaryClick: () -> Unit,
    onSkipClick: () -> Unit,
    onConsentToggle: (TermType) -> Unit,
    onDetailsClick: (DataSourceUiModel) -> Unit,
    onOpenTerm: (TermDocument) -> Unit,
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
                    // 데이터 소스 장에서만 연다. 앱 알림은 우리 알림이라 따로 설명할 것이 없다.
                    onDetailsClick = dataSourceOf(spec)?.let { source -> { onDetailsClick(source) } },
                    extra =
                        if (!spec.showsConsents || state.consentDocuments.isEmpty()) {
                            null
                        } else {
                            {
                                OnboardingConsentChecklist(
                                    documents = state.consentDocuments,
                                    checked = state.checkedConsents,
                                    locked = state.lockedConsents,
                                    isEnabled = !state.isConsentSubmitting,
                                    errorMessage = state.consentErrorMessage,
                                    onToggle = onConsentToggle,
                                    onOpenTerm = onOpenTerm,
                                )
                            }
                        },
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
                enabled = isPrimaryEnabled,
                modifier = Modifier.fillMaxWidth().height(CTA_HEIGHT),
                shape = MaterialTheme.shapes.medium,
            ) {
                if (state.isCompleting || state.isConsentSubmitting) {
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
    needsConsent: Boolean,
    locationStep: LocationPermissionStep,
): String =
    when {
        // 무엇을 누르는지 버튼이 말한다. `시작하기` 만으로는 동의가 함께 일어나는 줄 알 수 없다.
        needsConsent -> "모두 동의하고 시작하기"

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
    state: OnboardingUiState = OnboardingUiState(nickname = "김소마"),
) {
    LaimoryTheme(darkTheme = darkTheme) {
        OnboardingScreen(
            innerPadding = PaddingValues(),
            state = state,
            pagerState = rememberPagerState(initialPage = pageIndex, pageCount = { state.pages.size }),
            ctaLabel = ctaLabel,
            isPrimaryEnabled = true,
            showsSkip = showsSkip,
            isPageGranted = { false },
            onPrimaryClick = {},
            onSkipClick = {},
            onConsentToggle = {},
            onDetailsClick = {},
            onOpenTerm = {},
            onBack = {},
        )
    }
}

@Preview(name = "Onboarding / 동의", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun OnboardingConsentPreview() {
    val state =
        OnboardingUiState(
            nickname = "김소마",
            consentDocuments =
                listOf(
                    previewTerm(TermType.SENSITIVE_INFORMATION_CONSENT, "민감정보 처리 동의"),
                    previewTerm(TermType.THIRD_PARTY_PROVISION_CONSENT, "개인정보 제3자 제공 동의"),
                    previewTerm(TermType.CROSS_BORDER_TRANSFER_CONSENT, "개인정보 국외 이전 동의"),
                ),
        )
    OnboardingScreenPreview(
        pageIndex = state.pages.indexOfFirst { it.showsConsents },
        ctaLabel = "모두 동의하고 시작하기",
        state = state,
    )
}

private fun previewTerm(
    type: TermType,
    title: String,
) = TermDocument(
    termType = type,
    version = "1.0",
    title = title,
    contentUrl = "https://laimory.app/terms/preview/1.0",
    effectiveAt = LocalDateTime.of(2026, 8, 28, 0, 0),
)

/**
 * 이 장이 설명할 데이터 소스. 없으면 시트를 열 것이 없다.
 *
 * 앱 알림(리마인더)은 우리가 보내는 알림이라 무엇을 읽는지 설명할 것이 없고, 소개·완료 장은
 * 권한을 다루지 않는다.
 */
private fun dataSourceOf(page: OnboardingPageSpec): DataSourceUiModel? =
    page.permission?.let { permission -> DataSourceUiModel.entries.firstOrNull { it.permission == permission } }
