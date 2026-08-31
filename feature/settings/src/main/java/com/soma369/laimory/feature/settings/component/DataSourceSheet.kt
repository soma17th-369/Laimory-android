package com.soma369.laimory.feature.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = LaimoryShapes.extraLarge,
        dragHandle = { LaimorySheetDragHandle() },
    ) {
        DataSourceSheetContent(
            source = source,
            status = status,
            action = action,
            onAction = onAction,
            onDismiss = onDismiss,
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}

@Composable
private fun DataSourceSheetContent(
    source: DataSourceUiModel,
    status: DataSourceStatus,
    action: DataPermissionAction,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 공용 시트와 같은 16dp 좌우 여백과 24dp 섹션 간격을 한 부모에서 적용한다. 헤더와
    // 버튼에 서로 다른 패딩을 주면 제목은 모서리에 붙고 본문·버튼의 시작선도 어긋난다.
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.large)
                .padding(bottom = Spacing.extraLarge2),
        verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge2),
    ) {
        LaimorySheetHeader(title = source.label, onClose = onDismiss)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
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
            // 두 번 거부하면 Android 가 요청을 삼킨다. 왜 다이얼로그가 안 뜨는지 알려 주지
            // 않으면 사용자는 버튼이 고장난 줄 안다.
            if (status == DataSourceStatus.DENIED && action == DataPermissionAction.APP_SETTINGS) {
                Text(
                    text = "이 권한은 더 이상 앱에서 물어볼 수 없어요. 설정에서 직접 켜 주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        action.buttonLabel(status)?.let { label ->
            Button(
                modifier = Modifier.fillMaxWidth().height(ActionButtonHeight),
                onClick = onAction,
                shape = LaimoryShapes.large,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
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

@Preview(name = "DataSourceSheet / 허용됨", apiLevel = 36, showBackground = true, widthDp = 360, heightDp = 420)
@Composable
private fun DataSourceSheetGrantedPreview() {
    LaimoryTheme {
        DataSourceSheetPreviewBody(
            source = DataSourceUiModel.PHOTO,
            status = DataSourceStatus.GRANTED,
            action = DataPermissionAction.APP_SETTINGS,
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

/** ModalBottomSheet 는 Preview 에서 뜨지 않으므로 실제 시트 내용을 그대로 그린다. */
@Composable
private fun DataSourceSheetPreviewBody(
    source: DataSourceUiModel,
    status: DataSourceStatus,
    action: DataPermissionAction,
) {
    DataSourceSheetContent(
        source = source,
        status = status,
        action = action,
        onAction = {},
        onDismiss = {},
        modifier = Modifier.padding(top = Spacing.extraLarge),
    )
}

private val ActionButtonHeight = 52.dp
