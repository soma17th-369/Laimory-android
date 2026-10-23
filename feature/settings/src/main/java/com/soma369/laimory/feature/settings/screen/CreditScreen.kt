package com.soma369.laimory.feature.settings.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.core.ui.component.LaimoryTopAppBar
import com.soma369.laimory.core.ui.component.rememberDelayedVisibility
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.core.ui.theme.laimoryColors
import com.soma369.laimory.feature.settings.model.toCreditCount
import com.soma369.laimory.feature.settings.model.toWon
import com.soma369.laimory.feature.settings.model.unitPriceLabel
import com.soma369.laimory.feature.settings.state.CreditUiIntent
import com.soma369.laimory.feature.settings.state.CreditUiSideEffect
import com.soma369.laimory.feature.settings.state.CreditUiState
import com.soma369.laimory.feature.settings.viewmodel.CreditViewModel
import kotlinx.coroutines.flow.Flow
import com.soma369.laimory.core.ui.R as CoreUiR

@Composable
fun CreditRoute(
    innerPadding: PaddingValues,
    viewModel: CreditViewModel = hiltViewModel(),
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.sendIntent(CreditUiIntent.Refresh)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    CreditContent(
        innerPadding = innerPadding,
        state = state,
        onIntent = viewModel::sendIntent,
        sideEffectFlow = viewModel.sideEffect,
    )
}

@Composable
private fun CreditContent(
    innerPadding: PaddingValues,
    state: CreditUiState,
    onIntent: (CreditUiIntent) -> Unit,
    sideEffectFlow: Flow<CreditUiSideEffect>,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    LaunchedEffect(sideEffectFlow) {
        sideEffectFlow.collect { effect ->
            when (effect) {
                is CreditUiSideEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }
    CreditScreen(
        innerPadding = innerPadding,
        state = state,
        onIntent = onIntent,
    )
}

/** Figma `feat-Credit` 3317:426 · 잔액 0 3318:444 · 상품 누름 3318:532. */
@Composable
private fun CreditScreen(
    innerPadding: PaddingValues,
    state: CreditUiState,
    onIntent: (CreditUiIntent) -> Unit,
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
                    text = "크레딧",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            onBackClick = { onIntent(CreditUiIntent.NavigateBack) },
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.extraLarge)
                    .padding(top = Spacing.small, bottom = Spacing.extraLarge2),
            verticalArrangement = Arrangement.spacedBy(Spacing.large),
        ) {
            BalanceCard(state = state)
            ChargeNotReadyNotice()
            PackageSection(onPackageClick = { amount -> onIntent(CreditUiIntent.PackageClicked(amount)) })
            Text(
                text = "표시된 가격은 예정 가격이라 충전이 열릴 때 달라질 수 있어요.\n충전이 열리면 Google Play 결제로 구매할 수 있어요.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 잔액 카드(Figma 3356:567 · 받는 중 3356:709). 값에 따라 바뀌는 것은 숫자와 쓰임 줄뿐이고, 둘 다 자리를 고정해 두어 받는 중 ·
 * 받음 · 못 받음 사이에 카드 크기와 아래 내용이 움직이지 않는다.
 */
@Composable
private fun BalanceCard(state: CreditUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.extraLarge),
            horizontalArrangement = Arrangement.spacedBy(Spacing.large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
            ) {
                Text(
                    text = "남은 크레딧",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BalanceCount(remaining = state.remaining, isLoading = state.isLoading)
                UsageLine(remaining = state.remaining, cost = state.cost, isLoading = state.isLoading)
            }
            CoinBadge()
        }
    }
}

/**
 * 숫자 자리. 높이는 숫자 글자 한 줄로, 폭은 최소 폭으로 고정한다. 기억해 둔 값이 있으면 받는 중에도 그 숫자다. 값이 없을 때만
 * 기다리는데, 늦을 때만 로딩을 띄우고([rememberDelayedVisibility]) 그 전에는 자리만 비워 둔다. 못 받았으면 0 이 아니라 `-`.
 */
@Composable
private fun BalanceCount(
    remaining: Int?,
    isLoading: Boolean,
) {
    val style = MaterialTheme.typography.displayLarge
    val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }
    val showsProgress = rememberDelayedVisibility(active = remaining == null && isLoading)
    val count = remaining?.toCreditCount() ?: if (isLoading) null else "-"
    Box(
        modifier =
            Modifier
                .height(lineHeight)
                .widthIn(min = BALANCE_COUNT_MIN_WIDTH)
                .clearAndSetSemantics {
                    contentDescription = if (count == null) "남은 크레딧 불러오는 중" else "남은 크레딧 $count"
                },
        contentAlignment = Alignment.CenterStart,
    ) {
        when {
            showsProgress ->
                CircularProgressIndicator(
                    modifier = Modifier.size(BALANCE_PROGRESS_SIZE),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                )
            count != null -> Text(text = count, style = style, color = MaterialTheme.colorScheme.primary, maxLines = 1)
        }
    }
}

/**
 * 1개당 쓰임. 비용은 서버 값이라 받은 그대로 쓴다. 모자라면 몇 개가 필요한지로 바꾼다.
 *
 * 늘 한 줄을 차지한다 — 기다리는 동안에는 같은 문구를 보이지 않게 깔아 자리만 잡고, 못 받았으면 그렇다고 적는다. 줄이 생겼다
 * 없어지면 카드 높이가 바뀌어 아래 내용이 밀린다.
 */
@Composable
private fun UsageLine(
    remaining: Int?,
    cost: Int?,
    isLoading: Boolean,
) {
    val style = MaterialTheme.typography.bodySmall
    val variant = MaterialTheme.colorScheme.onSurfaceVariant
    when {
        remaining != null && cost != null ->
            if (remaining >= cost) {
                Text(
                    text = "${cost}개로 타임라인을 1번 만들 수 있어요",
                    style = style,
                    color = variant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    text = "만들려면 크레딧 ${cost}개가 필요해요",
                    style = style,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        isLoading ->
            Text(
                text = "1개로 타임라인을 1번 만들 수 있어요",
                modifier = Modifier.alpha(0f).clearAndSetSemantics { },
                style = style,
                maxLines = 1,
            )
        else -> Text(text = "잔액을 불러오지 못했어요", style = style, color = variant, maxLines = 1)
    }
}

/** 오른쪽 동전 배지. 숫자 옆에 붙이지 않고 원 안에 넉넉히 둔다. */
@Composable
private fun CoinBadge() {
    Box(
        modifier =
            Modifier
                .size(COIN_BADGE_SIZE)
                .background(MaterialTheme.laimoryColors.warningContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(CoreUiR.drawable.ico_default_coins),
            contentDescription = null,
            modifier = Modifier.size(COIN_ICON_SIZE),
            tint = MaterialTheme.laimoryColors.warning,
        )
    }
}

@Composable
private fun ChargeNotReadyNotice() {
    Text(
        text = "크레딧 충전은 준비 중이에요.",
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.laimoryColors.warningContainer, MaterialTheme.shapes.small)
                .padding(horizontal = 14.dp, vertical = Spacing.medium),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.laimoryColors.onWarningContainer,
    )
}

/**
 * 충전 상품 목록. 가격은 확정이 아닌 예정 가격이라 제목 줄 오른쪽에 한 번 알린다 — 줄마다 붙이면 가격 · `예정` · `준비 중` 이
 * 겹친다. 달라질 수 있다는 설명은 화면 아래 안내가 맡는다.
 */
@Composable
private fun PackageSection(onPackageClick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "충전 상품",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "예정 가격",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column {
                CREDIT_PACKAGES.forEachIndexed { index, item ->
                    PackageRow(item = item, onClick = { onPackageClick(item.amount) })
                    if (index != CREDIT_PACKAGES.lastIndex) {
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

/**
 * 충전 상품 한 줄(Figma `CreditPackageRow` 3317:378). 가격은 버튼이 아니라 글자로 오른쪽 열에 세운다 — 줄마다 `원` 끝이 한
 * 세로줄에 서서 가격표처럼 읽힌다. 누를 수 있다는 것은 끝의 화살표가 알린다.
 *
 * 아직 살 수 없다는 것은 위 배너 한 곳에서만 알린다. 줄마다 `준비 중` 을 붙이면 같은 말이 세 번 반복되고, 개당 가격이 들어갈
 * 자리도 모자란다.
 *
 * 충전을 열지 않았어도 누를 수 있다 — 누르면 준비 중이라고 알린다. 비활성으로 두면 왜 안 눌리는지 알 수 없다. 결제를 열면 같은
 * 줄을 누를 때 곧바로 Play 결제 창이 뜬다(상품 상세 화면은 두지 않는다).
 */
@Composable
private fun PackageRow(
    item: CreditPackage,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = Spacing.large, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(CoreUiR.drawable.ico_default_coins),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.laimoryColors.warning,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "${item.amount}개",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = listOfNotNull(item.period, unitPriceLabel(item.amount, item.price)).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.price.toWon(),
                modifier = Modifier.widthIn(min = PRICE_MIN_WIDTH),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
            )
            Icon(
                painter = painterResource(CoreUiR.drawable.ico_default_caret_right),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class CreditPackage(
    val amount: Int,
    val period: String,
    /** 원 단위 예정 가격. */
    val price: Int,
)

/**
 * 하루 한 번 기록하는 기준으로 기간을 붙인다. 가격은 예정 가격이다 — 충전을 열 때 Play 인앱 상품 가격과 맞추고, Play 가
 * 돌려주는 현지화된 가격 문자열로 바꾼다.
 */
private val CREDIT_PACKAGES =
    listOf(
        CreditPackage(amount = 1, period = "하루 치", price = 200),
        CreditPackage(amount = 7, period = "일주일 치", price = 1_000),
        CreditPackage(amount = 30, period = "한 달 치", price = 3_500),
    )

/** 가격 열의 최소 폭. 글자 길이를 따르면 짧은 가격의 줄만 화살표가 오른쪽으로 붙어 열이 흔들린다. */
private val PRICE_MIN_WIDTH = 72.dp

private val BALANCE_PROGRESS_SIZE = 28.dp

/** 숫자 자리 최소 폭. 로딩(28dp)과 두 자리 숫자가 같은 자리를 쓴다. */
private val BALANCE_COUNT_MIN_WIDTH = 48.dp
private val COIN_BADGE_SIZE = 64.dp
private val COIN_ICON_SIZE = 32.dp

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun CreditPreview() {
    LaimoryTheme {
        CreditScreen(
            innerPadding = PaddingValues(),
            state = CreditUiState(remaining = 42, cost = 1, isLoading = false),
            onIntent = {},
        )
    }
}

@Preview(name = "잔액 0", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun CreditShortPreview() {
    LaimoryTheme {
        CreditScreen(
            innerPadding = PaddingValues(),
            state = CreditUiState(remaining = 0, cost = 1, isLoading = false),
            onIntent = {},
        )
    }
}

@Preview(name = "네 자리", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun CreditFourDigitsPreview() {
    LaimoryTheme {
        CreditScreen(
            innerPadding = PaddingValues(),
            state = CreditUiState(remaining = 12_345, cost = 1, isLoading = false),
            onIntent = {},
        )
    }
}

@Preview(name = "받는 중", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun CreditLoadingPreview() {
    LaimoryTheme {
        CreditScreen(
            innerPadding = PaddingValues(),
            state = CreditUiState(isLoading = true),
            onIntent = {},
        )
    }
}

@Preview(name = "조회 실패 · 다크", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun CreditUnavailableDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        CreditScreen(
            innerPadding = PaddingValues(),
            state = CreditUiState(isLoading = false),
            onIntent = {},
        )
    }
}
