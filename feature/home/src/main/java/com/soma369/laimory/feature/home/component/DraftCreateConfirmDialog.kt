package com.soma369.laimory.feature.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.soma369.laimory.core.ui.component.LaimoryDialog
import com.soma369.laimory.core.ui.component.LaimoryDialogButtons
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
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
 */
@Composable
internal fun DraftCreateConfirmDialog(
    confirm: DraftCreateConfirm,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
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
        PhotoSection(photoUris = confirm.photoUris)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            confirm.counts.forEach { CountTile(count = it, modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun PhotoSection(photoUris: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            SourceIcon(iconRes = HomeSourceKind.PHOTO.iconRes)
            Text(
                text = "사진 ${photoUris.size}${DraftConsentTypeGroup.PHOTO.countUnit}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
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
                Text(
                    text = "사진을 선택하시면 풍성한 타임라인을 얻을 수 있어요",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            // 낱장을 읽어 주지 않는다 — 몇 장인지는 위 줄이 말한다.
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                items(photoUris) { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .size(THUMBNAIL_SIZE)
                                .clip(RoundedCornerShape(THUMBNAIL_CORNER))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }
            }
        }
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
        )
    }
}
