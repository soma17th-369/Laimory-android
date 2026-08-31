package com.soma369.laimory.feature.login.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermLinks
import com.soma369.laimory.core.ui.terms.appendTermLink
import com.soma369.laimory.core.ui.terms.rememberTermContentLauncher
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.core.ui.theme.laimorySignature
import com.soma369.laimory.feature.login.R
import com.soma369.laimory.feature.login.state.LoginPhase
import com.soma369.laimory.feature.login.state.LoginUiIntent
import com.soma369.laimory.feature.login.state.LoginUiSideEffect
import com.soma369.laimory.feature.login.state.LoginUiState
import com.soma369.laimory.feature.login.viewmodel.LoginViewModel
import kotlinx.coroutines.flow.Flow
import com.soma369.laimory.core.ui.R as CoreUiR

@Composable
fun LoginRoute(
    innerPadding: PaddingValues,
    onOpenAuthorizationUrl: (String) -> Boolean,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LoginContent(
        innerPadding = innerPadding,
        state = state,
        onIntent = viewModel::sendIntent,
        sideEffectFlow = viewModel.sideEffect,
        onOpenAuthorizationUrl = onOpenAuthorizationUrl,
    )
}

@Composable
private fun LoginContent(
    innerPadding: PaddingValues,
    state: LoginUiState,
    onIntent: (LoginUiIntent) -> Unit,
    sideEffectFlow: Flow<LoginUiSideEffect>,
    onOpenAuthorizationUrl: (String) -> Boolean,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var browserWasOpened by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        onIntent(LoginUiIntent.RefreshTermLinks)
    }

    LaunchedEffect(sideEffectFlow) {
        sideEffectFlow.collect { effect ->
            when (effect) {
                is LoginUiSideEffect.OpenAuthorizationUrl -> {
                    browserWasOpened = true
                    if (!onOpenAuthorizationUrl(effect.url)) {
                        browserWasOpened = false
                        onIntent(LoginUiIntent.AuthorizationLaunchFailed)
                    }
                }
            }
        }
    }

    // browserWasOpened를 key로 쓰면 RESUMED 상태에서 재등록된 observer가 즉시 ON_RESUME을 받아
    // Custom Tab을 열자마자 취소로 오인할 수 있으므로 lifecycleOwner가 바뀔 때만 등록한다.
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && browserWasOpened) {
                    browserWasOpened = false
                    onIntent(LoginUiIntent.BrowserReturnedWithoutCallback)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val termContentLauncher = rememberTermContentLauncher()

    LoginScreen(
        innerPadding = innerPadding,
        state = state,
        onProviderClick = { provider -> onIntent(LoginUiIntent.ProviderClicked(provider)) },
        onOpenTerm = { document -> termContentLauncher.open(document.contentUrl) },
    )
}

@Composable
private fun LoginScreen(
    innerPadding: PaddingValues,
    state: LoginUiState,
    onProviderClick: (SocialLoginProvider) -> Unit,
    onOpenTerm: (TermDocument) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    // 작은 화면이나 글자 확대에서 브랜드·버튼·약관이 잘리지 않도록 세로로 흘려 보낸다.
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.extraLarge3)
                    .padding(top = BrandTopPadding, bottom = ContentBottomPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge2),
        ) {
            LoginHeader()
            LoginActions(state = state, onProviderClick = onProviderClick)
            LegalNotice(links = state.termLinks, onOpenTerm = onOpenTerm)
        }
    }
}

@Composable
private fun LoginHeader() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = Spacing.extraLarge, bottom = BrandBottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        Image(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = WordmarkMaxHeight),
            painter = painterResource(R.drawable.img_laimory_wordmark),
            // 워드마크가 곧 앱 이름이라 스크린 리더에는 이름으로 읽힌다.
            contentDescription = "Laimory",
            contentScale = ContentScale.Fit,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
        ) {
            Text(
                text = "일상을 기록하고, 기억으로 남겨요",
                style = MaterialTheme.laimorySignature.small,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Tagline()
        }
    }
}

/**
 * `Life · AI · Memory` 태그라인.
 *
 * Figma 는 머리글자(L·AI·M)만 본문 색으로 올려 대비를 준다 — 한 문장으로 읽히도록 여러 Text 로
 * 쪼개지 않고 부분 스타일로 구성한다.
 */
@Composable
private fun Tagline() {
    val emphasisColor = MaterialTheme.colorScheme.onSurface
    Text(
        text =
            buildAnnotatedString {
                withStyle(SpanStyle(color = emphasisColor)) { append("L") }
                append("ife · ")
                withStyle(SpanStyle(color = emphasisColor)) { append("AI") }
                append(" · ")
                withStyle(SpanStyle(color = emphasisColor)) { append("M") }
                append("emory")
            },
        style = MaterialTheme.laimorySignature.note.copy(fontWeight = FontWeight.Bold, fontSize = TaglineFontSize),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = SUBTLE_TEXT_ALPHA),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun LoginActions(
    state: LoginUiState,
    onProviderClick: (SocialLoginProvider) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        SocialLoginButton(
            text = "구글로 시작하기",
            iconRes = CoreUiR.drawable.ico_social_google_logo,
            provider = SocialLoginProvider.GOOGLE,
            state = state,
            onClick = onProviderClick,
        )
        SocialLoginButton(
            text = "카카오로 시작하기",
            iconRes = CoreUiR.drawable.ico_social_kakao_logo,
            provider = SocialLoginProvider.KAKAO,
            state = state,
            onClick = onProviderClick,
        )
        // 버튼 아래 자리는 비워 두었다가 실패 시 사유가 들어간다. 높이를 미리 잡아 두면 오류가
        // 떠도 버튼과 약관 안내가 밀리지 않는다.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = ErrorSlotMinHeight),
            contentAlignment = Alignment.Center,
        ) {
            state.errorMessage?.let { message ->
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SocialLoginButton(
    text: String,
    iconRes: Int,
    provider: SocialLoginProvider,
    state: LoginUiState,
    onClick: (SocialLoginProvider) -> Unit,
) {
    val isInProgress = state.isInteractionDisabled && state.activeProvider == provider
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = SocialButtonMinHeight)
                // 진행 표시는 그림이라 낭독되지 않는다. 어느 버튼이 진행 중인지 상태로 알린다.
                .semantics { if (isInProgress) stateDescription = "로그인 진행 중" },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(SocialButtonBorderWidth, MaterialTheme.colorScheme.outlineVariant),
        enabled = !state.isInteractionDisabled,
        onClick = { onClick(provider) },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.extraLarge),
        ) {
            Image(
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .size(SocialIconSize),
                painter = painterResource(iconRes),
                contentDescription = null,
            )
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            if (isInProgress) {
                CircularProgressIndicator(
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .size(ProgressIndicatorSize),
                    strokeWidth = ProgressStrokeWidth,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * 약관 안내.
 *
 * 이 문구가 동의를 기록하지는 않는다. 실제 등록은 로그인 뒤 약관 화면의 CTA 가
 * `(termType, version)` 을 서버에 보내면서 이뤄진다 — 서버가 그 기록 없이는 인증 API 대부분을
 * 막으므로 문구만으로는 앱을 쓸 수 없다.
 */
@Composable
private fun LegalNotice(
    links: TermLinks,
    onOpenTerm: (TermDocument) -> Unit,
) {
    val linkStyle = SpanStyle(color = MaterialTheme.colorScheme.onSurface)
    Text(
        modifier = Modifier.fillMaxWidth(),
        text =
            buildAnnotatedString {
                append("로그인 시 라이모리의 ")
                appendTermLink("이용약관", links.termsOfService, linkStyle, onOpenTerm)
                append(" 및 ")
                appendTermLink("개인정보 처리방침", links.privacyPolicy, linkStyle, onOpenTerm)
                append("에\n동의하고 만 14세 이상임을 확인하는 것으로 간주합니다.")
            },
        style = MaterialTheme.typography.bodySmall.copy(fontSize = LegalFontSize, lineHeight = LegalLineHeight),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = SUBTLE_TEXT_ALPHA),
        textAlign = TextAlign.Center,
    )
}

/** Figma 상단 여백. spacing 토큰 밖의 화면 전용 값이라 이름을 붙여 둔다. */
private val BrandTopPadding = 120.dp
private val BrandBottomPadding = 40.dp
private val ContentBottomPadding = 60.dp
private val WordmarkMaxHeight = 120.dp
private val SocialButtonMinHeight = 56.dp
private val SocialIconSize = 32.dp
private val ProgressIndicatorSize = 20.dp
private val ProgressStrokeWidth = 2.dp
private val SocialButtonBorderWidth = 1.5.dp

/** 로그인 실패 문구가 들어갈 자리. 비어 있어도 높이를 잡아 레이아웃이 밀리지 않게 한다. */
private val ErrorSlotMinHeight = 41.dp

private val TaglineFontSize = 14.sp
private val LegalFontSize = 12.sp
private val LegalLineHeight = 18.sp

/** Figma 의 보조 문구 투명도(60%). */
private const val SUBTLE_TEXT_ALPHA = 0.6f

@Preview(name = "Login Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun LoginLightPreview() {
    LaimoryTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LoginScreen(PaddingValues(), LoginUiState(), {}, {})
        }
    }
}

@Preview(name = "Login Progress Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun LoginProgressLightPreview() {
    LaimoryTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LoginScreen(
                innerPadding = PaddingValues(),
                state =
                    LoginUiState(
                        phase = LoginPhase.WAITING_CALLBACK,
                        activeProvider = SocialLoginProvider.GOOGLE,
                    ),
                onProviderClick = {},
                onOpenTerm = {},
            )
        }
    }
}

@Preview(name = "Login Failure Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun LoginFailureLightPreview() {
    LaimoryTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LoginScreen(
                innerPadding = PaddingValues(),
                state = LoginUiState(errorMessage = "소셜 로그인을 완료하지 못했습니다. 다시 시도해 주세요."),
                onProviderClick = {},
                onOpenTerm = {},
            )
        }
    }
}

@Preview(name = "Login 작은 화면", showBackground = true, widthDp = 320, heightDp = 600)
@Composable
private fun LoginCompactPreview() {
    LaimoryTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LoginScreen(PaddingValues(), LoginUiState(), {}, {})
        }
    }
}

@Preview(name = "Login 큰 글자", showBackground = true, widthDp = 360, heightDp = 800, fontScale = 1.5f)
@Composable
private fun LoginLargeFontPreview() {
    LaimoryTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LoginScreen(
                innerPadding = PaddingValues(),
                state = LoginUiState(errorMessage = "소셜 로그인을 완료하지 못했습니다. 다시 시도해 주세요."),
                onProviderClick = {},
                onOpenTerm = {},
            )
        }
    }
}

@Preview(name = "Login Dark", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun LoginDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            LoginScreen(PaddingValues(), LoginUiState(), {}, {})
        }
    }
}

@Preview(name = "Login Progress Dark", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun LoginProgressDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            LoginScreen(
                innerPadding = PaddingValues(),
                state =
                    LoginUiState(
                        phase = LoginPhase.WAITING_CALLBACK,
                        activeProvider = SocialLoginProvider.KAKAO,
                    ),
                onProviderClick = {},
                onOpenTerm = {},
            )
        }
    }
}

@Preview(name = "Login Failure Dark", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun LoginFailureDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            LoginScreen(
                innerPadding = PaddingValues(),
                state = LoginUiState(errorMessage = "소셜 로그인을 완료하지 못했습니다. 다시 시도해 주세요."),
                onProviderClick = {},
                onOpenTerm = {},
            )
        }
    }
}
