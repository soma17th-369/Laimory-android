package com.soma369.laimory.feature.timeline.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import coil.compose.AsyncImage
import com.soma369.laimory.core.ui.component.photo.LaimoryPhotoViewerDialog
import com.soma369.laimory.core.ui.theme.Spacing

/** 타임라인 이벤트 사진을 크게 본다. 틀은 공용 뷰어이고, 여기서는 서버 사진 URL 을 그리는 방법만 정한다. */
@Composable
internal fun TimelinePhotoViewerDialog(
    photoUrls: List<String?>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    val isInPreview = LocalInspectionMode.current
    LaimoryPhotoViewerDialog(
        photoCount = photoUrls.size,
        initialIndex = initialIndex,
        onDismiss = onDismiss,
        photo = { index ->
            val photoUrl = photoUrls[index]
            if (!isInPreview && photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "타임라인 사진 ${index + 1}",
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
        },
    )
}
