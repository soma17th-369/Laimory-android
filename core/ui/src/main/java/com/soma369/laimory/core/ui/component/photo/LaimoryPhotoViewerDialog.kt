package com.soma369.laimory.core.ui.component.photo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing

/**
 * 사진 여러 장을 좌우로 넘기며 크게 보는 전체 화면.
 *
 * 사진을 그리는 방법은 부르는 쪽이 [photo] 로 정한다 — 이미지 로더를 공용 UI 모듈에 들이지 않기 위해서다.
 * [bottomBar] 에는 지금 보고 있는 사진에 대한 동작(예: 선택하기)을 둔다. 사진 영역과 겹치지 않게 아래에
 * 따로 자리를 잡는다.
 */
@Composable
fun LaimoryPhotoViewerDialog(
    photoCount: Int,
    initialIndex: Int,
    onDismiss: () -> Unit,
    photo: @Composable (index: Int) -> Unit,
    bottomBar: @Composable (currentIndex: Int) -> Unit = {},
) {
    if (photoCount <= 0) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        LaimoryPhotoViewer(
            photoCount = photoCount,
            initialIndex = initialIndex,
            onDismiss = onDismiss,
            photo = photo,
            bottomBar = bottomBar,
        )
    }
}

@Composable
private fun LaimoryPhotoViewer(
    photoCount: Int,
    initialIndex: Int,
    onDismiss: () -> Unit,
    photo: @Composable (index: Int) -> Unit,
    bottomBar: @Composable (currentIndex: Int) -> Unit,
) {
    val pagerState =
        rememberPagerState(
            initialPage = initialIndex.coerceIn(0, photoCount - 1),
            pageCount = { photoCount },
        )

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(top = VIEWER_TOP_PADDING, bottom = Spacing.large),
                    contentAlignment = Alignment.Center,
                ) {
                    photo(page)
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.medium),
            ) {
                Text(
                    text = "닫기",
                    color = Color.White,
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.large)
                    .padding(bottom = Spacing.extraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Text(
                text = "${pagerState.currentPage + 1} / $photoCount",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
            bottomBar(pagerState.currentPage)
        }
    }
}

/** 닫기 버튼 자리를 사진이 덮지 않게 비워 두는 높이. */
private val VIEWER_TOP_PADDING = 72.dp

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun LaimoryPhotoViewerPreview() {
    LaimoryTheme {
        LaimoryPhotoViewer(
            photoCount = 5,
            initialIndex = 2,
            onDismiss = {},
            photo = {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(Spacing.extraLarge)
                            .background(Color.DarkGray),
                )
            },
            bottomBar = {},
        )
    }
}
