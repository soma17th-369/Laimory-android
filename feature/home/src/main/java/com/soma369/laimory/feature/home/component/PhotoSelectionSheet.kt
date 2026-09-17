package com.soma369.laimory.feature.home.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.soma369.laimory.core.ui.R
import com.soma369.laimory.core.ui.component.photo.LaimoryPhotoViewerDialog
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.home.state.HomePhotoItem
import com.soma369.laimory.feature.home.state.HomeUiIntent
import com.soma369.laimory.feature.home.state.HomeUiState
import com.soma369.laimory.feature.home.state.MAX_PHOTO_SELECTION
import com.soma369.laimory.feature.home.state.isInputLocked
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
    // 완성된 날은 모인 사진을 보여 주기만 한다. 고르는 조작을 감춰, 눌리는데 아무 일도 없는 버튼을 남기지 않는다.
    val isReadOnly = state.isInputLocked
    val zone = remember { ZoneId.systemDefault() }
    val today = rememberToday(zone)
    val photosByDate =
        remember(state.availablePhotos, zone) {
            state.availablePhotos
                .groupBy { it.capturedAt.atZone(zone).toLocalDate() }
                .toSortedMap()
        }
    // 크게 보는 사진의 순번. 격자에 보이는 순서(날짜별) 그대로 넘긴다.
    val orderedPhotos = remember(photosByDate) { photosByDate.values.flatten() }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
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
                        } else if (isReadOnly) {
                            "${state.timeRangeLabel()} 사이에 모은 사진이에요. 이미 만든 기록이라 선택은 바꿀 수 없고, 누르면 크게 볼 수 있어요."
                        } else {
                            // 꾹 누르기는 보이지 않는 조작이라 여기서 한 번 알린다.
                            "${state.timeRangeLabel()} 사이에 모은 사진만 표시해요. 길게 누르면 크게 볼 수 있어요."
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
                    if (!isReadOnly) {
                        TextButton(onClick = { onIntent(HomeUiIntent.ToggleAllPhotos) }) {
                            Text(if (isAllSelected) "전체 해제" else "전체 선택")
                        }
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
                                onToggleAll = if (isReadOnly) null else ({ onIntent(HomeUiIntent.TogglePhotoDate(date)) }),
                            )
                        }
                        items(photos, key = HomePhotoItem::mediaStoreId) { photo ->
                            SelectablePhoto(
                                photo = photo,
                                selected = photo.mediaStoreId in state.pendingPhotoIds,
                                onToggle = if (isReadOnly) null else ({ onIntent(HomeUiIntent.TogglePhoto(photo.mediaStoreId)) }),
                                onOpen = { viewerIndex = orderedPhotos.indexOf(photo).coerceAtLeast(0) },
                            )
                        }
                    }
                }
            }

            // 버튼은 하나이고 **문구가 상태를 말한다.** 0장일 때 버튼을 잠가 두면 왜 막혔는지
            // 알려 주지 못한다. 사진 없이 두는 것은 정상 경로이므로 문구로 분명히 한다.
            //
            // `만들기` 라 하지 않는다 — 이 시트는 고른 결과를 홈에 돌려주고 닫힐 뿐이고,
            // 만들기는 사용자가 홈 CTA 를 눌러 시작한다.
            val hasSelection = state.pendingPhotoIds.isNotEmpty()
            Button(
                onClick = {
                    onIntent(
                        when {
                            // 읽기 전용에서 확정하면 ViewModel 이 막아 시트가 닫히지 않는다. 닫기만 한다.
                            isReadOnly -> HomeUiIntent.DismissPhotoSheet
                            hasSelection -> HomeUiIntent.ConfirmPhotoSelection
                            else -> HomeUiIntent.ContinueWithoutPhotos
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isPhotoLoading,
                // M3 기본값은 모서리를 완전히 둥글리므로(stadium) 우리 버튼보다 훨씬 둥글다.
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    when {
                        isReadOnly -> "닫기"
                        hasSelection -> "${state.pendingPhotoIds.size}장 선택 완료"
                        else -> "사진 없이 닫기"
                    },
                )
            }
            Spacer(modifier = Modifier.height(Spacing.large))
        }
    }

    viewerIndex?.let { initialIndex ->
        LaimoryPhotoViewerDialog(
            photoCount = orderedPhotos.size,
            initialIndex = initialIndex,
            onDismiss = { viewerIndex = null },
            photo = { index ->
                AsyncImage(
                    model = orderedPhotos[index].uri,
                    contentDescription = "사진 ${index + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            },
            // 크게 보는 이유가 고를지 판단하려는 것이라, 닫고 칸을 다시 찾아 체크하게 두지 않는다. 격자와 같은
            // 파란 원 체크를 같은 우측 상단에 둔다 — 버튼 문구를 따로 두면 격자와 조작이 달라 어색하다.
            topEndAction =
                if (isReadOnly) {
                    null
                } else {
                    { index ->
                        orderedPhotos.getOrNull(index)?.let { photo ->
                            val isSelected = photo.mediaStoreId in state.pendingPhotoIds
                            Box(
                                modifier =
                                    Modifier
                                        .size(VIEWER_CHECK_TOUCH_SIZE)
                                        .toggleable(
                                            value = isSelected,
                                            role = Role.Checkbox,
                                            onValueChange = { onIntent(HomeUiIntent.TogglePhoto(photo.mediaStoreId)) },
                                        ).semantics { contentDescription = "사진 선택" },
                                contentAlignment = Alignment.Center,
                            ) {
                                PhotoCheckBadge(selected = isSelected, size = VIEWER_BADGE_SIZE)
                            }
                        }
                    }
                },
        )
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
    onToggleAll: (() -> Unit)?,
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
        if (onToggleAll != null) {
            TextButton(onClick = onToggleAll) {
                Text(if (selectedCount == photoCount) "이 날짜 해제" else "이 날짜 전체 선택")
            }
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
        Button(onClick = onOpenAppSettings, shape = MaterialTheme.shapes.large) {
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

/**
 * 사진 시트의 칸 하나. **누르면 선택, 길게 누르면 크게 보기다.**
 *
 * 시트에서 가장 자주 하는 일은 여러 장을 고르는 것이라 칸 어디를 눌러도 고르게 한다. 크게 보기는
 * 가끔 하는 일이라 길게 누르기에 둔다 — 안드로이드 기본 사진 선택기와 같은 조작이다. 우측 상단 원은
 * 선택 상태를 보여 주기만 한다.
 *
 * [onToggle] 이 null 이면(완성된 날) 고를 수 없으므로 누르기와 길게 누르기 모두 크게 보기다.
 */
@Composable
private fun SelectablePhoto(
    photo: HomePhotoItem,
    selected: Boolean,
    onToggle: (() -> Unit)?,
    onOpen: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(
                    onClickLabel = if (onToggle != null) "사진 선택" else "사진 크게 보기",
                    role = if (onToggle != null) Role.Checkbox else null,
                    onLongClickLabel = "사진 크게 보기",
                    onLongClick = onOpen,
                    onClick = onToggle ?: onOpen,
                ),
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
        }
        // 고를 수 있으면 빈 원까지 보여 고르는 칸임을 알린다. 완성된 날은 고른 것만 표시한다.
        if (onToggle != null || selected) {
            PhotoCheckBadge(
                selected = selected,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.extraSmall),
            )
        }
    }
}

/** 크게 보기의 체크. 화면이 큰 만큼 배지도 키운다. */
private val VIEWER_BADGE_SIZE = 28.dp
private val VIEWER_CHECK_TOUCH_SIZE = 48.dp

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
