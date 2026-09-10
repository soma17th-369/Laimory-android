package com.soma369.laimory.feature.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * 사진 카드의 3×2 격자(Figma 2529:13315).
 *
 * **후보가 6장보다 적거나 0장이어도 격자 크기를 유지한다.** 빈 셀은 자리표시자로 채운다 — 장수에
 * 따라 카드 높이가 달라지면 아래 카드들이 매번 밀린다.
 *
 * `LazyVerticalGrid` 를 쓰지 않는다. 칸이 여섯으로 고정이라 지연 배치가 얻을 것이 없고, 세로
 * 스크롤 안에 세로 지연 격자를 넣으면 높이를 정할 수 없다.
 */
@Composable
internal fun HomePhotoGrid(
    photoUris: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CELL_GAP),
    ) {
        photoUris.take(MAX_CELLS).padToCells().chunked(COLUMNS).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CELL_GAP),
            ) {
                row.forEach { uri -> PhotoCell(uri = uri, modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PhotoCell(
    uri: String?,
    modifier: Modifier = Modifier,
) {
    val cell =
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    if (uri == null) {
        Box(modifier = cell)
        return
    }
    AsyncImage(
        model = uri,
        // 격자는 "무엇이 모였는지" 를 보여 주는 장식이다. 낱장마다 읽어 주면 카드 한 장을
        // 지나는 데 여섯 번을 듣게 된다 — 건수는 본문이 말한다.
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = cell,
    )
}

/** 빈 칸을 null 로 채워 항상 여섯 칸을 만든다. */
private fun List<String>.padToCells(): List<String?> = this + List(MAX_CELLS - size) { null }

private const val COLUMNS = 3
private const val MAX_CELLS = 6
private val CELL_GAP = 4.dp
