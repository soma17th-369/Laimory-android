package com.soma369.laimory.feature.home.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.soma369.laimory.core.ui.component.EmotionIcon
import com.soma369.laimory.core.ui.component.EmotionIconDefaults
import com.soma369.laimory.core.ui.theme.Emotion
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.core.ui.theme.containerColor
import com.soma369.laimory.feature.home.model.PastRecordUiModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Figma 홈 지난 기록 카드(680:1831)를 서버 DailyRecord 목록 데이터에 연결한 카드. */
@Composable
internal fun PastRecordCard(
    record: PastRecordUiModel,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
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
                    // 감정을 알 수 없는 기록도 중립 아이콘으로 표시한다 — 캘린더 셀과 같은 공통 컴포넌트.
                    EmotionIcon(emotion = record.emotion, size = EmotionIconDefaults.CompactSize)
                }
                Text(
                    text = record.summary ?: "기록된 이벤트가 없어요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
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
