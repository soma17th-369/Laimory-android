package com.soma369.laimory.feature.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.soma369.laimory.feature.home.state.HOME_PHOTO_GRID_CELLS
import com.soma369.laimory.feature.home.state.HomePhotoCell

/**
 * 사진 카드의 3×2 격자(Figma 2529:13315).
 *
 * **후보가 칸 수보다 적거나 0장이어도 격자 크기를 유지한다.** 빈 셀은 자리표시자로 채운다 — 장수에
 * 따라 카드 높이가 달라지면 아래 카드들이 매번 밀린다.
 *
 * `LazyVerticalGrid` 를 쓰지 않는다. 칸이 여섯으로 고정이라 지연 배치가 얻을 것이 없고, 세로
 * 스크롤 안에 세로 지연 격자를 넣으면 높이를 정할 수 없다.
 */
@Composable
internal fun HomePhotoGrid(
    cells: List<HomePhotoCell>,
    modifier: Modifier = Modifier,
    emptyMessage: String? = null,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CELL_GAP),
        ) {
            cells.take(HOME_PHOTO_GRID_CELLS).padToCells().chunked(COLUMNS).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CELL_GAP),
                ) {
                    row.forEach { cell ->
                        PhotoCell(cell = cell, showsPlaceholder = emptyMessage == null, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        // 빈 칸만 여섯 개면 사진이 없는 건지 아직 못 불러온 건지 알 수 없다. 사진이 없으면 자리표시자 칸을
        // 그리지 않고 같은 크기의 빈 자리 가운데에 적는다 — 카드 높이는 사진이 있을 때와 같다.
        emptyMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier.padding(horizontal = CELL_GAP),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PhotoCell(
    cell: HomePhotoCell?,
    showsPlaceholder: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(4.dp)
    if (cell == null && !showsPlaceholder) {
        // 자리만 차지한다. 칸을 그리면 빈 문구 뒤에 회색 격자가 비쳐 보인다.
        Box(modifier = modifier.aspectRatio(1f))
        return
    }
    val base =
        modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    if (cell == null) {
        Box(modifier = base)
        return
    }
    Box(modifier = base) {
        AsyncImage(
            model = cell.uri,
            // 격자는 "무엇이 모였는지" 를 보여 주는 장식이다. 낱장마다 읽어 주면 카드 한 장을
            // 지나는 데 여섯 번을 듣게 된다 — 건수는 본문이 말한다.
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        if (!cell.isSelected) return@Box
        // 고른 것은 테두리와 파란 원 체크 배지로 함께 표시한다 — 테두리만 두면 파래지기만 해서 무엇을
        // 뜻하는지 읽히지 않는다. 사진 시트와 같은 배지다.
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .border(2.dp, MaterialTheme.colorScheme.primary, shape),
        )
        PhotoCheckBadge(
            selected = true,
            size = GRID_BADGE_SIZE,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(GRID_BADGE_MARGIN),
        )
    }
}

/** 빈 칸을 null 로 채워 항상 칸 수를 맞춘다. */
private fun List<HomePhotoCell>.padToCells(): List<HomePhotoCell?> = this + List(HOME_PHOTO_GRID_CELLS - size) { null }

private const val COLUMNS = 3
private val CELL_GAP = 4.dp
private val GRID_BADGE_SIZE = 18.dp
private val GRID_BADGE_MARGIN = 4.dp
