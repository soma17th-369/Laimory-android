package com.soma369.laimory.feature.home.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.soma369.laimory.core.ui.model.displayLabel
import com.soma369.laimory.core.ui.theme.Emotion
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.core.ui.theme.color
import com.soma369.laimory.core.ui.theme.containerColor
import com.soma369.laimory.feature.home.model.PastRecordUiModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Figma 지난 기록 카드(`Past record` 2497:1109)를 서버 DailyRecord 목록 데이터에 연결한 카드.
 *
 * 시안의 높이 92 는 **최소**로 둔다. 고정하면 큰 글꼴에서 문구가 잘린다 — 기본 배율에서는 한 줄
 * 문구든 두 줄이든 92 로 같아 시안과 맞고, 글꼴을 키우면 카드가 늘어난다.
 */
@Composable
internal fun PastRecordCard(
    record: PastRecordUiModel,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 92.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PastRecordThumbnail(record = record)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = record.recordDate.format(PAST_RECORD_DATE_FORMAT),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // 시안은 이모지가 아니라 8dp 점이다. 색은 캘린더 셀과 같은 emotion 팔레트를
                    // 쓴다 — 이모지 에셋 팔레트와는 다른 축이다.
                    Box(
                        modifier =
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                // 감정을 알 수 없는 기록도 자리를 지킨다. 점이 사라지면 줄이 흔들린다.
                                .background(record.emotion?.color() ?: MaterialTheme.colorScheme.outlineVariant)
                                // 색만으로는 읽히지 않으므로 스크린 리더가 읽을 이름을 붙인다.
                                .semantics { contentDescription = "감정 ${record.emotion.displayLabel()}" },
                    )
                }
                Text(
                    text = record.summary ?: "기록된 이벤트가 없어요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    // 시안은 두 줄까지 보여 준다. 한 줄로 자르면 대표 문구가 대부분 잘린다.
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 대표 영역. PHOTO → 감정 컨테이너 색 → 중립(surfaceVariant) 순으로 fallback한다. */
@Composable
private fun PastRecordThumbnail(record: PastRecordUiModel) {
    Surface(
        modifier = Modifier.size(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = record.emotion?.containerColor() ?: MaterialTheme.colorScheme.surfaceVariant,
    ) {
        record.photoUrl?.let { photoUrl ->
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
            )
        }
    }
}

private val PAST_RECORD_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일 E", Locale.KOREA)

@Preview(name = "사진 대표", showBackground = true)
@Composable
private fun PastRecordCardPhotoPreview() =
    PastRecordCardPreview(
        previewRecord(photoUrl = "https://cdn.example.com/photo.jpg"),
    )

@Preview(name = "감정 대표", showBackground = true)
@Composable
private fun PastRecordCardEmotionPreview() = PastRecordCardPreview(previewRecord(emotion = Emotion.CALM))

@Preview(name = "중립 대표", showBackground = true)
@Composable
private fun PastRecordCardNeutralPreview() = PastRecordCardPreview(previewRecord(emotion = null))

@Preview(name = "이벤트 없음", showBackground = true)
@Composable
private fun PastRecordCardEmptyEventPreview() =
    PastRecordCardPreview(
        previewRecord(emotion = null, summary = null),
    )

@Composable
private fun PastRecordCardPreview(record: PastRecordUiModel) {
    LaimoryTheme {
        PastRecordCard(record = record, onClick = {})
    }
}

private fun previewRecord(
    emotion: Emotion? = Emotion.JOY,
    summary: String? = "점심 · 파스타 · 성수동",
    photoUrl: String? = null,
) = PastRecordUiModel(
    dailyRecordId = 31L,
    recordDate = LocalDate.of(2026, 7, 22),
    emotion = emotion,
    summary = summary,
    photoUrl = photoUrl,
)
