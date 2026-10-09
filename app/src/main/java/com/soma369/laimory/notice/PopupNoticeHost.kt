package com.soma369.laimory.notice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.domain.model.notice.Notice
import com.soma369.laimory.core.ui.R
import com.soma369.laimory.core.ui.component.sheet.LaimorySheetDragHandle
import com.soma369.laimory.core.ui.component.sheet.LaimorySheetHeader
import com.soma369.laimory.core.ui.theme.Spacing
import java.time.format.DateTimeFormatter

/**
 * 앱 시작 팝업 공지 시트(Figma `확정 / 팝업 공지 카드 시트` 3201:2651). 안 본 팝업 공지를 카드로 한 장씩 보여 주고 좌우로
 * 넘긴다. 아래 점이 몇 건 중 몇 번째인지 보여 준다(1건이면 점 없음).
 *
 * `자세히 보기` 는 지금 카드의 원문을 연다([onOpen]) — 시트는 그대로다. `모두 닫기` · X · 쓸어내리기 · 뒤로가기 · 바깥
 * 누름은 [onClose] 이고, **넘겨서 화면에 띄운 카드의 id 만** 넘긴다. 넘기지 않은 카드는 보지 않은 것이라 다음에 다시 뜬다.
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
    onClose: (viewedIds: Set<Long>) -> Unit,
) {
    if (notices.isEmpty() || !isVisible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pagerState = rememberPagerState(pageCount = { notices.size })
    // 화면을 다시 그릴 값이 아니라 닫을 때 넘길 기록이라 상태로 두지 않는다.
    val viewedIds = remember(notices) { mutableSetOf<Long>() }
    LaunchedEffect(notices, pagerState.currentPage) {
        notices.getOrNull(pagerState.currentPage)?.let { viewedIds += it.id }
    }
    val close = { onClose(viewedIds.toSet()) }
    ModalBottomSheet(
        onDismissRequest = close,
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
            verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LaimorySheetHeader(title = "공지사항", onClose = close)
            HorizontalPager(
                state = pagerState,
                pageSpacing = Spacing.medium,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                NoticeCard(notice = notices[page], position = "${notices.size}건 중 ${page + 1}번째")
            }
            if (notices.size > 1) PageIndicator(count = notices.size, current = pagerState.currentPage)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f).height(ButtonHeight),
                    onClick = close,
                    shape = RoundedCornerShape(ButtonCornerRadius),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text(
                        text = if (notices.size > 1) "모두 닫기" else "닫기",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                }
                Button(
                    modifier = Modifier.weight(1f).height(ButtonHeight),
                    onClick = { notices.getOrNull(pagerState.currentPage)?.let(onOpen) },
                    shape = RoundedCornerShape(ButtonCornerRadius),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                ) {
                    Text(
                        text = "자세히 보기",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** 카드 높이는 고정이다 — 제목 길이가 달라도 넘길 때 시트가 출렁이지 않게. 제목은 최대 3줄. */
@Composable
private fun NoticeCard(
    notice: Notice,
    position: String,
) {
    val date = notice.publishedAt.format(PublishedDateFormatter)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(CardHeight)
                .clip(RoundedCornerShape(CardCornerRadius))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = Spacing.extraLarge, vertical = Spacing.extraLarge)
                .clearAndSetSemantics { contentDescription = "$position, ${notice.title}, $date" },
        verticalArrangement = Arrangement.spacedBy(Spacing.medium, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ico_setting_notice),
            contentDescription = null,
            modifier = Modifier.size(MegaphoneSize),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = notice.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = date,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 지금 쪽은 길쭉한 점, 나머지는 동그란 점. */
@Composable
private fun PageIndicator(
    count: Int,
    current: Int,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(DotGap)) {
        repeat(count) { index ->
            val isCurrent = index == current
            Box(
                modifier =
                    Modifier
                        .width(if (isCurrent) CurrentDotWidth else DotSize)
                        .height(DotSize)
                        .clip(RoundedCornerShape(DotSize / 2))
                        .background(if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}

private val PublishedDateFormatter = DateTimeFormatter.ofPattern("M월 d일")
private val SheetCornerRadius = 24.dp
private val CardHeight = 220.dp
private val CardCornerRadius = 16.dp
private val MegaphoneSize = 40.dp
private val DotSize = 6.dp
private val CurrentDotWidth = 18.dp
private val DotGap = 6.dp
private val ButtonHeight = 52.dp
private val ButtonCornerRadius = 16.dp
