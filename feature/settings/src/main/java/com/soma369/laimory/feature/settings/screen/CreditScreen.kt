package com.soma369.laimory.feature.settings.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.domain.model.credit.TimelineCredit
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.core.ui.component.LaimoryTopAppBar
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.core.ui.theme.laimoryColors
import com.soma369.laimory.feature.settings.state.CreditBalance
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
            BalanceCard(balance = state.balance)
            ChargeNotReadyNotice()
            PackageSection(onPackageClick = { amount -> onIntent(CreditUiIntent.PackageClicked(amount)) })
            Text(
                text = "가격과 결제 방법은 충전을 열 때 안내할게요.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BalanceCard(balance: CreditBalance) {
    val credit = (balance as? CreditBalance.Loaded)?.credit
    val count =
        when (balance) {
            CreditBalance.Loading -> ""
            CreditBalance.Unavailable -> "-"
            is CreditBalance.Loaded -> balance.credit.remaining.toString()
        }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "남은 크레딧",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier =
                    Modifier
                        .heightIn(min = BALANCE_ROW_MIN_HEIGHT)
                        // 받는 중에는 빈 숫자를 읽지 않게 한다.
                        .clearAndSetSemantics {
                            if (balance !is CreditBalance.Loading) contentDescription = "남은 크레딧 $count"
                        },
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(CoreUiR.drawable.ico_default_coins),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.laimoryColors.warning,
                )
                Text(
                    text = count,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            credit?.let { UsageLine(credit = it) }
        }
    }
}

/** 1개당 쓰임. 비용은 서버 값이라 받은 그대로 쓴다. 모자라면 무엇이 얼마나 필요한지로 바꾼다. */
@Composable
private fun UsageLine(credit: TimelineCredit) {
    if (credit.isEnough) {
        Text(
            text = "${credit.cost}개로 타임라인을 1번 만들 수 있어요",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Text(
            text = "타임라인을 만들려면 크레딧 ${credit.cost}개가 필요해요",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun ChargeNotReadyNotice() {
    Text(
        text = "크레딧 충전은 준비 중이에요. 곧 열릴 거예요.",
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.laimoryColors.warningContainer, MaterialTheme.shapes.small)
                .padding(horizontal = 14.dp, vertical = Spacing.medium),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.laimoryColors.onWarningContainer,
    )
}

@Composable
private fun PackageSection(onPackageClick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Text(
            text = "충전 상품",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
 * 충전 상품 한 줄(Figma `CreditPackageRow` 3317:378). 가격이 정해지면 `준비 중` 자리에 가격이 온다.
 *
 * 충전을 열지 않았어도 누를 수 있다 — 누르면 준비 중이라고 알린다. 비활성으로 두면 왜 안 눌리는지 알 수 없다.
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
                text = item.period,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "준비 중",
            modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .padding(horizontal = 10.dp, vertical = Spacing.extraSmall),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class CreditPackage(
    val amount: Int,
    val period: String,
)

/** 하루 한 번 기록하는 기준으로 기간을 붙인다. 가격은 아직 정하지 않았다. */
private val CREDIT_PACKAGES =
    listOf(
        CreditPackage(amount = 7, period = "일주일 치"),
        CreditPackage(amount = 30, period = "한 달 치"),
    )

/** 받는 중에도 숫자 줄 높이를 지켜 값이 도착할 때 아래가 밀리지 않게 한다. */
private val BALANCE_ROW_MIN_HEIGHT = 40.dp

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun CreditPreview() {
    LaimoryTheme {
        CreditScreen(
            innerPadding = PaddingValues(),
            state = CreditUiState(balance = CreditBalance.Loaded(TimelineCredit(cost = 1, remaining = 42))),
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
            state = CreditUiState(balance = CreditBalance.Loaded(TimelineCredit(cost = 1, remaining = 0))),
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
            state = CreditUiState(balance = CreditBalance.Unavailable),
            onIntent = {},
        )
    }
}
