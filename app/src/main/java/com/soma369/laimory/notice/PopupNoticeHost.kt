package com.soma369.laimory.notice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.domain.model.notice.Notice
import com.soma369.laimory.core.ui.R
import com.soma369.laimory.core.ui.component.sheet.LaimorySheetDragHandle
import com.soma369.laimory.core.ui.component.sheet.LaimorySheetHeader
import com.soma369.laimory.core.ui.theme.Spacing
import java.time.format.DateTimeFormatter

/**
 * 앱 시작 팝업 공지 시트(Figma 3180:1676). 안 본 팝업 공지를 한 시트에 목록으로 모아 띄운다 — 제목 줄 `새 공지 N건` 으로
 * 개수를 보인다.
 *
 * 줄을 누르면 원문을 열고 시트는 그대로 둔다([onOpen]). `확인` · X · 쓸어내리기 · 뒤로가기 · 바깥 누름은 모두
 * [onClose] — 목록 전부 본 것이다.
 *
 * 공용 Dialog 자리(`GlobalDialogHost`)와 따로 둔다. 그쪽은 한 번에 하나만 보여 줘 다른 Dialog 가 뜨면 밀려 사라지고,
 * 계정 경계에서 통째 비워진다. 띄울지는 [isVisible] 로 받는다.
 *
 * @param isVisible 홈이 보이고 강제 업데이트 · 권장 업데이트 안내 · 다른 전역 Dialog 가 없을 때만 참.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopupNoticeHost(
    notices: List<Notice>,
    isVisible: Boolean,
    onOpen: (Notice) -> Unit,
    onClose: () -> Unit,
) {
    if (notices.isEmpty() || !isVisible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius),
        dragHandle = { LaimorySheetDragHandle() },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.large)
                    .padding(bottom = Spacing.extraLarge2),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge2),
        ) {
            LaimorySheetHeader(
                title = "새 공지",
                onClose = onClose,
                titleAccessory = {
                    Text(
                        text = "${notices.size}건",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            )
            // 많으면 목록만 스크롤하고 `확인` 은 늘 보이게 한다.
            Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                notices.forEachIndexed { index, notice ->
                    NoticeRow(notice = notice, onClick = { onOpen(notice) })
                    if (index != notices.lastIndex) {
                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
            Button(
                modifier = Modifier.fillMaxWidth().height(ConfirmButtonHeight),
                onClick = onClose,
                shape = RoundedCornerShape(ConfirmButtonCornerRadius),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Text(
                    text = "확인",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

@Composable
private fun NoticeRow(
    notice: Notice,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClickLabel = "원문 열기", onClick = onClick)
                .padding(vertical = Spacing.large),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
        ) {
            Text(
                text = notice.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = notice.publishedAt.format(PublishedDateFormatter),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ico_default_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(ChevronSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val PublishedDateFormatter = DateTimeFormatter.ofPattern("M월 d일")
private val SheetCornerRadius = 24.dp
private val ChevronSize = 16.dp
private val ConfirmButtonHeight = 52.dp
private val ConfirmButtonCornerRadius = 16.dp
