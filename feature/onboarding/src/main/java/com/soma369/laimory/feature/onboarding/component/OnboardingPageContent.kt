package com.soma369.laimory.feature.onboarding.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.ui.greeting.GreetingEmphasis
import com.soma369.laimory.core.ui.greeting.nicknameGreetingSegments
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.onboarding.model.OnboardingPageSpec

/**
 * 온보딩 한 장의 본문.
 *
 * 어느 장이든 같은 골격(라벨 · 제목 · 설명 · 이미지)으로 그린다 — 장마다 배치를 달리하면
 * 목록에 항목을 더하는 것만으로는 새 장을 못 만든다.
 *
 * 세로로 스크롤되게 둔다. 큰 글꼴에서 설명이 길어지면 화면을 넘기는데, 고정 배치면 잘린다.
 */
@Composable
internal fun OnboardingPageContent(
    page: OnboardingPageSpec,
    nickname: String?,
    isGranted: Boolean = false,
    modifier: Modifier = Modifier,
    /** 설명 아래에 장이 따로 붙이는 것. 동의 장의 확인 목록이 여기로 들어온다. */
    extra: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.large)
                .padding(top = CONTENT_TOP_PADDING),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        if (page.showsGreeting) {
            NicknameGreeting(nickname = nickname)
        }
        page.label?.let { label ->
            Text(
                text = if (isGranted) "$label · 연결됨" else label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        page.image?.let { image ->
            Image(
                painter = painterResource(image),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
            )
        }
        extra?.invoke(this)
    }
}

/** 홈과 같은 규칙으로 조립한다. 같은 사용자를 두 화면이 다르게 부르면 안 된다. */
@Composable
private fun NicknameGreeting(nickname: String?) {
    val normalColor = MaterialTheme.colorScheme.onSurfaceVariant
    val nicknameColor = MaterialTheme.colorScheme.onSurface
    // 조각을 여러 Text 로 나누면 접근성 서비스가 따로 읽으므로 한 문장으로 합친다.
    val greeting =
        remember(nickname, normalColor, nicknameColor) {
            buildAnnotatedString {
                nicknameGreetingSegments(nickname).forEach { segment ->
                    val color =
                        when (segment.emphasis) {
                            GreetingEmphasis.NORMAL -> normalColor
                            GreetingEmphasis.NICKNAME -> nicknameColor
                        }
                    withStyle(SpanStyle(color = color)) { append(segment.text) }
                }
            }
        }
    Text(text = greeting, style = MaterialTheme.typography.titleLarge)
}

/**
 * 본문 시작 위치.
 *
 * 세로 가운데 정렬 대신 고정 여백을 쓴다 — 스크롤 가능한 열에서는 높이가 무한이라 weight 로
 * 가운데를 잡을 수 없고, 장마다 글이 길이가 달라 가운데 정렬이면 제목이 위아래로 흔들린다.
 */
private val CONTENT_TOP_PADDING = 48.dp
