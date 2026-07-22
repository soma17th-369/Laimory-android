package com.soma369.laimory.feature.timeline.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing

@Composable
internal fun TimelinePhotoViewerDialog(
    photoUrls: List<String?>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    if (photoUrls.isEmpty()) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        TimelinePhotoViewer(
            photoUrls = photoUrls,
            initialIndex = initialIndex,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun TimelinePhotoViewer(
    photoUrls: List<String?>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    val pagerState =
        rememberPagerState(
            initialPage = initialIndex.coerceIn(photoUrls.indices),
            pageCount = photoUrls::size,
        )
    val isInPreview = LocalInspectionMode.current

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val photoUrl = photoUrls[page]
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(vertical = 72.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (!isInPreview && photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "타임라인 사진 ${page + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(Spacing.extraLarge)
                                .background(Color.DarkGray),
                    )
                }
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

        Text(
            text = "${pagerState.currentPage + 1} / ${photoUrls.size}",
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Spacing.extraLarge),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun TimelinePhotoViewerPreview() {
    LaimoryTheme {
        TimelinePhotoViewer(
            photoUrls = List(5) { null },
            initialIndex = 2,
            onDismiss = {},
        )
    }
}
