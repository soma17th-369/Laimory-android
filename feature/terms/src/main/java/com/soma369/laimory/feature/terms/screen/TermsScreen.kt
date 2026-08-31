package com.soma369.laimory.feature.terms.screen

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.domain.model.terms.TermsGateState
import com.soma369.laimory.core.ui.terms.appendTermLink
import com.soma369.laimory.core.ui.terms.rememberTermContentLauncher
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.terms.state.TermsUiIntent
import com.soma369.laimory.feature.terms.state.TermsUiState
import com.soma369.laimory.feature.terms.viewmodel.TermsViewModel
import java.time.LocalDateTime

/**
 * 이용약관 동의 화면.
 *
 * 온보딩보다 앞선 앱 루트다 — 서버가 인증 API 대부분을 이 동의로 막으므로 건너뛸 자리가 없다.
 * 대신 다른 계정으로 들어갈 길(로그아웃)은 남긴다. 그것마저 없으면 이 화면에 갇힌다.
 */
@Composable
fun TermsRoute(
    innerPadding: PaddingValues,
    viewModel: TermsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val termContentLauncher = rememberTermContentLauncher()

    TermsScreen(
        innerPadding = innerPadding,
        state = state,
        onOpenTerm = { document -> termContentLauncher.open(document.contentUrl) },
        onIntent = viewModel::sendIntent,
    )
}

@Composable
private fun TermsScreen(
    innerPadding: PaddingValues,
    state: TermsUiState,
    onOpenTerm: (TermDocument) -> Unit,
    onIntent: (TermsUiIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.extraLarge)
                    .padding(top = HeaderTopPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.large),
        ) {
            when {
                state.isLoading -> LoadingBody()
                state.hasFailed -> FailureBody()
                else -> {
                    ConsentBody(state = state, onOpenTerm = onOpenTerm)
                    AgeConfirmationRow(
                        isConfirmed = state.isAgeConfirmed,
                        isEnabled = !state.isSubmitting,
                        onConfirmationChange = { onIntent(TermsUiIntent.AgeConfirmationChanged(it)) },
                    )
                }
            }
        }

        Actions(state = state, onIntent = onIntent)
    }
}

@Composable
private fun LoadingBody() {
    Box(
        modifier = Modifier.fillMaxWidth().height(BodyMinHeight),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/**
 * 조회 실패.
 *
 * 빈 catalog 로 치환하지 않는다 — 통과시켜 주면 다음 화면부터 서버가 계속 거절해 무엇이 잘못됐는지
 * 알 수 없는 상태가 된다. 여기서 다시 시도할 자리를 준다.
 */
@Composable
private fun FailureBody() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Text(
            text = "약관을 불러오지 못했어요",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "네트워크 상태를 확인하고 다시 시도해 주세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ConsentBody(
    state: TermsUiState,
    onOpenTerm: (TermDocument) -> Unit,
) {
    val linkStyle = SpanStyle(color = MaterialTheme.colorScheme.onSurface)

    Text(
        text = "시작하기 전에\n약관을 확인해 주세요",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
        // 처리방침은 동의 대상이 아니라 상시 공개 문서다. 동의받는 것처럼 쓰면 안 된다.
        text =
            buildAnnotatedString {
                append("아래 버튼을 누르면 ")
                appendTermLink("이용약관", state.termsOfService, linkStyle, onOpenTerm)
                append("에 동의하게 됩니다. 개인정보를 어떻게 다루는지는 ")
                appendTermLink("개인정보 처리방침", state.privacyPolicy, linkStyle, onOpenTerm)
                append("에서 확인하실 수 있어요.")
            },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * 만 14세 이상 자기확인.
 *
 * 약관 동의와 한 덩어리로 묶지 않는다 — 가입 자격 확인이지 동의가 아니다. 기본은 해제이고,
 * 생년월일이나 본인인증을 요구하지 않는다.
 */
@Composable
private fun AgeConfirmationRow(
    isConfirmed: Boolean,
    isEnabled: Boolean,
    onConfirmationChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                // 글자까지 터치 영역에 넣는다. 체크박스만 누르게 하면 눌러야 할 곳이 너무 작다.
                .toggleable(
                    value = isConfirmed,
                    enabled = isEnabled,
                    role = Role.Checkbox,
                    onValueChange = onConfirmationChange,
                ).padding(vertical = Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        // 행 전체가 이미 토글이라 체크박스는 그림만 맡는다. 둘 다 누르면 두 번 뒤집힌다.
        Checkbox(checked = isConfirmed, onCheckedChange = null, enabled = isEnabled)
        Text(
            text = "[필수 확인] 만 14세 이상입니다",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun Actions(
    state: TermsUiState,
    onIntent: (TermsUiIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 오류 자리를 늘 잡아 둔다. 비었다 찼다 하면 버튼이 위아래로 튄다.
        Box(
            modifier = Modifier.fillMaxWidth().height(ErrorSlotHeight),
            contentAlignment = Alignment.Center,
        ) {
            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Button(
            onClick = { onIntent(if (state.hasFailed) TermsUiIntent.RetryClicked else TermsUiIntent.AgreeClicked) },
            enabled = if (state.hasFailed) !state.isSubmitting else state.canAgree,
            modifier = Modifier.fillMaxWidth().height(CtaHeight),
            shape = MaterialTheme.shapes.medium,
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.height(CtaSpinnerSize), strokeWidth = 2.dp)
            } else {
                Text(
                    // 결과가 분명한 문구를 쓴다. `확인` 같은 말로는 무엇에 동의하는지 알 수 없다.
                    text = if (state.hasFailed) "다시 시도" else "이용약관에 동의하고 시작하기",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }

        // 건너뛸 수는 없지만 다른 계정으로 갈 길은 남긴다.
        TextButton(
            onClick = { onIntent(TermsUiIntent.LogoutClicked) },
            enabled = !state.isSubmitting,
        ) {
            Text(text = "다른 계정으로 로그인", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private val HeaderTopPadding = 48.dp
private val BodyMinHeight = 240.dp
private val ErrorSlotHeight = 40.dp
private val CtaHeight = 52.dp
private val CtaSpinnerSize = 18.dp

@Preview(name = "Terms / 동의", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun TermsConsentPreview() {
    LaimoryTheme {
        TermsScreen(
            innerPadding = PaddingValues(),
            state =
                TermsUiState(
                    gate = TermsGateState.Required(listOf(previewDocument(TermType.TERMS_OF_SERVICE))),
                    termsOfService = previewDocument(TermType.TERMS_OF_SERVICE),
                    privacyPolicy = previewDocument(TermType.PRIVACY_POLICY),
                ),
            onOpenTerm = {},
            onIntent = {},
        )
    }
}

@Preview(name = "Terms / 조회 실패", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun TermsFailurePreview() {
    LaimoryTheme {
        TermsScreen(
            innerPadding = PaddingValues(),
            state = TermsUiState(gate = TermsGateState.Failed),
            onOpenTerm = {},
            onIntent = {},
        )
    }
}

private fun previewDocument(type: TermType) =
    TermDocument(
        termType = type,
        version = "1.0",
        title = type.name,
        contentUrl = "https://laimory.app/terms/preview/1.0",
        effectiveAt = LocalDateTime.of(2026, 8, 28, 0, 0),
    )
