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
import androidx.compose.ui.draw.clipToBounds
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
 * 홈 팝업 공지 시트(Figma `확정 / 팝업 공지 카드 시트 — 썸네일형` 3204:2663 의 3204:2664 · 2685 · 3425:378 · 3204:2709).
 * 처리하지 않은 팝업 공지를 카드로 한 장씩 보여 준다.
 *
 * 카드마다 `다시 보지 않기`([onDismissForever]) | `다음`([onClose]) 으로 처리한다. 마지막 장에서는 `다음` 이 `닫기` 가
 * 된다 — 동작은 같다. 처리한 카드는 [notices] 에서 빠지고 남은 카드의 첫 장이 보인다. 늘 첫 장부터 차례로 처리하고, 쓸어
 * 넘겨 건너뛸 수 없다 — 공지는 하나씩 다 봐야 하는 것이다. 넘길 수 있어 보이는 점 인디케이터 대신 제목 줄 오른쪽에
 * `n / N` 으로 몇 번째인지 보여 준다(1건이면 숨김). 카드를 누르면 원문을 연다([onOpen]) — 시트와 카드는 그대로다.
 *
 * 헤더 X 는 없다. 손잡이 · 제목 줄로 쓸어내리기 · 바깥 누름 · 뒤로가기는 남은 카드를 모두 닫는다([onCloseAll]). 본문을
 * 끌어서는 시트가 움직이지 않는다.
 *
 * `공지사항` 제목은 손잡이 바로 아래에 붙여 한 덩어리 머리 줄로 둔다(손잡이 슬롯 안). 썸네일은 시트 폭 전체를 쓴다.
 *
 * 공용 Dialog 자리(`GlobalDialogHost`)와 따로 둔다. 그쪽은 한 번에 하나만 보여 줘 다른 Dialog 가 뜨면 밀려 사라지고,
 * 계정 경계에서 통째 비워진다. 띄울지는 [isVisible] 로 받는다.
 *
 * @param total 이번에 띄운 공지 건수(`n / N` 의 N). 카드를 처리해 [notices] 가 줄어도 그대로다.
 * @param isVisible 홈이 보이고 강제 업데이트 · 권장 업데이트 안내 · 다른 전역 Dialog 가 없을 때만 참.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopupNoticeHost(
    notices: List<PopupNotice>,
    total: Int,
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
    // 대기열과 건수는 따로 오므로 어긋난 순간에도 숫자가 뒤집히지 않게 남은 건수를 하한으로 둔다.
    val count = maxOf(total, notices.size)
    val position = count - notices.size + 1
    val isLast = notices.size == 1
    ModalBottomSheet(
        onDismissRequest = onCloseAll,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius),
        dragHandle = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LaimorySheetDragHandle(topPadding = Spacing.medium, bottomPadding = Spacing.small)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.large),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "공지사항",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (count > 1) {
                        // 화면 읽기 도구에는 카드가 "N건 중 n번째"로 읽히므로 숫자는 따로 읽지 않는다.
                        Text(
                            text = "$position / $count",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                    }
                }
            }
        },
    ) {
        // 본문을 끌어서는 시트가 움직이지 않는다 — 손잡이로만 끌어 내린다. 시트보다 먼저 세로 끌기를 가져가 소비하면 시트의
        // 끌기는 시작되지 않는다. 누르기(카드 · 버튼)는 끌기 문턱을 넘지 않으므로 그대로 받는다.
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) { detectVerticalDragGestures { change, _ -> change.consume() } }
                    .padding(top = Spacing.large, bottom = Spacing.extraLarge2),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge),
        ) {
            // 버튼은 먼저 자리를 잡고, 카드는 남은 높이 안에서만 그린다. 짧은 창(분할 화면 · 가로)에서도 버튼이 밀려나지
            // 않게 하려는 것이다 — 본문은 스크롤도 끌기도 없어 밀려난 버튼에 닿을 방법이 없다.
            NoticeCard(
                notice = notice,
                position = "${count}건 중 ${position}번째",
                onClick = { onOpen(notice) },
                modifier = Modifier.weight(1f, fill = false),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.large),
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
                        text = if (isLast) "닫기" else "다음",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * 카드 = 썸네일(1:1, 시트 폭) + 제목(Figma 썸네일형 3204:2663). 팝업 응답엔 게시일이 없어 일자는 싣지 않는다. 이미지를
 * 보여 주는 규칙은 [NoticeThumbnail]. 누르면 원문을 연다.
 *
 * 제목 칸은 늘 2줄 높이다 — 제목 길이가 달라도 카드가 바뀔 때 시트가 출렁이지 않게. 높이가 모자라면 제목 칸을 먼저
 * 두고 썸네일을 남은 높이에 맞춰 줄인다.
 */
@Composable
private fun NoticeCard(
    notice: PopupNotice,
    position: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClickLabel = "원문 보기", role = Role.Button, onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = "$position, ${notice.title}" },
        verticalArrangement = Arrangement.spacedBy(CardGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NoticeThumbnail(imageUrl = notice.thumbnailUrl, modifier = Modifier.weight(1f, fill = false))
        Text(
            text = notice.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.large).clearAndSetSemantics { },
        )
    }
}

/**
 * 1:1 칸, 시트 폭 전체, 높이 고정. 이미지는 자르거나 키우지 않고 칸 안에 통째로 맞춘다 — 세로 포스터도 위아래가 잘리지
 * 않는다. 정사각이 아니면 남는 위아래 · 좌우에 같은 이미지를 꽉 채워 흐리게 깐다. 칸 비율을 고정해 두어 이미지가 도착해도
 * 시트 높이가 바뀌지 않는다. 불러오는 중 · 실패면 회색 바탕에 워드마크(3204:2685) — 서버가 팝업 지정에 썸네일을 필수로 해
 * 주소가 없는 경우는 없다.
 *
 * 흐린 배경은 칸보다 크게 키워 그린다. 흐림은 가장자리에서 바깥의 빈 곳을 섞어 옅어지는데, 칸 크기 그대로면 그 옅은 테가
 * 회색 바탕을 비춰 여백처럼 보인다. 키워서 옅은 테를 칸 밖(잘리는 곳)으로 밀어낸다. 흐림(`Modifier.blur`)은 Android 12
 * 부터 그려진다. 그 아래에서는 흐리지 않은 채 어둡게만 깔린다.
 *
 * 창이 짧아 시트 폭만큼 높이를 못 받으면 받은 높이를 한 변으로 하는 정사각으로 줄어 가운데에 놓인다. 폭을 꽉 채우도록
 * 강제하지 않는 것은 그 때문이다 — 강제하면 정사각을 지키려다 높이를 넘친다.
 */
@Composable
private fun NoticeThumbnail(
    imageUrl: String,
    modifier: Modifier = Modifier,
) {
    val painter = rememberAsyncImagePainter(model = imageUrl)
    val isLoaded = painter.state is AsyncImagePainter.State.Success
    Box(
        modifier =
            modifier
                .aspectRatio(THUMBNAIL_ASPECT_RATIO)
                .clipToBounds()
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

private val SheetCornerRadius = 24.dp

private const val THUMBNAIL_ASPECT_RATIO = 1f
private val BackdropBlur = 24.dp
private const val BACKDROP_DIM = 0.2f

/** 흐린 배경을 키우는 배율. 옅은 테(흐림 반경쯤)가 칸 밖으로 넉넉히 나가는 크기다 — 칸 한 변의 20% 씩. */
private const val BACKDROP_SCALE = 1.4f
private val WordmarkWidth = 140.dp

private val CardGap = 14.dp
private val ButtonHeight = 52.dp
private val ButtonCornerRadius = 16.dp
