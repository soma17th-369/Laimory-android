package com.soma369.laimory.core.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.ui.R
import com.soma369.laimory.core.ui.theme.Emotion
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.core.ui.theme.color

/**
 * 하루를 대표하는 감정을 나타내는 원형 아이콘. Figma `MoodEmoji` 컴포넌트를 옮긴 것으로 홈·캘린더가 공유한다.
 *
 * [emotion] 이 null 이면 감정을 알 수 없는 상태로 보고 중립 물음표를 표시한다. 기록 자체가 없는 날은
 * 이 컴포넌트를 그리지 않는 것이 호출부 책임이다 — "감정 미상"과 "기록 없음"은 다른 상태다.
 *
 * @param contentDescription null 이면 접근성 트리에서 제외한다(날짜 셀처럼 부모가 설명을 소유하는 경우).
 */
@Composable
fun EmotionIcon(
    emotion: Emotion?,
    modifier: Modifier = Modifier,
    size: Dp = EmotionIconDefaults.Size,
    contentDescription: String? = null,
) {
    Box(
        modifier =
            modifier
                .size(size)
                .background(
                    // 감정 미상은 팔레트 색을 빌려 쓰지 않고 중립 톤으로 떨어뜨린다.
                    color = emotion?.color() ?: MaterialTheme.colorScheme.outline,
                    shape = CircleShape,
                ).clearAndSetSemantics {
                    contentDescription?.let { this.contentDescription = it }
                },
        contentAlignment = Alignment.Center,
    ) {
        if (emotion == null) {
            NeutralEmotionMark(size = size)
        } else {
            Image(
                painter = painterResource(emotion.glyphRes()),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** 감정을 알 수 없을 때의 물음표. 원 지름에 비례해 커져야 크기를 바꿔도 균형이 유지된다. */
@Composable
private fun NeutralEmotionMark(size: Dp) {
    val markSize = with(LocalDensity.current) { (size * NEUTRAL_MARK_RATIO).toSp() }
    Text(
        text = "?",
        style =
            MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = markSize,
                lineHeight = markSize,
            ),
        // 글리프 5종과 같은 흰색 계열 — 원 배경 위에 얹히는 표정선이라 테마 색으로 갈아끼우지 않는다.
        color = Color.White.copy(alpha = NEUTRAL_MARK_ALPHA),
    )
}

@DrawableRes
private fun Emotion.glyphRes(): Int =
    when (this) {
        Emotion.JOY -> R.drawable.ico_emotion_joy
        Emotion.CALM -> R.drawable.ico_emotion_calm
        Emotion.MELLOW -> R.drawable.ico_emotion_mellow
        Emotion.WEARY -> R.drawable.ico_emotion_weary
        Emotion.DOWN -> R.drawable.ico_emotion_down
    }

object EmotionIconDefaults {
    /**
     * Figma `MoodEmoji` 가 실제로 그려지는 지름.
     *
     * 컴포넌트 프레임은 24dp 지만 안쪽 원이 프레임을 넘겨 36dp 로 렌더된다(캘린더 셀 실측 기준).
     * 레이아웃 슬롯이 아니라 눈에 보이는 크기를 정본으로 삼는다.
     */
    val Size: Dp = 36.dp

    /** 목록 카드처럼 한 줄 안에 얹는 자리용 축소 크기. */
    val CompactSize: Dp = 24.dp
}

private const val NEUTRAL_MARK_RATIO = 0.61f
private const val NEUTRAL_MARK_ALPHA = 0.7f

@Preview(name = "감정 5종 + 중립", showBackground = true)
@Composable
private fun EmotionIconPreview() {
    LaimoryTheme {
        Row(
            modifier = Modifier.padding(Spacing.large),
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Emotion.entries.forEach { emotion -> EmotionIcon(emotion = emotion) }
            EmotionIcon(emotion = null)
        }
    }
}

@Preview(name = "감정 5종 + 중립 · 다크", showBackground = true, backgroundColor = 0xFF13110E)
@Composable
private fun EmotionIconDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        Row(
            modifier = Modifier.padding(Spacing.large),
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Emotion.entries.forEach { emotion -> EmotionIcon(emotion = emotion) }
            EmotionIcon(emotion = null)
        }
    }
}
