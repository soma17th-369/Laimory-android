package com.soma369.laimory.feature.home.component

import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.soma369.laimory.core.domain.model.tutorial.RecommendedRecordDate
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.home.state.HomeDateRecommendation
import com.soma369.laimory.feature.home.state.HomeRecommendationPhotoAccess
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.soma369.laimory.core.ui.R as UiR

/**
 * 홈 튜토리얼의 첫 장 — 최근 한 달 중 사진을 많이 찍은 날을 추천해 첫 타임라인을 만들어 볼 날을 고르게 한다.
 *
 * 화면 전체를 검게 덮고 그 위에 날짜 카드를 대각선으로 겹쳐 쌓는다. 뒤 카드일수록 오른쪽 아래로 밀리고 살짝 기운다. 앞 카드를
 * 왼쪽으로 넘기면 왼쪽 위로 기울며 비켜나고 다음 카드가 앞으로 온다. 오른쪽으로 넘기면 앞서 본 카드가 돌아온다. 카드를 누르면 그
 * 날을 고른다.
 *
 * 추천은 날짜까지만 하고, 사진은 그 날 홈 사진 카드에서 사용자가 고른다. 추천이 없으면 그 사실과 `다른 날 직접 고르기` ·
 * `건너뛰기` 만 남는다. 사진을 못 읽어 비었으면(권한 없음 · 일부만 허용) 허용 버튼을 앞에 둔다 — 허용하고 돌아오면 이 화면
 * 그대로 추천이 채워진다. 화면 전체가 덮개라 빈 곳을 눌러 닫지 않는다 — 넘기다 빗나간 손가락에 튜토리얼이
 * 끝나면 안 된다. 뒤로가기는 건너뛰기와 같다.
 */
@Composable
internal fun HomeDateRecommendationOverlay(
    recommendation: HomeDateRecommendation,
    onPickDate: (LocalDate) -> Unit,
    onPickOtherDate: () -> Unit,
    onRequestPhotoAccess: () -> Unit,
    onSkip: () -> Unit,
) {
    Dialog(
        onDismissRequest = onSkip,
        properties =
            DialogProperties(
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        // 창 기본 어둡게 하기 대신 직접 칠한다 — 코치마크와 같은 농도로 맞춘다.
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = DIM_ALPHA))
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center,
        ) {
            HomeDateRecommendationContent(
                recommendation = recommendation,
                onPickDate = onPickDate,
                onPickOtherDate = onPickOtherDate,
                onRequestPhotoAccess = onRequestPhotoAccess,
                onSkip = onSkip,
            )
        }
    }
}

@Composable
private fun HomeDateRecommendationContent(
    recommendation: HomeDateRecommendation,
    onPickDate: (LocalDate) -> Unit,
    onPickOtherDate: () -> Unit,
    onRequestPhotoAccess: () -> Unit,
    onSkip: () -> Unit,
) {
    val suggestedAccess = recommendation.suggestedPhotoAccess
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
        ) {
            Text(
                text = "어느 날로 첫 타임라인을 만들어 볼까요?",
                style = MaterialTheme.typography.headlineMedium,
                color = OnDim,
            )
            Text(
                text =
                    when {
                        suggestedAccess == HomeRecommendationPhotoAccess.NONE ->
                            "사진 권한을 허용하면 최근 한 달 중 사진을 많이 찍은 날을 골라 드려요."
                        suggestedAccess == HomeRecommendationPhotoAccess.LIMITED ->
                            "허용한 사진 중에는 최근 한 달에 찍은 사진이 없어요. 사진을 더 허용하면 많이 찍은 날을 골라 드려요."
                        recommendation.dates.isEmpty() -> "최근 한 달 동안 찍은 사진을 찾지 못했어요. 만들어 볼 날을 직접 골라 주세요."
                        recommendation.dates.size == 1 -> "최근 한 달 중 사진을 가장 많이 찍은 날이에요. 눌러서 골라 주세요."
                        else -> "최근 한 달 중 사진을 많이 찍은 날이에요. 넘겨 보고 눌러서 골라 주세요."
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = OnDimVariant,
            )
        }

        if (recommendation.dates.isNotEmpty()) {
            RecommendedDateDeck(dates = recommendation.dates, onPickDate = onPickDate)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            if (suggestedAccess != null) {
                Button(
                    onClick = onRequestPhotoAccess,
                    modifier = Modifier.fillMaxWidth().height(ButtonHeight),
                    shape = RoundedCornerShape(ButtonCornerRadius),
                ) {
                    Text(
                        text = if (suggestedAccess == HomeRecommendationPhotoAccess.LIMITED) "사진 더 허용하기" else "사진 권한 허용하기",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            OutlinedButton(
                onClick = onPickOtherDate,
                modifier = Modifier.fillMaxWidth().height(ButtonHeight),
                shape = RoundedCornerShape(ButtonCornerRadius),
                border = BorderStroke(1.dp, OnDimVariant),
            ) {
                Text(text = "다른 날 직접 고르기", style = MaterialTheme.typography.titleMedium, color = OnDim)
            }
            TextButton(onClick = onSkip) {
                Text(text = "건너뛰기", style = MaterialTheme.typography.labelLarge, color = OnDimVariant)
            }
        }
    }
}

/**
 * 추천한 날 카드 더미. 앞 카드 하나만 끌 수 있고, 남은 카드가 최대 두 장까지 뒤에 대각선으로 비친다. 목록이라 처음과 끝이 있다 —
 * 마지막 카드 뒤에는 아무것도 없고, 넘겨도 첫 카드로 돌아가지 않는다.
 *
 * 끌다 놓았을 때 충분히 왼쪽으로 갔으면 앞 카드가 왼쪽 위로 날아가고 다음 카드가 앞으로 온다. 충분히 오른쪽이면 앞서 본 카드가
 * 왼쪽 위에서 돌아온다. 덜 갔거나 그 쪽에 카드가 없으면 제자리로 돌아간다 — 끝에서는 끌리는 양을 줄여 더 없다는 걸 느끼게 한다.
 * 아래 줄의 좌우 화살표와 점으로도 옮긴다(끌기를 모르는 사용자 · 끌기 대신 쓸 수 있는 길). 화살표는 그 쪽에 카드가 없으면 흐려진다.
 */
@Composable
private fun RecommendedDateDeck(
    dates: List<RecommendedRecordDate>,
    onPickDate: (LocalDate) -> Unit,
) {
    // 권한을 허용하고 다시 센 추천은 다른 날들이다. 처음 카드부터 보여 준다.
    var current by rememberSaveable(dates.map { it.date }) { mutableIntStateOf(0) }
    val dragX = remember { Animatable(0f) }
    val dragY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val swipeThreshold = with(density) { SwipeThreshold.toPx() }
    val exitX = with(density) { -ExitOffsetX.toPx() }
    val exitY = with(density) { -ExitOffsetY.toPx() }
    val stepX = with(density) { StackStepX.toPx() }
    val stepY = with(density) { StackStepY.toPx() }
    val cardWidthPx = with(density) { CardWidth.toPx() }
    val canSwipe = dates.size > 1
    val hasPrevious = current > 0
    val hasNext = current < dates.lastIndex
    // 넘기는 중에 화살표를 또 누르면 무시한다. 겹치면 한 번에 두 장을 건너뛰거나 끝을 지나친다.
    var moveJob by remember { mutableStateOf<Job?>(null) }

    suspend fun settle() =
        coroutineScope {
            launch { dragX.animateTo(0f) }
            launch { dragY.animateTo(0f) }
        }

    suspend fun flyOut() =
        coroutineScope {
            launch { dragX.animateTo(exitX, tween(FLY_MILLIS)) }
            launch { dragY.animateTo(exitY, tween(FLY_MILLIS)) }
        }

    fun step(action: suspend () -> Unit) {
        if (moveJob?.isActive == true) return
        moveJob = scope.launch { action() }
    }

    fun showNext() =
        step {
            if (current < dates.lastIndex) {
                flyOut()
                current += 1
                dragX.snapTo(0f)
                dragY.snapTo(0f)
            } else {
                settle()
            }
        }

    // 앞서 본 카드가 왼쪽 위에서 돌아온다.
    fun showPrevious() =
        step {
            if (current > 0) {
                current -= 1
                dragX.snapTo(exitX)
                dragY.snapTo(exitY)
            }
            settle()
        }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
    ) {
        // 뒤 카드는 그리기만 옮겨(graphicsLayer) 자리를 차지하지 않는다. 밀리고 기운 만큼 아래에 자리를 남겨 점 · 버튼과 겹치지 않게 한다.
        Box(
            modifier =
                Modifier
                    .width(CardWidth + StackStepX * (VISIBLE_CARDS - 1))
                    .padding(bottom = StackStepY * (VISIBLE_CARDS - 1) + StackTiltOverhang),
            contentAlignment = Alignment.TopStart,
        ) {
            // 남은 카드만 뒤에 쌓는다. 마지막 카드에 오면 뒤가 비어 끝이라는 게 보인다.
            val visible = minOf(dates.size - current, VISIBLE_CARDS)
            // 뒤 카드부터 그려 앞 카드가 위에 오게 한다.
            for (depth in visible - 1 downTo 0) {
                val index = current + depth
                val date = dates[index]
                key(index) {
                    val isFront = depth == 0
                    // 앞으로 나오는 카드는 스르륵 당겨지고, 앞서 본 카드가 돌아오면 밀린 카드는 스르륵 뒤로 물러난다.
                    val pose = remember { Animatable(depth.toFloat()) }
                    LaunchedEffect(depth) { pose.animateTo(depth.toFloat(), tween(SHIFT_MILLIS)) }
                    RecommendedDateCard(
                        date = date,
                        onClick = { onPickDate(date.date) },
                        isFront = isFront,
                        modifier =
                            Modifier
                                .width(CardWidth)
                                .zIndex((visible - depth).toFloat())
                                .graphicsLayer {
                                    val d = pose.value
                                    val drag = if (isFront) dragX.value else 0f
                                    translationX = stepX * d + drag
                                    translationY = stepY * d + if (isFront) dragY.value else 0f
                                    // 끄는 만큼 기운다. 왼쪽으로 가면 반시계로.
                                    rotationZ = BACK_TILT_DEGREES * d + drag / cardWidthPx * FRONT_TILT_DEGREES
                                    scaleX = 1f - BACK_SHRINK * d
                                    scaleY = 1f - BACK_SHRINK * d
                                    alpha = (1f - BACK_FADE * d) * (1f - (drag / exitX).coerceIn(0f, 1f) * FRONT_FADE)
                                }.then(
                                    if (isFront && canSwipe) {
                                        Modifier.pointerInput(current) {
                                            detectDragGestures(
                                                onDragEnd = {
                                                    when {
                                                        // 넘기는 중에 다시 끌었다 놓았으면 넘기기는 무시되니 제자리로만 돌린다.
                                                        moveJob?.isActive == true -> scope.launch { settle() }
                                                        dragX.value < -swipeThreshold -> showNext()
                                                        dragX.value > swipeThreshold -> showPrevious()
                                                        else -> scope.launch { settle() }
                                                    }
                                                },
                                                onDragCancel = { scope.launch { settle() } },
                                            ) { change, amount ->
                                                change.consume()
                                                val nextX = dragX.value + amount.x
                                                val isPastEnd = (nextX < 0f && !hasNext) || (nextX > 0f && !hasPrevious)
                                                val resistance = if (isPastEnd) EDGE_DRAG_RESISTANCE else 1f
                                                scope.launch {
                                                    dragX.snapTo(dragX.value + amount.x * resistance)
                                                    dragY.snapTo(dragY.value + amount.y * resistance)
                                                }
                                            }
                                        }
                                    } else {
                                        Modifier
                                    },
                                ),
                    )
                }
            }
        }
        if (canSwipe) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DeckArrowButton(
                    iconRes = UiR.drawable.ico_default_caret_left,
                    contentDescription = "이전 날 보기",
                    enabled = hasPrevious,
                    onClick = ::showPrevious,
                )
                dates.indices.forEach { index ->
                    Box(
                        modifier =
                            Modifier
                                .size(PageDotTouchSize)
                                .semantics { contentDescription = "${index + 1}번째 날 보기" }
                                .clickable { if (moveJob?.isActive != true) current = index },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(PageDotSize)
                                    .background(if (index == current) MaterialTheme.colorScheme.primary else OnDimMuted, CircleShape),
                        )
                    }
                }
                DeckArrowButton(
                    iconRes = UiR.drawable.ico_default_caret_right,
                    contentDescription = "다음 날 보기",
                    enabled = hasNext,
                    onClick = ::showNext,
                )
            }
        }
    }
}

/** 카드 더미 아래 좌우 화살표. 덮개 위라 밝은 색이고, 그 쪽에 카드가 없으면 흐려진 채 눌리지 않는다. */
@Composable
private fun DeckArrowButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(DeckArrowTouchSize),
        colors = IconButtonDefaults.iconButtonColors(contentColor = OnDim, disabledContentColor = OnDimMuted),
    ) {
        // tint 를 직접 주면 IconButton 의 disabled content color 를 덮어써 비활성이 활성처럼 보인다.
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(DeckArrowIconSize),
        )
    }
}

/**
 * 추천한 날 한 장. 날짜 아래에 그 날 찍은 사진을 같은 크기 3열 정사각 그리드(3×3)로 보여 준다.
 *
 * 세 줄을 넘는 날은 앞 카드일 때 줄이 천천히 위로 흘러 미리 받은 사진을 모두 보여 준다. 대표 한 장을 고르면 마지막에 찍은
 * 아무 사진(화면 · 문서)이 그 날의 얼굴이 되기 쉬워, 고르지 않고 하루를 통째로 보여 준다. 손가락을 대고 있는 동안은
 * 멈춘다 — 넘기거나 누르려는 카드가 움직이면 안 된다. 어두운 사진도 덮개와 섞이지 않게 테두리를 둔다.
 *
 * @param isFront 앞 카드만 누를 수 있고 흐른다. 뒤 카드는 비쳐 보이기만 한다.
 */
@Composable
private fun RecommendedDateCard(
    date: RecommendedRecordDate,
    onClick: () -> Unit,
    isFront: Boolean,
    modifier: Modifier = Modifier,
) {
    val dateLabel = date.date.format(RecommendedDateFormat)
    val countLabel = recommendedCountLabel(date)
    var isHeld by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        enabled = isFront,
        modifier =
            modifier
                .semantics(mergeDescendants = true) {
                    contentDescription = "$dateLabel, $countLabel"
                    if (!isFront) onClick(label = null) { false }
                }.pointerInput(Unit) {
                    // 누르기 · 끌기를 가로채지 않고 손가락이 닿아 있는지만 본다.
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        isHeld = true
                        try {
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                            } while (event.changes.any { it.pressed })
                        } finally {
                            isHeld = false
                        }
                    }
                },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = CardElevation,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.small).clearAndSetSemantics { },
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = Spacing.extraSmall, vertical = Spacing.extraSmall),
            )
            RecommendedPhotoGrid(photoUris = date.photoUris, isFlowing = isFront && !isHeld)
            Text(
                text = countLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.extraSmall),
            )
        }
    }
}

/**
 * 같은 크기 사진 3열 그리드. [VISIBLE_ROWS] 줄까지는 멈춰 있고, 더 있으면 [isFlowing] 동안 위로 흐른다.
 *
 * 끝에 닿으면 첫 줄이 아래에서 이어 올라온다 — 줄을 두 번 이어 그리고 한 바퀴 높이만큼 올라가면 0 으로 되돌린다. 마지막 줄이
 * 덜 차면 앞 사진으로 채워 이음매에 빈칸이 생기지 않게 한다. 멈췄다 다시 흐를 때는 멈춘 자리에서 잇는다. 기기에서 애니메이션을
 * 꺼 둔 사용자에게는 흐르지 않고 첫 세 줄만 보여 준다.
 */
@Composable
private fun RecommendedPhotoGrid(
    photoUris: List<String>,
    isFlowing: Boolean,
) {
    val context = LocalContext.current
    val isAnimationEnabled =
        remember(context) {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        }
    val rows = photoUris.chunked(GRID_COLUMNS)
    if (rows.size <= VISIBLE_ROWS) {
        // 사진이 적은 날도 세 줄 높이를 지킨다. 카드 키가 다르면 뒤 카드 아랫단이 앞 카드 밑으로 삐져나온다.
        PhotoGridRows(rows = rows + List(VISIBLE_ROWS - rows.size) { emptyList() }, modifier = Modifier.clip(MaterialTheme.shapes.medium))
        return
    }
    val filled = photoUris + photoUris.take((GRID_COLUMNS - photoUris.size % GRID_COLUMNS) % GRID_COLUMNS)
    val loopRows = filled.chunked(GRID_COLUMNS)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val rowStep = (maxWidth - GridSpacing * (GRID_COLUMNS - 1)) / GRID_COLUMNS + GridSpacing
        val loopPx = with(LocalDensity.current) { (rowStep * loopRows.size).toPx() }
        val offset = remember(loopRows) { Animatable(0f) }
        LaunchedEffect(isFlowing, isAnimationEnabled, loopPx) {
            if (!isFlowing || !isAnimationEnabled) return@LaunchedEffect
            // 첫 화면을 잠깐 보여 준 뒤 흐른다. 뜨자마자 움직이면 무엇이 흐르는지 못 읽는다.
            delay(FLOW_START_DELAY_MILLIS)
            while (true) {
                val remaining = (loopPx - offset.value) / loopPx
                offset.animateTo(loopPx, tween((remaining * loopRows.size * ROW_FLOW_MILLIS).toInt(), easing = LinearEasing))
                offset.snapTo(0f)
            }
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(rowStep * VISIBLE_ROWS - GridSpacing)
                    .clip(MaterialTheme.shapes.medium),
        ) {
            PhotoGridRows(
                rows = loopRows + loopRows.take(VISIBLE_ROWS),
                modifier =
                    Modifier
                        .wrapContentHeight(align = Alignment.Top, unbounded = true)
                        .graphicsLayer { translationY = -offset.value },
            )
        }
    }
}

/** 그리드 줄들. 덜 찬 줄도 칸 크기는 같다 — 사진이 두세 장뿐인 날도 칸이 커지지 않는다. 빈칸은 옅게 칠해 자리만 남긴다. */
@Composable
private fun PhotoGridRows(
    rows: List<List<String>>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GridSpacing)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(GridSpacing)) {
                repeat(GRID_COLUMNS) { index ->
                    val uri = row.getOrNull(index)
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .then(
                                    if (uri == null) {
                                        Modifier.background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = EMPTY_CELL_ALPHA),
                                            MaterialTheme.shapes.small,
                                        )
                                    } else {
                                        Modifier
                                    },
                                ),
                    ) {
                        if (uri != null) GridPhoto(uri = uri)
                    }
                }
            }
        }
    }
}

@Composable
private fun GridPhoto(uri: String) {
    AsyncImage(
        model = uri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

/** `찍은 사진 12장 · 일정 2개`. 일정이 없으면 사진만 적는다. */
internal fun recommendedCountLabel(date: RecommendedRecordDate): String =
    buildString {
        append("찍은 사진 ${date.capturedPhotoCount}장")
        if (date.calendarEventCount > 0) append(" · 일정 ${date.calendarEventCount}개")
    }

private val RecommendedDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREA)

/** 덮개 위 글씨. 테마와 상관없이 검은 덮개 위라 밝은 색으로 고정한다. */
private val OnDim = Color.White
private val OnDimVariant = Color.White.copy(alpha = 0.72f)
private val OnDimMuted = Color.White.copy(alpha = 0.32f)
private const val DIM_ALPHA = 0.9f

private val CardWidth = 264.dp
private val StackStepX = 18.dp
private val StackStepY = 16.dp

/** 뒤 카드가 기울며 아래로 삐져나오는 높이. 카드 폭 264dp 를 10° 기울였을 때 모서리가 내려오는 만큼에 여유를 더했다. */
private val StackTiltOverhang = 28.dp
private const val VISIBLE_CARDS = 3
private const val BACK_TILT_DEGREES = 5f
private const val BACK_SHRINK = 0.06f
private const val BACK_FADE = 0.25f
private const val FRONT_TILT_DEGREES = 14f
private const val FRONT_FADE = 0.6f
private val SwipeThreshold = 72.dp
private val ExitOffsetX = 360.dp
private val ExitOffsetY = 220.dp

/** 끝 카드에서 더 없는 쪽으로 끌 때 손가락을 따라가는 비율. */
private const val EDGE_DRAG_RESISTANCE = 0.3f
private const val FLY_MILLIS = 260
private const val SHIFT_MILLIS = 280
private val CardElevation = 8.dp

private val ButtonHeight = 52.dp
private val ButtonCornerRadius = 12.dp
private val PageDotSize = 6.dp
private val PageDotTouchSize = 24.dp
private val DeckArrowTouchSize = 40.dp
private val DeckArrowIconSize = 20.dp
private val GridSpacing = 4.dp
private const val GRID_COLUMNS = 3
private const val VISIBLE_ROWS = 3
private const val EMPTY_CELL_ALPHA = 0.5f

/** 한 줄이 올라가는 데 걸리는 시간. 사진을 알아볼 수 있을 만큼 느리게 둔다. */
private const val ROW_FLOW_MILLIS = 3200
private const val FLOW_START_DELAY_MILLIS = 1200L

private val PreviewDates =
    listOf(
        RecommendedRecordDate(LocalDate.of(2026, 10, 18), capturedPhotoCount = 40, calendarEventCount = 2, photoUris = List(24) { "" }),
        RecommendedRecordDate(LocalDate.of(2026, 10, 11), capturedPhotoCount = 6, calendarEventCount = 0, photoUris = List(6) { "" }),
        RecommendedRecordDate(LocalDate.of(2026, 10, 3), capturedPhotoCount = 3, calendarEventCount = 1, photoUris = List(3) { "" }),
    )

@Preview(name = "추천 날짜 카드 더미 - 라이트", showBackground = true, backgroundColor = 0xFF333333)
@Composable
private fun HomeDateRecommendationLightPreview() {
    LaimoryTheme {
        HomeDateRecommendationContent(HomeDateRecommendation(PreviewDates), {}, {}, {}, {})
    }
}

@Preview(name = "추천 날짜 카드 더미 - 다크", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun HomeDateRecommendationDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        HomeDateRecommendationContent(HomeDateRecommendation(PreviewDates), {}, {}, {}, {})
    }
}

@Preview(name = "추천 날짜 - 추천 없음", showBackground = true, backgroundColor = 0xFF333333)
@Composable
private fun HomeDateRecommendationEmptyPreview() {
    LaimoryTheme {
        HomeDateRecommendationContent(HomeDateRecommendation(emptyList()), {}, {}, {}, {})
    }
}

@Preview(name = "추천 날짜 - 사진 권한 없음", showBackground = true, backgroundColor = 0xFF333333)
@Composable
private fun HomeDateRecommendationNoAccessPreview() {
    LaimoryTheme {
        HomeDateRecommendationContent(HomeDateRecommendation(emptyList(), HomeRecommendationPhotoAccess.NONE), {}, {}, {}, {})
    }
}
