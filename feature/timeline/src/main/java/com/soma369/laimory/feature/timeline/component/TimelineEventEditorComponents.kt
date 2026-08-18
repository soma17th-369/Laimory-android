package com.soma369.laimory.feature.timeline.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.ui.component.LaimorySelectField
import com.soma369.laimory.core.ui.component.LaimoryTextField
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.timeline.state.TimelineEventExistingPhoto
import com.soma369.laimory.feature.timeline.state.TimelineEventPendingPhoto
import com.soma369.laimory.feature.timeline.state.TimelineEventPhotoUploadState
import com.soma369.laimory.feature.timeline.state.TimelineEventTimeField
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.soma369.laimory.core.ui.R as UiR

@Composable
internal fun TimelineEventTypeSection(
    selectedType: TimelineEventType,
    enabled: Boolean,
    onSelect: (TimelineEventType) -> Unit,
    modifier: Modifier = Modifier,
) {
    TimelineEditorSection(title = "유형", modifier = modifier) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            items(
                items = TimelineEventTypeDisplayOrder,
                key = TimelineEventType::name,
            ) { eventType ->
                val selected = eventType == selectedType
                Column(
                    modifier =
                        Modifier
                            .width(EventTypeItemWidth)
                            .clickable(
                                enabled = enabled,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelect(eventType) },
                            ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                ) {
                    Surface(
                        modifier = Modifier.size(EventTypeCircleSize),
                        shape = CircleShape,
                        color =
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(eventType.iconResource()),
                                contentDescription = null,
                                modifier = Modifier.size(EventTypeIconSize),
                                tint =
                                    if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            )
                        }
                    }
                    Text(
                        text = eventType.displayLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (selected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
internal fun TimelineEditorTextSection(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    error: String?,
    placeholder: String,
    supportingText: String?,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    singleLine: Boolean = true,
    counter: String? = null,
) {
    LaimoryTextField(
        value = value,
        onValueChange = onValueChange,
        label = title,
        placeholder = placeholder,
        supportingText = supportingText,
        counterText = counter,
        error = error,
        enabled = enabled,
        singleLine = singleLine,
        fieldHeight = if (singleLine) SingleLineFieldHeight else MemoFieldHeight,
        focusRequester = focusRequester,
        modifier = modifier,
    )
}

@Composable
internal fun TimelineEventTimeSection(
    recordDate: LocalDate,
    startAt: LocalDateTime,
    endAt: LocalDateTime?,
    enabled: Boolean,
    error: String?,
    onOpenPicker: (TimelineEventTimeField) -> Unit,
    onClearEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TimelineEditorSection(title = "시간", modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimelineTimeField(
                text = summaryLabel(recordDate, startAt),
                label = "시작 시각",
                enabled = enabled,
                isActive = false,
                isError = error != null,
                onClick = { onOpenPicker(TimelineEventTimeField.START) },
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "~",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TimelineTimeField(
                text = endAt?.let { summaryLabel(recordDate, it) } ?: "없음",
                label = "종료 시각",
                enabled = enabled,
                isActive = false,
                isError = error != null,
                onClick = { onOpenPicker(TimelineEventTimeField.END) },
                modifier = Modifier.weight(1f),
            )
        }
        if (endAt != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onClearEnd, enabled = enabled) {
                    Text("종료 없음")
                }
            }
        }
        error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/**
 * 요약 필드에 표시할 시각.
 *
 * 가로로 나란히 두면 필드 하나가 화면 반쪽뿐이라 `(MM.dd) HH:mm`은 좁은 화면에서 잘린다.
 * 기록 날짜와 같으면 시각만, 다음 날이면 `익일`만 앞에 붙여 짧게 유지한다. 시트로는 고를 수 없는
 * 날짜는 서버 값에서만 올 수 있으므로 그때만 날짜를 그대로 보여준다.
 */
internal fun summaryLabel(
    recordDate: LocalDate,
    dateTime: LocalDateTime,
): String {
    val time = dateTime.format(SummaryTimeFormatter)
    return when (dateTime.toLocalDate()) {
        recordDate -> time
        recordDate.plusDays(1) -> "익일 $time"
        else -> "(${dateTime.format(SummaryDateFormatter)}) $time"
    }
}

@Composable
internal fun TimelineEventPhotoSection(
    existingPhotos: List<TimelineEventExistingPhoto>,
    pendingPhotos: List<TimelineEventPendingPhoto>,
    enabled: Boolean,
    onAddClick: () -> Unit,
    onRemoveExisting: (Long) -> Unit,
    onRemovePending: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TimelineEditorSection(title = "사진", modifier = modifier) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            items(
                items = existingPhotos,
                key = TimelineEventExistingPhoto::timelineItemId,
            ) { photo ->
                TimelineEditorPhoto(
                    model = photo.photoUrl,
                    contentDescription = "기존 이벤트 사진",
                    removeContentDescription = "이벤트에서 사진 제거",
                    onRemove =
                        if (enabled) {
                            { onRemoveExisting(photo.timelineItemId) }
                        } else {
                            null
                        },
                )
            }
            items(pendingPhotos, key = TimelineEventPendingPhoto::rawId) { photo ->
                TimelineEditorPhoto(
                    model = photo.clientPhotoUri,
                    contentDescription = "추가할 이벤트 사진",
                    uploadState = photo.uploadState,
                    removeContentDescription = "추가 대상에서 제외",
                    onRemove =
                        if (enabled) {
                            { onRemovePending(photo.rawId) }
                        } else {
                            null
                        },
                )
            }
            item(key = "add-photo") {
                Box(
                    modifier =
                        Modifier
                            .size(PhotoSize)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surface)
                            .dashedBorder(MaterialTheme.colorScheme.outlineVariant)
                            .clickable(enabled = enabled, onClick = onAddClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(UiR.drawable.ico_timeline_editor_add_photo),
                        contentDescription = "사진 추가",
                        modifier = Modifier.size(PhotoIconSize),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineEditorSection(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            trailing?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        content()
    }
}

@Composable
private fun TimelineTimeField(
    text: String,
    label: String,
    enabled: Boolean,
    isActive: Boolean,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaimorySelectField(
        value = text,
        onClick = onClick,
        // 필드 안에는 시각만 있어 화면을 못 보면 어느 쪽이 시작이고 종료인지 알 수 없다.
        modifier = modifier.semantics { contentDescription = "$label, $text" },
        enabled = enabled,
        isActive = isActive,
        isError = isError,
        fieldHeight = SingleLineFieldHeight,
        contentAlignment = Alignment.Center,
    )
}

@Composable
private fun TimelineEditorPhoto(
    model: String?,
    contentDescription: String,
    uploadState: TimelineEventPhotoUploadState? = null,
    removeContentDescription: String? = null,
    onRemove: (() -> Unit)? = null,
) {
    val isPreview = LocalInspectionMode.current
    Box(
        modifier =
            Modifier
                .size(PhotoSize)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .dashedBorder(MaterialTheme.colorScheme.outlineVariant),
    ) {
        if (!isPreview && model != null) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = painterResource(UiR.drawable.ico_timeline_editor_photo_placeholder),
                contentDescription = contentDescription,
                modifier =
                    Modifier
                        .size(PhotoIconSize)
                        .align(Alignment.Center),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (uploadState == TimelineEventPhotoUploadState.UPLOADING) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    strokeWidth = 2.dp,
                )
            }
        }
        if (uploadState == TimelineEventPhotoUploadState.FAILED) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "업로드 실패",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        onRemove?.let {
            Surface(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(22.dp),
                onClick = it,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.inverseSurface,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(UiR.drawable.ico_setting_trash),
                        contentDescription = removeContentDescription,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                }
            }
        }
    }
}

private fun Modifier.dashedBorder(color: Color): Modifier =
    drawWithCache {
        val strokeWidth = 1.dp.toPx()
        val radius = 12.dp.toPx()
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
        onDrawBehind {
            drawRoundRect(
                color = color,
                cornerRadius = CornerRadius(radius, radius),
                style =
                    Stroke(
                        width = strokeWidth,
                        pathEffect = pathEffect,
                    ),
            )
        }
    }

private val SummaryTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val SummaryDateFormatter = DateTimeFormatter.ofPattern("MM.dd")
private val EventTypeCircleSize = 44.dp
private val EventTypeIconSize = 24.dp
private val EventTypeItemWidth = 44.dp
private val SingleLineFieldHeight = 48.dp
private val MemoFieldHeight = 120.dp
private val PhotoSize = 64.dp
private val PhotoIconSize = 20.dp
