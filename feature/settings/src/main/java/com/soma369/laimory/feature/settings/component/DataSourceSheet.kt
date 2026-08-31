package com.soma369.laimory.feature.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.ui.component.sheet.LaimorySheetDragHandle
import com.soma369.laimory.core.ui.component.sheet.LaimorySheetHeader
import com.soma369.laimory.core.ui.permission.DataPermissionAction
import com.soma369.laimory.core.ui.permission.DataSourceStatus
import com.soma369.laimory.core.ui.theme.LaimoryShapes
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.settings.model.DataSourceUiModel
import com.soma369.laimory.feature.settings.model.buttonLabel

/**
 * 데이터 소스 하나의 상태와 다음 행동을 보여주는 시트.
 *
 * 행을 눌렀을 때 시스템 설정으로 곧장 보내지 않는 이유는, 소스마다 갈 수 있는 곳이 다르기
 * 때문이다 — 사진·캘린더는 다이얼로그, 백그라운드 위치는 앱 설정, 알림 읽기는 알림 접근 설정,
 * 그리고 아무 데도 갈 수 없는 기기도 있다. 시트 하나가 그 차이를 흡수하면 목록은 소스가 늘어도
 * 그대로 둘 수 있다.
 *
 * 버튼은 항상 하나다. 상태마다 할 수 있는 일이 하나뿐이라 선택지를 늘리면 고민만 는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DataSourceSheet(
    source: DataSourceUiModel,
    status: DataSourceStatus,
    action: DataPermissionAction,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = LaimoryShapes.extraLarge,
        dragHandle = { LaimorySheetDragHandle() },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = Spacing.extraLarge2),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            LaimorySheetHeader(title = source.label, onClose = onDismiss)
            Column(
                modifier = Modifier.padding(horizontal = Spacing.extraLarge),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                Text(
                    text = source.statusLabel(status),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = source.purpose,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // 켤 방법이 없는 기기에는 안내만 남긴다. 누를 수 없는 버튼을 두면 무엇이 잘못됐는지
                // 알 수 없고, 없는 화면을 찾아 헤매게 된다.
                if (status == DataSourceStatus.UNSUPPORTED) {
                    Text(
                        text = "이 기기에는 해당 설정 화면이 없어 켤 수 없어요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            action.buttonLabel(status)?.let { label ->
                Button(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.extraLarge)
                            .padding(top = Spacing.extraSmall),
                    onClick = onAction,
                ) {
                    Text(text = label, modifier = Modifier.padding(vertical = 6.dp))
                }
            }
        }
    }
}

@Preview(name = "DataSourceSheet / 일부 사진", apiLevel = 36, showBackground = true, widthDp = 360, heightDp = 420)
@Composable
private fun DataSourceSheetLimitedPreview() {
    LaimoryTheme {
        DataSourceSheetPreviewBody(
            source = DataSourceUiModel.PHOTO,
            status = DataSourceStatus.LIMITED,
            action = DataPermissionAction.RESELECT_PHOTOS,
        )
    }
}

@Preview(name = "DataSourceSheet / 미지원", apiLevel = 36, showBackground = true, widthDp = 360, heightDp = 420)
@Composable
private fun DataSourceSheetUnsupportedPreview() {
    LaimoryTheme {
        DataSourceSheetPreviewBody(
            source = DataSourceUiModel.NOTIFICATION,
            status = DataSourceStatus.UNSUPPORTED,
            action = DataPermissionAction.NONE,
        )
    }
}

/** ModalBottomSheet 는 Preview 에서 뜨지 않으므로 내용만 같은 배치로 그린다. */
@Composable
private fun DataSourceSheetPreviewBody(
    source: DataSourceUiModel,
    status: DataSourceStatus,
    action: DataPermissionAction,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.extraLarge),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        LaimorySheetHeader(title = source.label, onClose = {})
        Column(
            modifier = Modifier.padding(horizontal = Spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            Text(
                text = source.statusLabel(status),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = source.purpose,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        action.buttonLabel(status)?.let { label ->
            Button(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.extraLarge),
                onClick = {},
            ) {
                Text(text = label, modifier = Modifier.padding(vertical = 6.dp))
            }
        }
    }
}
