package com.soma369.laimory.feature.login.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.laimorySignature
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

    LoginScreen(
        innerPadding = innerPadding,
        state = state,
        onProviderClick = { provider -> onIntent(LoginUiIntent.ProviderClicked(provider)) },
    )
}

@Composable
private fun LoginScreen(
    innerPadding: PaddingValues,
    state: LoginUiState,
    onProviderClick: (SocialLoginProvider) -> Unit,
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
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(top = 120.dp, bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            LoginHeader()
            LoginActions(state = state, onProviderClick = onProviderClick)
            LegalNotice()
        }
    }
}

@Composable
private fun LoginHeader() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Laimory",
            style =
                MaterialTheme.laimorySignature.large.copy(
                    fontSize = 42.sp,
                    lineHeight = 61.sp,
                ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "일상을 기록하고, 기억으로 남겨요",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Life · AI · Memory",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun LoginActions(
    state: LoginUiState,
    onProviderClick: (SocialLoginProvider) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
        Text(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            text = "둘러보기",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            textDecoration = TextDecoration.Underline,
        )
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

@Composable
private fun SocialLoginButton(
    text: String,
    iconRes: Int,
    provider: SocialLoginProvider,
    state: LoginUiState,
    onClick: (SocialLoginProvider) -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        enabled = !state.isInteractionDisabled,
        onClick = { onClick(provider) },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
        ) {
            androidx.compose.foundation.Image(
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .size(32.dp),
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
            if (state.isInteractionDisabled && state.activeProvider == provider) {
                CircularProgressIndicator(
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun LegalNotice() {
    val linkStyle = SpanStyle(textDecoration = TextDecoration.Underline)
    Text(
        modifier = Modifier.fillMaxWidth(),
        text =
            buildAnnotatedString {
                append("로그인 시 라이모리의 ")
                pushStyle(linkStyle)
                append("이용약관")
                pop()
                append(" 및 ")
                pushStyle(linkStyle)
                append("개인정보 처리방침")
                pop()
                append("에\n동의하는 것으로 간주합니다.")
            },
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        textAlign = TextAlign.Center,
    )
}

@Preview(name = "Login Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun LoginLightPreview() {
    LaimoryTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LoginScreen(PaddingValues(), LoginUiState(), {})
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
            )
        }
    }
}

@Preview(name = "Login Dark", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun LoginDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            LoginScreen(PaddingValues(), LoginUiState(), {})
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
            )
        }
    }
}
