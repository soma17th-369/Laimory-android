package com.soma369.laimory.feature.onboarding.screen

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.ui.permission.DataPermission
import com.soma369.laimory.core.ui.permission.LocationPermissionStep
import com.soma369.laimory.core.ui.permission.rememberDataPermissionState
import com.soma369.laimory.core.ui.terms.rememberTermContentLauncher
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.onboarding.component.OnboardingConsentChecklist
import com.soma369.laimory.feature.onboarding.component.OnboardingPageContent
import com.soma369.laimory.feature.onboarding.component.OnboardingProgress
import com.soma369.laimory.feature.onboarding.model.OnboardingPageSpec
import com.soma369.laimory.feature.onboarding.model.PermissionGuideSpec
import com.soma369.laimory.feature.onboarding.model.advancesAfterGrant
import com.soma369.laimory.feature.onboarding.model.isPageDone
import com.soma369.laimory.feature.onboarding.model.permissionGuideSpec
import com.soma369.laimory.feature.onboarding.state.OnboardingUiIntent
import com.soma369.laimory.feature.onboarding.state.OnboardingUiState
import com.soma369.laimory.feature.onboarding.viewmodel.OnboardingViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import com.soma369.laimory.core.ui.R as UiR

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

    // 백그라운드 위치까지 받았으면 수집 상태를 맞춘다. 전환이 아니라 상태를 본다 — 진입 시점에
    // 이미 허용돼 있으면 전환이 없어서, 전환만 보면 그 사용자는 영영 켜지지 않는다. 사용자가 일부러
    // 꺼 둔 수집을 되살리지 않는 판단은 reconcile 이 이미 갖고 있다.
    // 이동수단 인식만 거부한 사용자도 수집은 돈다 — GRANTED 로 좁히면 그 사용자는 여기서 켜지지 않는다.
    LaunchedEffect(permissionState.locationStep) {
        if (permissionState.locationStep.collectsInBackground) {
            onIntent(OnboardingUiIntent.ReconcileLocationTracking)
        }
    }

    val currentPage = state.pages.getOrNull(pagerState.currentPage)
    val isLastPage = pagerState.currentPage == state.pages.lastIndex
    // 이미 받은 권한은 다시 묻지 않는다. 시스템이 두 번째 요청을 조용히 무시해 아무 일도
    // 일어나지 않은 것처럼 보이기 때문이다.
    val needsRequest = currentPage?.permission != null && !permissionState.isPageDone(currentPage.permission)
    // 아직 받을 것이 남아 있는 장인지. 이미 다 동의했고 연령까지 확인한 사용자에게는 채울 것이
    // 없으므로 마지막 장이 평범한 마무리 장이 된다.
    //
    // 연령 확인도 여기에 넣는다 — 약관을 모두 동의한 계정이라도 확인이 남아 있으면 아직 채울 것이
    // 있는 장이다. 다만 CTA 가 대신 체크해 주지는 않는다(확인하지 않은 사용자가 확인한 것으로
    // 기록되면 이 확인을 둔 이유가 사라진다).
    val needsConsent =
        currentPage?.showsConsents == true &&
            (state.consentDocuments.any { it.termType !in state.lockedConsents } || !state.isAgeConfirmed)
    val termContentLauncher = rememberTermContentLauncher()
    // 이 장에서 우리 버튼으로 요청을 보냈는지. 두 가지가 여기에 달려 있다 — 허용될 때까지 안내를
    // 띄우는 것과, 허용이 끝나면 다음 장으로 넘기는 것. 누르지 않은 장에는 둘 다 하지 않는다.
    var requestedPermissions by remember { mutableStateOf(emptySet<DataPermission>()) }
    val goNext: () -> Unit = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }

    // 허용이 끝나면 버튼을 한 번 더 누르지 않아도 넘어간다. 넘기면서 표시를 지워 **한 번의 허용에
    // 한 장만** 넘긴다 — 결과 콜백과 복귀 재조회가 잇따라 와도 두 장을 건너뛰지 않는다.
    val currentPermission = currentPage?.permission
    val isCurrentPageDone = permissionState.isPageDone(currentPermission)
    LaunchedEffect(pagerState.currentPage, currentPermission, isCurrentPageDone) {
        val permission = currentPermission ?: return@LaunchedEffect
        val advances =
            advancesAfterGrant(
                permission = permission,
                isPageDone = isCurrentPageDone,
                wasRequestedHere = permission in requestedPermissions,
                isLastPage = isLastPage,
            )
        if (!advances) return@LaunchedEffect
        requestedPermissions = requestedPermissions - permission
        goNext()
    }

    OnboardingScreen(
        innerPadding = innerPadding,
        state = state,
        pagerState = pagerState,
        ctaLabel =
            ctaLabel(
                page = currentPage,
                needsRequest = needsRequest,
                isLastPage = isLastPage,
                needsConsent = needsConsent,
                hasConsentLoadFailed = state.hasConsentLoadFailed,
                locationStep = permissionState.locationStep,
            ),
        // 연령 미확인으로 버튼을 잠그지 않는다. 눌리지 않는 회색 버튼은 왜 막혔는지 말해 주지
        // 못한다 — 대신 버튼이 확인을 함께 채운다(문구가 `모두 동의하고 시작하기` 다).
        isPrimaryEnabled = !state.isCompleting && !state.isConsentSubmitting,
        // 건너뛰기는 요청이 남아 있을 때만 둔다. 이미 허용했거나 안내 전용 장에서는 건너뛸 것이
        // 없어, 버튼만 남으면 무엇을 건너뛰는지 알 수 없다.
        showsSkip = currentPage?.isSkippable == true && needsRequest && !isLastPage,
        isPageGranted = { page -> permissionState.isPageDone(page.permission) },
        // 위치는 창이 두 번 뜨므로 남은 단계를 보고 안내도 함께 바뀐다.
        guideFor = { page ->
            page.permission
                ?.takeIf { it in requestedPermissions && !permissionState.isPageDone(it) }
                ?.let { permissionGuideSpec(permission = it, locationStep = permissionState.locationStep) }
        },
        onPrimaryClick = {
            when {
                // 요청이 아니라 `act` 로 부른다. 두 번 거부해 시스템이 요청을 삼키는 장에서는 요청
                // 대신 앱 정보 화면으로 길을 바꿔야 한다 — `request` 는 그 판정을 거치지 않아, 막힌
                // 장에서 눌러도 아무 일이 없는 버튼이 된다.
                needsRequest ->
                    currentPage?.permission?.let { permission ->
                        requestedPermissions = requestedPermissions + permission
                        permissionState.act(permission)
                    }
                // 불러오지 못한 채로 끝낼 수 없다. 같은 자리에서 다시 시도한다.
                isLastPage && state.hasConsentLoadFailed -> onIntent(OnboardingUiIntent.RetryConsentLoad)
                isLastPage -> onIntent(OnboardingUiIntent.Complete)
                else -> goNext()
            }
        },
        onConsentToggle = { termType -> onIntent(OnboardingUiIntent.ConsentToggled(termType)) },
        onAgeConfirmationToggle = { onIntent(OnboardingUiIntent.AgeConfirmationToggled) },
        onOpenTerm = { document -> termContentLauncher.open(document.contentUrl) },
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
    isPrimaryEnabled: Boolean,
    showsSkip: Boolean,
    isPageGranted: (OnboardingPageSpec) -> Boolean,
    guideFor: (OnboardingPageSpec) -> PermissionGuideSpec? = { null },
    onPrimaryClick: () -> Unit,
    onSkipClick: () -> Unit,
    onConsentToggle: (TermType) -> Unit,
    onAgeConfirmationToggle: () -> Unit,
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
        // 진행 표시·뒤로·`나중에` 를 한 줄에 둔다. 어디까지 왔는지와 빠져나갈 길이 같은 자리에
        // 있어야 장마다 눈이 아래위로 옮겨 다니지 않는다.
        //
        // 가운데 정렬 상단바(`LaimoryTopAppBar`)를 쓰지 않는다 — 그것은 제목 자리의 양옆을 48dp 씩
        // 비우므로 진행 표시가 폭을 채우지 못한다. 양옆 자리는 버튼이 없는 장에서도 폭을 그대로
        // 지킨다. 장마다 막대 길이가 달라지면 넘길 때 표시가 늘었다 줄었다 한다.
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(TOP_BAR_HEIGHT)
                    .padding(horizontal = Spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.width(TOP_BAR_SIDE_WIDTH), contentAlignment = Alignment.CenterStart) {
                // 첫 장에는 뒤로 갈 곳이 없다. 앱 루트라 빠져나가면 빈 화면이 남는다.
                if (pagerState.currentPage > 0) {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(UiR.drawable.ico_default_caret_left),
                            contentDescription = "뒤로 가기",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(TOP_BAR_ICON_SIZE),
                        )
                    }
                }
            }
            OnboardingProgress(
                currentIndex = pagerState.currentPage,
                pageCount = state.pages.size,
                modifier = Modifier.weight(1f).padding(horizontal = Spacing.small),
            )
            Box(modifier = Modifier.width(TOP_BAR_SIDE_WIDTH), contentAlignment = Alignment.CenterEnd) {
                if (showsSkip) {
                    TextButton(onClick = onSkipClick, enabled = !state.isCompleting) {
                        Text(text = "나중에", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                // 쓸어서 넘기지 못하게 한다. 권한 장은 버튼을 누르면 시스템 창이 이어지는 흐름이라,
                // 옆으로 넘겨 버리면 무엇을 허용했는지 모르는 채 지나간다. 이동은 버튼으로만 한다.
                userScrollEnabled = false,
            ) { page ->
                state.pages.getOrNull(page)?.let { spec ->
                    OnboardingPageContent(
                        page = spec,
                        nickname = state.nickname,
                        isGranted = isPageGranted(spec),
                        guide = guideFor(spec),
                        // 문서가 비어도(이미 다 동의했거나 catalog 가 아직 없어도) 목록을 그린다 —
                        // 연령 확인 줄은 서버 문서와 무관하게 언제나 받아야 한다.
                        extra =
                            if (!spec.showsConsents) {
                                null
                            } else {
                                {
                                    OnboardingConsentChecklist(
                                        documents = state.consentDocuments,
                                        checked = state.checkedConsents,
                                        locked = state.lockedConsents,
                                        isAgeConfirmed = state.isAgeConfirmed,
                                        isEnabled = !state.isConsentSubmitting,
                                        errorMessage = state.consentErrorMessage,
                                        onToggle = onConsentToggle,
                                        onToggleAge = onAgeConfirmationToggle,
                                        onOpenTerm = onOpenTerm,
                                    )
                                }
                            },
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.large),
        ) {
            // `나중에` 와 진행 표시가 상단 바로 올라가면서 이 자리는 완료 실패 문구만 쓴다. 높이를 비워 두지 않는다 —
            // 모든 장이 같은 높이라 넘길 때 튀지 않고, 실패 문구는 마지막 장에서만 잠깐 끼어든다.
            if (state.hasCompletionFailed) {
                Text(
                    text = "완료를 저장하지 못했어요. 다시 시도해 주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(
                onClick = onPrimaryClick,
                enabled = isPrimaryEnabled,
                modifier = Modifier.fillMaxWidth().height(CTA_HEIGHT),
                shape = MaterialTheme.shapes.medium,
            ) {
                // 버튼 안에서 돌리지 않는다. 글자가 사라졌다 돌아오면 무엇을 누른 버튼인지가 가려지고,
                // 기다리는 동안 화면이 멈춘 것처럼 보인다. 보내는 사이에는 버튼이 눌리지 않는 것으로
                // 충분하다(`isPrimaryEnabled`).
                Text(text = ctaLabel, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

/**
 * 주 버튼 문구.
 *
 * 위치만 한 장 안에서 문구가 바뀐다. 전경 위치를 받고 돌아오면 `항상 허용` 이 남으므로, 버튼이 그
 * 다음 걸음을 말한다 — 누르면 이 앱의 위치 권한 화면이 곧장 뜬다.
 */
private fun ctaLabel(
    page: OnboardingPageSpec?,
    needsRequest: Boolean,
    isLastPage: Boolean,
    needsConsent: Boolean,
    hasConsentLoadFailed: Boolean,
    locationStep: LocationPermissionStep,
): String =
    when {
        // 불러오지 못한 목록을 두고 `시작하기` 라고 쓰면, 눌러도 아무 일이 없는 버튼이 된다.
        isLastPage && hasConsentLoadFailed -> "다시 시도"

        // 무엇을 누르는지 버튼이 말한다. `시작하기` 만으로는 동의가 함께 일어나는 줄 알 수 없다.
        needsConsent -> "모두 동의하고 시작하기"

        page?.permission == DataPermission.LOCATION && locationStep == LocationPermissionStep.BACKGROUND ->
            "항상 허용하러 가기"

        needsRequest -> page?.primaryCta.orEmpty()
        isLastPage -> page?.primaryCta.orEmpty()
        page?.permission != null -> "다음"
        else -> page?.primaryCta.orEmpty()
    }

/** 상단 바. 다른 화면의 상단바와 같은 높이라 장을 넘나들어도 본문 시작 높이가 흔들리지 않는다. */
private val TOP_BAR_HEIGHT = 52.dp

/** 진행 표시 양옆에 두는 자리. 뒤로·`나중에` 가 없는 장에서도 이 폭은 그대로 둔다. */
private val TOP_BAR_SIDE_WIDTH = 64.dp

private val TOP_BAR_ICON_SIZE = 24.dp

private val CTA_HEIGHT = 52.dp

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
            onAgeConfirmationToggle = {},
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
)
