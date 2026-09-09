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

/**
 * 하루를 대표하는 감정을 나타내는 원형 아이콘. Figma `Emoji-v2` 컴포넌트를 옮긴 것으로 홈·캘린더가 공유한다.
 *
 * 감정 5종은 원 배경·표정선·악센트가 에셋 한 장에 들어 있어 테마 색을 입히지 않는다 — 라이트와 다크가
 * 같은 색으로 보인다(구형도 감정 base 색이 테마별로 같았으므로 보이는 결과는 달라지지 않는다).
 *
 * [emotion] 이 null 이면 감정을 알 수 없는 상태로 보고 중립 물음표를 표시한다. **이때만** 원을 직접 깔고
 * 테마 색을 쓴다 — 미상은 색을 빌려 오는 자리가 아니라 비어 있음의 표시라, 다크에서 배경과 함께 어두워져야
 * 한다. 기록 자체가 없는 날은 이 컴포넌트를 그리지 않는 것이 호출부 책임이다 — "감정 미상"과 "기록 없음"은
 * 다른 상태다.
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
                    // 감정 5종은 원이 에셋 안에 있다. 미상만 중립 톤 원을 직접 깐다.
                    color = if (emotion == null) MaterialTheme.colorScheme.outline else Color.Transparent,
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
        // 신형 글리프 5종이 먹선이라 물음표도 같은 무게로 맞춘다. 원과 함께 테마를 따라야 해 토큰으로 둔다.
        color = MaterialTheme.colorScheme.onSurface,
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
     * Figma `Emoji-v2` 가 그려지는 지름.
     *
     * 신형은 원 배경까지 포함한 36x36 프레임이라 프레임 크기가 곧 눈에 보이는 크기다. 구형 `MoodEmoji` 는
     * 24dp 프레임을 원이 넘겨 36dp 로 렌더되던 것을 실측으로 맞춰 뒀는데, 그 어긋남이 사라졌다.
     */
    val Size: Dp = 36.dp

    /** 목록 카드처럼 한 줄 안에 얹는 자리용 축소 크기. */
    val CompactSize: Dp = 24.dp
}

private const val NEUTRAL_MARK_RATIO = 0.61f

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

@Preview(name = "감정 5종 + 중립 · 축소", showBackground = true)
@Composable
private fun EmotionIconCompactPreview() {
    LaimoryTheme {
        Row(
            modifier = Modifier.padding(Spacing.large),
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Emotion.entries.forEach { emotion ->
                EmotionIcon(emotion = emotion, size = EmotionIconDefaults.CompactSize)
            }
            EmotionIcon(emotion = null, size = EmotionIconDefaults.CompactSize)
        }
    }
}
