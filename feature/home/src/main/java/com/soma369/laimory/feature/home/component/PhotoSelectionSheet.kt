package com.soma369.laimory.feature.home.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.soma369.laimory.core.ui.R
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.home.state.HomePhotoItem
import com.soma369.laimory.feature.home.state.HomeUiIntent
import com.soma369.laimory.feature.home.state.HomeUiState
import com.soma369.laimory.feature.home.state.MAX_PHOTO_SELECTION
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhotoSelectionSheet(
    state: HomeUiState,
    onIntent: (HomeUiIntent) -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val zone = remember { ZoneId.systemDefault() }
    val today = rememberToday(zone)
    val photosByDate =
        remember(state.availablePhotos, zone) {
            state.availablePhotos
                .groupBy { it.capturedAt.atZone(zone).toLocalDate() }
                .toSortedMap()
        }
    val isAllSelected =
        state.availablePhotos.isNotEmpty() &&
            state.pendingPhotoIds.size == minOf(state.availablePhotos.size, MAX_PHOTO_SELECTION)
    ModalBottomSheet(
        onDismissRequest = { onIntent(HomeUiIntent.DismissPhotoSheet) },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.extraLarge, vertical = Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.large),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
                Text(
                    text = "사진 선택",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text =
                        if (state.isPhotoAccessDenied) {
                            "사진 접근을 허용하지 않아 사진을 불러올 수 없어요."
                        } else {
                            "${state.timeRangeLabel()} 사이에 모은 사진만 표시해요."
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.isPhotoAccessLimited && !state.isPhotoAccessDenied) {
                    Column {
                        Text(
                            text = "기기에서 허용한 사진만 표시하고 있어요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        TextButton(
                            onClick = { onIntent(HomeUiIntent.RequestAdditionalPhotoAccess) },
                        ) {
                            Text("허용 사진 추가")
                        }
                    }
                }
            }

            if (state.isPhotoAccessDenied) {
                DeniedPhotoAccess(onOpenAppSettings = onOpenAppSettings)
            } else if (state.isPhotoLoading) {
                PhotoSelectionLoading()
            } else if (state.availablePhotos.isEmpty()) {
                EmptyPhotoSelection()
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${state.pendingPhotoIds.size}/${MAX_PHOTO_SELECTION}장 선택",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    TextButton(onClick = { onIntent(HomeUiIntent.ToggleAllPhotos) }) {
                        Text(if (isAllSelected) "전체 해제" else "전체 선택")
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 88.dp),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 88.dp, max = 400.dp),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                    verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                ) {
                    photosByDate.forEach { (date, photos) ->
                        item(
                            key = "date-$date",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            PhotoDateHeader(
                                date = date,
                                selectedDate = state.selectedDate,
                                today = today,
                                selectedCount = photos.count { it.mediaStoreId in state.pendingPhotoIds },
                                photoCount = photos.size,
                                onToggleAll = { onIntent(HomeUiIntent.TogglePhotoDate(date)) },
                            )
                        }
                        items(photos, key = HomePhotoItem::mediaStoreId) { photo ->
                            SelectablePhoto(
                                photo = photo,
                                selected = photo.mediaStoreId in state.pendingPhotoIds,
                                onClick = { onIntent(HomeUiIntent.TogglePhoto(photo.mediaStoreId)) },
                            )
                        }
                    }
                }
            }

            // 사진을 고를 수 있는 상태에서만 확정 버튼을 둔다. 거부 상태에서는 고를 것이 없어
            // 버튼이 있어도 누를 이유가 없다.
            if (!state.isPhotoAccessDenied) {
                Button(
                    onClick = { onIntent(HomeUiIntent.ConfirmPhotoSelection) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isPhotoLoading && state.pendingPhotoIds.isNotEmpty(),
                ) {
                    Text("${state.pendingPhotoIds.size}장으로 초안 만들기")
                }
            }

            // 0장 선택 후 확인과 나누어 둔다 — 아무것도 고르지 않은 확인이 실수인지 의도인지
            // 구분되지 않으면, 사진이 빠진 초안을 만들어 놓고 왜 비었는지 알 수 없다.
            TextButton(
                onClick = { onIntent(HomeUiIntent.ContinueWithoutPhotos) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("사진 없이 계속")
            }
            Spacer(modifier = Modifier.height(Spacing.large))
        }
    }
}

@Composable
private fun PhotoSelectionLoading() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PhotoDateHeader(
    date: LocalDate,
    selectedDate: LocalDate,
    today: LocalDate,
    selectedCount: Int,
    photoCount: Int,
    onToggleAll: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = photoDateLabel(date, selectedDate, today),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "$selectedCount/${photoCount}장 선택",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onToggleAll) {
            Text(if (selectedCount == photoCount) "이 날짜 해제" else "이 날짜 전체 선택")
        }
    }
}

/**
 * 사진 접근을 거부당했을 때의 안내.
 *
 * 이 자리를 비워 두면 초안 만들기를 눌렀는데 빈 시트가 뜨는 것으로 보인다 — 무엇이 막혔는지와
 * 어디서 풀 수 있는지를 함께 둔다.
 */
@Composable
private fun DeniedPhotoAccess(onOpenAppSettings: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.extraLarge2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ico_timeline_photo),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = "설정에서 사진 접근을 허용하면 그날 찍은 사진을 모아서 보여드려요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onOpenAppSettings) {
            Text("설정 열기")
        }
    }
}

@Composable
private fun EmptyPhotoSelection() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.extraLarge2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ico_timeline_photo),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = "선택한 범위에 사진이 없어요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SelectablePhoto(
    photo: HomePhotoItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = if (selected) "선택된 사진" else "선택되지 않은 사진",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (selected) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
            )
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.extraSmall)
                        .size(22.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

private fun photoDateLabel(
    date: LocalDate,
    selectedDate: LocalDate,
    today: LocalDate,
): String {
    val relation =
        when (date) {
            selectedDate -> if (date == today) "오늘" else "기준일"
            selectedDate.plusDays(1) -> "익일"
            else -> null
        }
    val dateLabel = "${date.monthValue}월 ${date.dayOfMonth}일"
    return relation?.let { "$dateLabel · $it" } ?: dateLabel
}
