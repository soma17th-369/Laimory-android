package com.soma369.laimory.feature.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.home.state.DraftConsentDetailItem
import com.soma369.laimory.feature.home.state.DraftConsentTypeSummary

/** 유형 1개의 실제 전송 항목을 확인하는 시트. 확인 전용이며 원본 데이터 편집은 제공하지 않는다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DraftConsentTypeDetailSheet(
    summary: DraftConsentTypeSummary,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.extraLarge, vertical = Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Text("${summary.group.label()} 전송 상세", style = MaterialTheme.typography.titleLarge)
            Text(
                text = summary.countLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "아래 항목이 그대로 서버로 전송돼요. 이 화면에서는 내용을 수정할 수 없어요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyColumn(
                modifier = Modifier.heightIn(max = DETAIL_LIST_MAX_HEIGHT),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                summary.sections.forEach { section ->
                    section.title?.let { sectionTitle ->
                        item(key = "section-$sectionTitle") {
                            Text(
                                text = "$sectionTitle ${section.items.size}건",
                                modifier = Modifier.padding(top = Spacing.small),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                    items(items = section.items, key = DraftConsentDetailItem::key) { item ->
                        DetailItemRow(item)
                    }
                }
            }
            Spacer(modifier = Modifier.height(Spacing.large))
        }
    }
}

@Composable
private fun DetailItemRow(item: DraftConsentDetailItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        item.imageUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = "전송할 사진 미리보기",
                modifier =
                    Modifier
                        .size(DETAIL_THUMBNAIL_SIZE)
                        .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )
            item.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                )
            }
            Text(
                text = item.timeText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

private val DETAIL_LIST_MAX_HEIGHT = 480.dp
private val DETAIL_THUMBNAIL_SIZE = 48.dp
