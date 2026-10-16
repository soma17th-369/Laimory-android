package com.soma369.laimory.notice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.soma369.laimory.core.domain.model.notice.PopupNotice
import com.soma369.laimory.core.ui.R
import com.soma369.laimory.core.ui.component.sheet.LaimorySheetDragHandle
import com.soma369.laimory.core.ui.theme.Spacing

/**
 * 홈 팝업 공지 시트(Figma `확정 / 팝업 공지 카드 시트 — 썸네일형` 3204:2663 의 3204:2664 · 2685 · 2709). 처리하지 않은
 * 팝업 공지를 카드로 한 장씩 보여 준다.
 *
 * 카드마다 `다시 보지 않기`([onDismissForever]) | `닫기`([onClose]) 로 처리한다. 처리한 카드는 [notices] 에서 빠지고
 * 남은 카드의 첫 장이 보인다. 늘 첫 장부터 차례로 처리하고, 쓸어 넘기거나 점을 눌러 건너뛸 수 없다 — 공지는 하나씩 다
 * 봐야 하는 것이다. 아래 점은 남은 건수와 지금 차례만 보여 주는 표시다(1건이어도 점 하나). 카드를 누르면 원문을
 * 연다([onOpen]) — 시트와 카드는 그대로다.
 *
 * 헤더 X 는 없다. 손잡이로 쓸어내리기 · 바깥 누름 · 뒤로가기는 남은 카드를 모두 닫는다([onCloseAll]). 본문을 끌어서는
 * 시트가 움직이지 않는다.
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
    // 늘 대기열의 첫 장이다. 처리한 카드가 빠지면 다음 차례가 첫 장이 된다.
    val notice = notices.first()
    ModalBottomSheet(
        onDismissRequest = onCloseAll,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius),
        dragHandle = { LaimorySheetDragHandle(bottomPadding = Spacing.medium) },
    ) {
        // 본문을 끌어서는 시트가 움직이지 않는다 — 손잡이로만 끌어 내린다. 시트보다 먼저 세로 끌기를 가져가 소비하면 시트의
        // 끌기는 시작되지 않는다. 누르기(카드 · 버튼)는 끌기 문턱을 넘지 않으므로 그대로 받는다.
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) { detectVerticalDragGestures { change, _ -> change.consume() } }
                    .padding(horizontal = Spacing.large)
                    .padding(bottom = Spacing.extraLarge2),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge),
        ) {
            Text(
                text = "공지사항",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.small),
            )
            NoticeCard(
                notice = notice,
                position = "${notices.size}건 중 1번째",
                onClick = { onOpen(notice) },
            )
            PageIndicator(count = notices.size, modifier = Modifier.align(Alignment.CenterHorizontally))
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

/**
 * 카드 = 썸네일(1:1) + 제목(Figma 썸네일형 3204:2663). 팝업 응답엔 게시일이 없어 일자는 싣지 않는다. 이미지를 보여 주는
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
 * 1:1 칸, 높이 고정. 이미지는 자르지 않고 칸 안에 통째로 맞춘다. 정사각이 아니면 남는 위아래 · 좌우에 같은 이미지를 꽉
 * 채워 흐리게 깐다. 불러오는 중 · 실패면 회색 바탕에 워드마크(3204:2685) — 서버가 팝업 지정에 썸네일을 필수로 해 주소가
 * 없는 경우는 없다.
 *
 * 흐린 배경은 칸보다 크게 키워 그린다. 흐림은 가장자리에서 바깥의 빈 곳을 섞어 옅어지는데, 칸 크기 그대로면 그 옅은 테가
 * 회색 바탕을 비춰 여백처럼 보인다. 키워서 옅은 테를 칸 밖(잘리는 곳)으로 밀어낸다.
 *
 * 흐림(`Modifier.blur`)은 Android 12 부터 그려진다. 그 아래에서는 흐리지 않은 채 어둡게만 깔린다.
 */
@Composable
private fun NoticeThumbnail(imageUrl: String) {
    val painter = rememberAsyncImagePainter(model = imageUrl)
    val isLoaded = painter.state is AsyncImagePainter.State.Success
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(THUMBNAIL_ASPECT_RATIO)
                .clip(RoundedCornerShape(ThumbnailCornerRadius))
                .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoaded) {
            Image(
                painter = painter,
                contentDescription = null,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .scale(BACKDROP_SCALE)
                        .blur(BackdropBlur, BlurredEdgeTreatment.Unbounded),
                contentScale = ContentScale.Crop,
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = BACKDROP_DIM)))
        } else {
            Image(
                painter = painterResource(R.drawable.img_laimory_wordmark),
                contentDescription = null,
                modifier = Modifier.width(WordmarkWidth),
            )
        }
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}

/**
 * 남은 건수만큼 점, 지금 차례(늘 첫 칸)는 길쭉한 점. 누를 수 없는 표시다. 화면 읽기 도구에는 카드가 "N건 중 1번째"로
 * 읽히므로 점은 따로 읽지 않는다.
 */
@Composable
private fun PageIndicator(
    count: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(DotGap),
    ) {
        repeat(count) { index ->
            val isCurrent = index == 0
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

private val SheetCornerRadius = 24.dp

private const val THUMBNAIL_ASPECT_RATIO = 1f
private val BackdropBlur = 24.dp
private const val BACKDROP_DIM = 0.2f

/** 흐린 배경을 키우는 배율. 옅은 테(흐림 반경쯤)가 칸 밖으로 넉넉히 나가는 크기다 — 칸 한 변의 20% 씩. */
private const val BACKDROP_SCALE = 1.4f
private val ThumbnailCornerRadius = 16.dp
private val WordmarkWidth = 140.dp

private val CardGap = 14.dp
private val DotSize = 6.dp
private val CurrentDotWidth = 18.dp
private val DotGap = 6.dp
private val ButtonHeight = 52.dp
private val ButtonCornerRadius = 16.dp
