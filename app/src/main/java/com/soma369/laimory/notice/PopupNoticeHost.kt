package com.soma369.laimory.notice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.soma369.laimory.core.domain.model.notice.PopupNotice
import com.soma369.laimory.core.ui.R
import com.soma369.laimory.core.ui.component.sheet.LaimorySheetDragHandle
import com.soma369.laimory.core.ui.theme.Spacing
import kotlin.math.abs

/**
 * 홈 팝업 공지 시트(Figma `확정 / 팝업 공지 카드 시트 — 썸네일형` 3204:2663 의 3204:2664 · 2685 · 2709). 처리하지 않은
 * 팝업 공지를 카드로 한 장씩 보여 준다.
 *
 * 카드마다 `다시 보지 않기`([onDismissForever]) | `닫기`([onClose]) 로 처리한다. 처리한 카드는 [notices] 에서 빠지고
 * 남은 카드의 첫 장이 보인다 — 인디케이터도 함께 줄고 커서는 첫 칸이다. 좌우로 쓸어 넘길 수 없고, 아래 점을 눌러 다른
 * 카드를 본다(1건이면 점 없음). 카드를 누르면 원문을 연다([onOpen]) — 시트와 카드는 그대로다.
 *
 * 헤더 X 는 없다. 쓸어내리기 · 바깥 누름 · 뒤로가기는 남은 카드를 모두 닫는다([onCloseAll]).
 *
 * 공용 Dialog 자리(`GlobalDialogHost`)와 따로 둔다. 그쪽은 한 번에 하나만 보여 줘 다른 Dialog 가 뜨면 밀려 사라지고,
 * 계정 경계에서 통째 비워진다. 띄울지는 [isVisible] 로 받는다.
 *
 * @param isVisible 홈이 보이고 강제 업데이트 · 권장 업데이트 안내 · 다른 전역 Dialog 가 없을 때만 참.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopupNoticeHost(
    notices: List<PopupNotice>,
    isVisible: Boolean,
    onOpen: (PopupNotice) -> Unit,
    onDismissForever: (PopupNotice) -> Unit,
    onClose: (PopupNotice) -> Unit,
    onCloseAll: () -> Unit,
) {
    if (notices.isEmpty() || !isVisible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // 카드가 빠져 목록이 바뀌면 커서는 첫 칸으로 돌아간다(큐에서 다음 차례).
    var current by remember(notices) { mutableIntStateOf(0) }
    val notice = notices[current.coerceIn(notices.indices)]
    ModalBottomSheet(
        onDismissRequest = onCloseAll,
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
        ) {
            Text(
                text = "공지사항",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.small),
            )
            // 카드 · 점 · 버튼 사이 간격은 점 줄이 위아래로 품는다 — 6dp 점의 눌림 영역을 간격만큼 넓히려는 것이다.
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                NoticeCard(
                    notice = notice,
                    position = "${notices.size}건 중 ${current + 1}번째",
                    onClick = { onOpen(notice) },
                )
                if (notices.size > 1) {
                    PageIndicator(count = notices.size, current = current, onSelect = { current = it })
                } else {
                    Spacer(modifier = Modifier.height(Spacing.extraLarge))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f).height(ButtonHeight),
                        onClick = { onDismissForever(notice) },
                        shape = RoundedCornerShape(ButtonCornerRadius),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Text(
                            text = "다시 보지 않기",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                        )
                    }
                    Button(
                        modifier = Modifier.weight(1f).height(ButtonHeight),
                        onClick = { onClose(notice) },
                        shape = RoundedCornerShape(ButtonCornerRadius),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                    ) {
                        Text(
                            text = "닫기",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 카드 = 썸네일(4:3) + 제목(Figma 썸네일형 3204:2663). 팝업 응답엔 게시일이 없어 일자는 싣지 않는다. 이미지를 보여 주는
 * 규칙은 [NoticeThumbnail]. 누르면 원문을 연다.
 *
 * 제목 칸은 늘 2줄 높이다 — 제목 길이가 달라도 카드가 바뀔 때 시트가 출렁이지 않게.
 */
@Composable
private fun NoticeCard(
    notice: PopupNotice,
    position: String,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ThumbnailCornerRadius))
                .clickable(onClickLabel = "원문 보기", role = Role.Button, onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = "$position, ${notice.title}" },
        verticalArrangement = Arrangement.spacedBy(CardGap),
    ) {
        NoticeThumbnail(imageUrl = notice.thumbnailUrl)
        Text(
            text = notice.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

/**
 * 4:3 칸, 높이 고정. 가로 이미지(가로 ≥ 세로)는 칸을 꽉 채워 자르고, **세로로 긴 이미지는 자르지 않고** 칸 높이에 맞춰
 * 가운데 둔 뒤 좌우 빈 곳에 같은 이미지를 꽉 채워 흐리게 깐다(Figma 3241:2698). 불러오는 중 · 실패면 회색 바탕에
 * 워드마크(3204:2685) — 서버가 팝업 지정에 썸네일을 필수로 해 주소가 없는 경우는 없다.
 *
 * 흐림(`Modifier.blur`)은 Android 12 부터 그려진다. 그 아래에서는 흐리지 않은 채 어둡게만 깔린다.
 */
@Composable
private fun NoticeThumbnail(imageUrl: String) {
    val painter = rememberAsyncImagePainter(model = imageUrl)
    val loaded = painter.state as? AsyncImagePainter.State.Success
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(THUMBNAIL_ASPECT_RATIO)
                .clip(RoundedCornerShape(ThumbnailCornerRadius))
                .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (loaded == null) {
            Image(
                painter = painterResource(R.drawable.img_laimory_wordmark),
                contentDescription = null,
                modifier = Modifier.width(WordmarkWidth),
            )
        }
        run {
            val size = loaded?.painter?.intrinsicSize
            val isPortrait = size != null && size.isSpecified && size.height > size.width
            if (isPortrait) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().blur(PortraitBackdropBlur),
                    contentScale = ContentScale.Crop,
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = PORTRAIT_BACKDROP_DIM)))
            }
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = if (isPortrait) ContentScale.Fit else ContentScale.Crop,
            )
        }
    }
}

/**
 * 지금 카드는 길쭉한 점, 나머지는 동그란 점. 점을 누르면 그 카드로 간다.
 *
 * 점은 6dp 라 그대로는 누르기 어렵다. 줄 전체(위아래 간격 · 좌우 여백 포함)를 눌림 영역으로 두고, 누른 자리에서 가장
 * 가까운 점을 고른다. 화면 읽기 도구는 점마다 따로 고른다.
 */
@Composable
private fun PageIndicator(
    count: Int,
    current: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .height(IndicatorTouchHeight)
                .pointerInput(count, current) {
                    detectTapGestures { offset ->
                        val centers = dotCenters(count, current).map { (it + IndicatorTouchInset).toPx() }
                        val nearest = centers.indices.minBy { abs(centers[it] - offset.x) }
                        onSelect(nearest)
                    }
                }.padding(horizontal = IndicatorTouchInset),
        horizontalArrangement = Arrangement.spacedBy(DotGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val isCurrent = index == current
            Box(
                modifier =
                    Modifier
                        .width(if (isCurrent) CurrentDotWidth else DotSize)
                        .height(DotSize)
                        .clip(RoundedCornerShape(DotSize / 2))
                        .background(if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        .semantics {
                            role = Role.Tab
                            selected = isCurrent
                            contentDescription = "${count}건 중 ${index + 1}번째 공지"
                            onClick {
                                onSelect(index)
                                true
                            }
                        },
            )
        }
    }
}

/** 점 줄 왼쪽 끝(좌우 여백 안쪽)부터 각 점 가운데까지의 거리. */
private fun dotCenters(
    count: Int,
    current: Int,
): List<Dp> {
    var start = 0.dp
    return List(count) { index ->
        val width = if (index == current) CurrentDotWidth else DotSize
        (start + width / 2).also { start += width + DotGap }
    }
}

private val SheetCornerRadius = 24.dp
private const val THUMBNAIL_ASPECT_RATIO = 4f / 3f
private val ThumbnailCornerRadius = 16.dp
private val WordmarkWidth = 140.dp
private val PortraitBackdropBlur = 24.dp
private const val PORTRAIT_BACKDROP_DIM = 0.2f
private val CardGap = 14.dp
private val DotSize = 6.dp
private val CurrentDotWidth = 18.dp
private val DotGap = 6.dp

/** 점 줄의 눌림 높이 = 위아래 간격(카드 · 버튼과의 20dp) + 점 6dp. 보이는 배치는 시안과 같다. */
private val IndicatorTouchHeight = Spacing.extraLarge * 2 + DotSize

/** 점 줄 양옆으로 넓힌 눌림 영역. */
private val IndicatorTouchInset = Spacing.large
private val ButtonHeight = 52.dp
private val ButtonCornerRadius = 16.dp
