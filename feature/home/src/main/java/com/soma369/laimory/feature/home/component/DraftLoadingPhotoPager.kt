package com.soma369.laimory.feature.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

/**
 * 사진 뷰어의 페이지 수.
 *
 * 마지막에서 처음으로 돌아갈 때 전체를 거꾸로 되감지 않도록, 페이지를 아주 크게 두고 사진은
 * 나머지 연산으로 고른다. 사진이 한 장이면 넘길 것이 없어 한 페이지만 둔다.
 */
internal fun draftPhotoPageCount(photoCount: Int): Int = if (photoCount > 1) Int.MAX_VALUE else 1

/** 양방향으로 넘길 수 있도록 가운데에서 시작하되, 첫 화면이 첫 사진이 되도록 맞춘다. */
internal fun draftPhotoInitialPage(photoCount: Int): Int {
    if (photoCount <= 1) return 0
    val middle = draftPhotoPageCount(photoCount) / 2
    return middle - middle % photoCount
}

/** 페이지 번호가 가리키는 사진. */
internal fun draftPhotoIndexFor(
    page: Int,
    photoCount: Int,
): Int = if (photoCount <= 0) 0 else Math.floorMod(page, photoCount)

/**
 * 전송된 사진을 한 장씩 넘겨 보여준다.
 *
 * 3초마다 자동으로 넘어가되 사용자가 손으로 넘기는 동안에는 멈춘다. 손을 뗀 뒤 다시 같은 시간을
 * 기다렸다가 이어간다 — 보고 있는 사진을 뺏지 않기 위해서다.
 *
 * 양옆 사진이 살짝 보이고 가운데만 원래 크기로 그린다. 크기와 투명도는 [graphicsLayer] 안에서
 * 읽어 그리기 단계에서만 반영하므로, 넘기는 동안 레이아웃이 다시 돌지 않는다.
 */
@Composable
internal fun DraftLoadingPhotoPager(
    photoUris: List<String>,
    modifier: Modifier = Modifier,
) {
    if (photoUris.isEmpty()) return
    val pagerState =
        rememberPagerState(
            initialPage = draftPhotoInitialPage(photoUris.size),
            pageCount = { draftPhotoPageCount(photoUris.size) },
        )
    AutoAdvance(pagerState = pagerState, isEnabled = photoUris.size > 1)

    BoxWithConstraints(modifier = modifier) {
        // 사진은 정사각이라 높이를 폭에서 얻는다. Figma 크기를 최대값으로 두고 좁은 화면에서만
        // 줄인다 — 넓은 화면에서 키우면 사진이 흐려지고, 대신 양옆이 더 보인다.
        val pageSize = minOf(MaxPageSize, maxWidth - MinPeekWidth * 2)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.height(pageSize),
            contentPadding = PaddingValues(horizontal = (maxWidth - pageSize) / 2),
            pageSpacing = PageSpacing,
            pageSize = PageSize.Fixed(pageSize),
        ) { page ->
            DraftPhotoPage(
                uri = photoUris[draftPhotoIndexFor(page, photoUris.size)],
                distanceFromCurrent = { (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction },
            )
        }
    }
}

/**
 * 자동 넘김.
 *
 * 스크롤이 멈추길 기다린 뒤 한 박자 쉬고 넘긴다. 사용자가 손으로 넘긴 직후에도 같은 만큼 기다리므로
 * 손을 떼자마자 화면이 튀지 않는다.
 */
@Composable
private fun AutoAdvance(
    pagerState: PagerState,
    isEnabled: Boolean,
) {
    LaunchedEffect(pagerState, isEnabled) {
        if (!isEnabled) return@LaunchedEffect
        while (true) {
            snapshotFlow { pagerState.isScrollInProgress }.first { isScrolling -> !isScrolling }
            delay(ADVANCE_INTERVAL)
            if (pagerState.isScrollInProgress) continue
            pagerState.animateScrollToPage(pagerState.currentPage + 1)
        }
    }
}

@Composable
private fun DraftPhotoPage(
    uri: String,
    distanceFromCurrent: () -> Float,
) {
    val shape = RoundedCornerShape(PageCornerRadius)
    val pageModifier =
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                // 가운데에서 멀어질수록 작아지고 옅어진다. 값은 그리기 단계에서만 읽는다.
                val distance = distanceFromCurrent()
                val nearness = 1f - distance.absoluteValue.coerceIn(0f, 1f)
                val scale = lerp(NEIGHBOUR_SCALE, 1f, nearness)
                // 화면 안쪽으로 보이는 가장자리를 기준으로 줄인다.
                //
                // 가운데를 기준으로 줄이면 양옆 사진이 화면 밖으로 물러나 보이는 폭이 절반 아래로
                // 준다. 왼쪽 이웃은 오른쪽 가장자리가, 오른쪽 이웃은 왼쪽 가장자리가 보이는 쪽이다.
                transformOrigin = TransformOrigin(if (distance > 0f) 1f else 0f, 0.5f)
                scaleX = scale
                scaleY = scale
                alpha = lerp(NEIGHBOUR_ALPHA, 1f, nearness)
                clip = true
                this.shape = shape
            }

    if (LocalInspectionMode.current) {
        // Preview 에서는 실제 URI 를 읽을 수 없어 자리만 보여준다.
        Box(modifier = pageModifier.background(MaterialTheme.colorScheme.surfaceVariant))
        return
    }
    AsyncImage(
        model = uri,
        contentDescription = null,
        modifier = pageModifier,
        contentScale = ContentScale.Crop,
    )
}

/** 넘기는 간격. 사용자가 손으로 넘긴 뒤에도 같은 만큼 기다렸다가 이어간다. */
private val ADVANCE_INTERVAL = 3.seconds

/** 사진 한 장의 최대 크기. 정사각이라 높이도 같은 값을 쓴다. */
private val MaxPageSize = 216.dp

/** 양옆 사진이 최소한 이만큼은 보이도록 폭을 양보한다. */
private val MinPeekWidth = 48.dp
private val PageSpacing = 16.dp
private val PageCornerRadius = 16.dp

/** 양옆 사진의 축소율. Figma 의 200 대 216 비율이다. */
private const val NEIGHBOUR_SCALE = 200f / 216f
private const val NEIGHBOUR_ALPHA = 0.55f
