package com.soma369.laimory.feature.timeline.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.ui.component.LaimoryDialog
import com.soma369.laimory.core.ui.component.LaimoryDialogButtons
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.core.ui.R as UiR

/**
 * 저장 전(DRAFT) 타임라인 맨 위의 AI 안내 한 줄. 누르면 [TimelineAiNoticeDialog] 가 열린다.
 *
 * 닫는 버튼이 없다 — 저장 전에는 언제 열어도 보인다. AI 가 만든 내용을 확인하는 단계가 바로 이 화면이라,
 * 한 번 닫으면 다음에 열 때 그 사실을 잊는다.
 *
 * 빨강은 얇은 테두리와 아이콘에만 쓴다. 바탕까지 칠하면 오류(실패·한도 초과)처럼 읽히고, 이 띠는 열 때마다
 * 보이는 자리라 무거워진다. 문구와 `자세히` 는 같은 진한 글자색이다 — 읽어야 할 것은 문구라 옅게 두지 않는다.
 */
@Composable
internal fun TimelineAiNotice(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(NoticeCornerRadius)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.error), shape)
                .clickable(onClickLabel = "AI 안내 자세히 보기", role = Role.Button, onClick = onClick)
                .padding(
                    start = Spacing.medium,
                    end = Spacing.small,
                    top = NoticeVerticalPadding,
                    bottom = NoticeVerticalPadding,
                ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(UiR.drawable.ico_default_sparkle),
            contentDescription = null,
            modifier = Modifier.size(NoticeIconSize),
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            text = "AI가 만든 기록이라 실제와 다를 수 있어요",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(MoreIconGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "자세히",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                painter = painterResource(UiR.drawable.ico_default_caret_right),
                contentDescription = null,
                modifier = Modifier.size(MoreIconSize),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * AI 안내 자세히 보기.
 *
 * 모델은 사용자가 알아보는 이름(OpenAI GPT)까지만 쓰고 버전(5.6 · mini 등)은 쓰지 않는다. AI 서버는 단계마다
 * 다른 GPT 모델을 쓰고 환경 설정만으로 바꾸므로, 버전까지 박으면 앱 업데이트 없이 문구가 틀린다.
 * **AI 서버가 GPT 가 아닌 모델로 옮기면 이 문구도 함께 바꾼다.**
 */
@Composable
internal fun TimelineAiNoticeDialog(onDismiss: () -> Unit) {
    LaimoryDialog(
        title = "AI가 만든 기록이에요",
        body =
            "OpenAI GPT 모델이 고른 사진·위치·일정·알림을 바탕으로 이 기록을 만들었어요.\n\n" +
                "AI가 만든 결과라 시간·장소·내용이 실제와 다를 수 있어요. " +
                "저장하기 전에 한 번 확인하고, 다른 부분은 직접 고쳐 주세요.",
        buttons = LaimoryDialogButtons.One(label = "확인", onClick = onDismiss),
        onDismissRequest = onDismiss,
    )
}

private val NoticeCornerRadius = 12.dp
private val NoticeVerticalPadding = 10.dp
private val NoticeIconSize = 16.dp
private val MoreIconSize = 14.dp
private val MoreIconGap = 2.dp

@Preview(showBackground = true)
@Composable
private fun TimelineAiNoticePreview() {
    LaimoryTheme {
        TimelineAiNotice(onClick = {}, modifier = Modifier.padding(Spacing.large))
    }
}

@Preview(showBackground = true)
@Composable
private fun TimelineAiNoticeDialogPreview() {
    LaimoryTheme {
        TimelineAiNoticeDialog(onDismiss = {})
    }
}
