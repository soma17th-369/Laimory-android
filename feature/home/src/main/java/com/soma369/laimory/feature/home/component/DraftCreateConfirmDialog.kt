package com.soma369.laimory.feature.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.soma369.laimory.core.ui.component.LaimoryDialog
import com.soma369.laimory.core.ui.component.LaimoryDialogButtons
import com.soma369.laimory.core.ui.component.photo.LaimoryPhotoViewerDialog
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.core.ui.theme.laimoryColors
import com.soma369.laimory.feature.home.state.DraftConsentTypeGroup
import com.soma369.laimory.feature.home.state.DraftCreateConfirm
import com.soma369.laimory.feature.home.state.DraftCreateConfirmCount
import com.soma369.laimory.feature.home.state.HomeSourceKind
import com.soma369.laimory.core.ui.R as UiR

/**
 * 타임라인 만들기 확인 다이얼로그.
 *
 * 사진은 고른 것을 한 줄로 넘겨 보이고, 나머지는 칸마다 건수만 적는다. 사진이 없으면 썸네일 줄 자리에
 * 같은 높이의 안내 칸을 둔다 — 사진 유무로 높이가 바뀌면 `만들기` 가 손가락 밑에서 움직인다.
 *
 * 여기서 사진을 고르러 갈 수 있다(Figma `Home / Timeline confirm dialog` 2854:1400). 0장이면 안내 아래
 * `사진 고르기`, 고른 사진이 있으면 머리 줄 오른쪽 `사진 바꾸기`. 둘 다 [onPickPhotos] 로 사진 시트를 열고,
 * 시트를 닫으면 이 다이얼로그가 새 사진으로 다시 뜬다.
 */
@Composable
internal fun DraftCreateConfirmDialog(
    confirm: DraftCreateConfirm,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onPickPhotos: () -> Unit,
) {
    LaimoryDialog(
        title = "타임라인을 만들까요?",
        buttons =
            LaimoryDialogButtons.Two(
                secondaryLabel = "취소",
                onSecondaryClick = onDismiss,
                primaryLabel = "만들기",
                onPrimaryClick = onConfirm,
            ),
        onDismissRequest = onDismiss,
    ) {
        PhotoSection(photoUris = confirm.photoUris, onPickPhotos = onPickPhotos)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            confirm.counts.forEach { CountTile(count = it, modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun PhotoSection(
    photoUris: List<String>,
    onPickPhotos: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            SourceIcon(iconRes = HomeSourceKind.PHOTO.iconRes)
            Text(
                text = "사진 ${photoUris.size}${DraftConsentTypeGroup.PHOTO.countUnit}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (photoUris.isNotEmpty()) PickPhotosLink(label = "사진 바꾸기", onClick = onPickPhotos)
        }
        if (photoUris.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(THUMBNAIL_SIZE)
                        .clip(RoundedCornerShape(THUMBNAIL_CORNER))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = Spacing.medium),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                ) {
                    Text(
                        text = "사진을 선택하시면 풍성한 타임라인을 얻을 수 있어요",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    PickPhotosLink(label = "사진 고르기", onClick = onPickPhotos)
                }
            }
        } else {
            PhotoStrip(photoUris = photoUris)
        }
    }
}

/** 사진 시트로 가는 글자 버튼. 안내 칸(64) 안에 들어가야 해서 버튼 최소 높이를 쓰지 않는다. */
@Composable
private fun PickPhotosLink(
    label: String,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        modifier =
            Modifier
                .clip(RoundedCornerShape(Spacing.small))
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = Spacing.extraSmall, vertical = 2.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.laimoryColors.primaryText,
    )
}

/**
 * 고른 사진 한 줄. 누르거나 꾹 누르면 크게 본다 — 64 칸으로는 무슨 사진인지 가늠이 안 된다.
 *
 * 뷰어는 이 다이얼로그 안에서 여는 별도 창이라 확인 다이얼로그 위에 뜨고, 닫으면 다이얼로그로 돌아온다.
 */
@Composable
private fun PhotoStrip(photoUris: List<String>) {
    var viewerIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        itemsIndexed(photoUris) { index, uri ->
            AsyncImage(
                model = uri,
                contentDescription = "사진 ${index + 1}",
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(THUMBNAIL_SIZE)
                        .clip(RoundedCornerShape(THUMBNAIL_CORNER))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .combinedClickable(
                            onClickLabel = "사진 크게 보기",
                            onLongClickLabel = "사진 크게 보기",
                            onLongClick = { viewerIndex = index },
                            onClick = { viewerIndex = index },
                        ),
            )
        }
    }
    viewerIndex?.let { initialIndex ->
        LaimoryPhotoViewerDialog(
            photoCount = photoUris.size,
            initialIndex = initialIndex,
            onDismiss = { viewerIndex = null },
            photo = { index ->
                AsyncImage(
                    model = photoUris[index],
                    contentDescription = "사진 ${index + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            },
        )
    }
}

/** 칸 하나를 `일정 2개` 한 마디로 읽힌다. 아이콘·제목·숫자를 따로 읽으면 세 번 멈춘다. */
@Composable
private fun CountTile(
    count: DraftCreateConfirmCount,
    modifier: Modifier = Modifier,
) {
    val label = count.group.label()
    val value = "${count.count}${count.group.countUnit}"
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(TILE_CORNER))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = Spacing.small, vertical = Spacing.medium)
                .clearAndSetSemantics { contentDescription = "$label $value" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TILE_GAP),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
        ) {
            SourceIcon(iconRes = count.group.homeIconRes())
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SourceIcon(iconRes: Int) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        modifier = Modifier.size(ICON_SIZE),
        tint = MaterialTheme.colorScheme.onSurface,
    )
}

/** 홈 카드와 같은 글리프. 건강은 카드가 없어 설정 화면의 것을 쓴다. */
private fun DraftConsentTypeGroup.homeIconRes(): Int =
    when (this) {
        DraftConsentTypeGroup.PHOTO -> HomeSourceKind.PHOTO.iconRes
        DraftConsentTypeGroup.CALENDAR -> HomeSourceKind.CALENDAR.iconRes
        DraftConsentTypeGroup.LOCATION -> HomeSourceKind.LOCATION.iconRes
        DraftConsentTypeGroup.NOTIFICATION -> HomeSourceKind.NOTIFICATION.iconRes
        DraftConsentTypeGroup.HEALTH -> UiR.drawable.ico_setting_datasource_health
    }

private val THUMBNAIL_SIZE = 64.dp
private val THUMBNAIL_CORNER = 8.dp
private val TILE_CORNER = 12.dp
private val TILE_GAP = 6.dp
private val ICON_SIZE = 20.dp

private val PREVIEW_COUNTS =
    listOf(
        DraftCreateConfirmCount(DraftConsentTypeGroup.CALENDAR, 2),
        DraftCreateConfirmCount(DraftConsentTypeGroup.LOCATION, 8),
        DraftCreateConfirmCount(DraftConsentTypeGroup.NOTIFICATION, 17),
    )

@Preview
@Composable
private fun DraftCreateConfirmDialogPreview() {
    LaimoryTheme {
        DraftCreateConfirmDialog(
            confirm = DraftCreateConfirm(photoUris = List(5) { "" }, counts = PREVIEW_COUNTS),
            onConfirm = {},
            onDismiss = {},
            onPickPhotos = {},
        )
    }
}

@Preview
@Composable
private fun DraftCreateConfirmDialogEmptyPreview() {
    LaimoryTheme {
        DraftCreateConfirmDialog(
            confirm = DraftCreateConfirm(photoUris = emptyList(), counts = PREVIEW_COUNTS),
            onConfirm = {},
            onDismiss = {},
            onPickPhotos = {},
        )
    }
}
