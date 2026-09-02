package com.soma369.laimory.feature.onboarding.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soma369.laimory.core.ui.greeting.GreetingEmphasis
import com.soma369.laimory.core.ui.greeting.nicknameGreetingSegments
import com.soma369.laimory.core.ui.theme.LaimoryShapes
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
    /** 권한을 자세히 설명하는 시트를 여는 창구. 없으면 버튼을 그리지 않는다. */
    onDetailsClick: (() -> Unit)? = null,
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
        verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge),
    ) {
        // 라벨 줄은 높이를 고정하고 아래로 붙인다. 첫 장의 인사말(titleLarge)과 나머지 장의
        // 라벨(labelLarge)은 글자 크기가 달라, 그대로 두면 장마다 제목이 다른 높이에서 시작한다.
        Box(
            modifier = Modifier.fillMaxWidth().height(LABEL_SLOT_HEIGHT),
            contentAlignment = Alignment.BottomStart,
        ) {
            when {
                page.showsGreeting -> NicknameGreeting(nickname = nickname)
                page.brandLabel != null ->
                    Text(
                        text = page.brandLabel,
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = BRAND_LABEL_LETTER_SPACING),
                        color = MaterialTheme.colorScheme.primary,
                    )

                page.label != null ->
                    Text(
                        text = if (isGranted) "${page.label} · 연결됨" else page.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
            }
        }
        // 제목은 두 줄 자리를 늘 차지한다. 한 줄짜리 장에서 아래가 올라오면 넘길 때마다
        // 이미지와 설명이 위아래로 튄다.
        Text(
            text = page.title,
            modifier = Modifier.fillMaxWidth().heightIn(min = TITLE_SLOT_MIN_HEIGHT),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        // 이미지는 제목과 설명 사이에 온다 — 글을 다 읽기 전에 무엇을 연결하는 화면인지 보여 준다.
        // 아직 에셋이 없는 장은 자리를 접는다. 빈 상자를 남기면 이미지를 못 불러온 것처럼 보인다.
        page.image?.let { image ->
            if (page.scrollsImage) {
                AutoScrollingImage(
                    image = image,
                    viewportHeight = SCROLLING_IMAGE_HEIGHT,
                    imageWidth = SCROLLING_IMAGE_WIDTH,
                )
            } else {
                Image(
                    painter = painterResource(image),
                    contentDescription = null,
                    // 원본 비율 그대로 둔다. 예시 그림이라 잘리면 무엇을 보여 주는지 알 수 없다.
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth,
                )
            }
        }
        page.description?.takeIf(String::isNotBlank)?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // 무엇을 읽고 무엇을 읽지 않는지는 시트가 맡는다. 이 자리에 다 적으면 장마다 문단이
        // 길어져, 정작 읽어야 할 사람은 안 읽고 넘긴다.
        onDetailsClick?.let { onClick ->
            Text(
                modifier =
                    Modifier
                        .clip(LaimoryShapes.small)
                        .clickable(onClick = onClick)
                        .padding(vertical = Spacing.extraSmall),
                text = "무엇을 읽나요?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
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

/** 라벨 줄 높이. 인사말(20/28)이 들어가도 잘리지 않는 값이다. */
private val LABEL_SLOT_HEIGHT = 28.dp

/** 제목이 늘 차지하는 최소 높이. titleLarge 두 줄(28 x 2). */
private val TITLE_SLOT_MIN_HEIGHT = 56.dp

/** 흘려 보여 주는 그림의 창. 시안의 image-wrap 328x300 을 그대로 쓴다. 그림 자체는 이보다 길다. */
private val SCROLLING_IMAGE_HEIGHT = 300.dp

/** 창 안에서 그림이 차지하는 폭. 시안이 창(328)보다 좁게 두고 가운데 정렬한다. */
private val SCROLLING_IMAGE_WIDTH = 280.dp

/** 브랜드 라벨만 자간을 넓혀 권한 라벨과 결을 다르게 둔다. */
private val BRAND_LABEL_LETTER_SPACING = 0.4.sp
