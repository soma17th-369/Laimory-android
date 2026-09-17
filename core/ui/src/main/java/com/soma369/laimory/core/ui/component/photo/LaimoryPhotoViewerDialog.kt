package com.soma369.laimory.core.ui.component.photo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.soma369.laimory.core.ui.R
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing

/**
 * 사진 여러 장을 좌우로 넘기며 크게 보는 전체 화면.
 *
 * 사진을 그리는 방법은 부르는 쪽이 [photo] 로 정한다 — 이미지 로더를 공용 UI 모듈에 들이지 않기 위해서다.
 *
 * 닫기는 좌측 상단, 지금 보는 사진에 대한 동작([topEndAction], 예: 선택 체크)은 우측 상단이다. 격자에서
 * 체크가 칸의 우측 상단에 있으므로 크게 볼 때도 같은 자리에 두면 따로 배울 것이 없다.
 *
 * [notice] 는 지금 보는 사진에 붙는 안내(예: 선택 상한)로, 번호 위에 적는다. 이 화면은 별도 창이라 부르는
 * 쪽 스낵바가 가려지므로 안내는 여기서 그려야 보인다.
 */
@Composable
fun LaimoryPhotoViewerDialog(
    photoCount: Int,
    initialIndex: Int,
    onDismiss: () -> Unit,
    photo: @Composable (index: Int) -> Unit,
    topEndAction: (@Composable (currentIndex: Int) -> Unit)? = null,
    notice: (currentIndex: Int) -> String? = { null },
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
            topEndAction = topEndAction,
            notice = notice,
        )
    }
}

@Composable
private fun LaimoryPhotoViewer(
    photoCount: Int,
    initialIndex: Int,
    onDismiss: () -> Unit,
    photo: @Composable (index: Int) -> Unit,
    topEndAction: (@Composable (currentIndex: Int) -> Unit)?,
    notice: (currentIndex: Int) -> String?,
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

            IconButton(
                onClick = onDismiss,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(Spacing.small),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ico_default_close),
                    contentDescription = "닫기",
                    tint = Color.White,
                )
            }
            topEndAction?.let { action ->
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(Spacing.small),
                ) {
                    action(pagerState.currentPage)
                }
            }
        }

        notice(pagerState.currentPage)?.let { message ->
            Text(
                text = message,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.extraLarge)
                        .padding(bottom = Spacing.medium),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = "${pagerState.currentPage + 1} / $photoCount",
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = Spacing.extraLarge),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
        )
    }
}

/** 상단 닫기·동작 버튼 자리를 사진이 덮지 않게 비워 두는 높이. */
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
            topEndAction = null,
            notice = { "최대 20장까지 고를 수 있어요." },
        )
    }
}
