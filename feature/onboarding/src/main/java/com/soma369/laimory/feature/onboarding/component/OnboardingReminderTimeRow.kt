package com.soma369.laimory.feature.onboarding.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import com.soma369.laimory.core.ui.component.timepicker.toHourLabel
import com.soma369.laimory.core.ui.reminder.DAILY_REMINDER_TIME_LABEL
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import java.time.LocalTime
import com.soma369.laimory.core.ui.R as UiR

/**
 * 알림 장의 리마인더 시간대 줄. 누르면 시간대 시트를 연다.
 *
 * 설정의 같은 줄은 목록 속 하위 줄이라 값만 두지만, 여기는 처음 보는 화면이라 칸으로 감싸고 화살표를 붙여 눌러서
 * 바꿀 수 있다는 것을 드러낸다.
 */
@Composable
internal fun OnboardingReminderTimeRow(
    time: LocalTime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = ROW_MIN_HEIGHT)
                .clip(ROW_SHAPE)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(start = Spacing.large, end = Spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = DAILY_REMINDER_TIME_LABEL,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = time.toHourLabel(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            painter = painterResource(UiR.drawable.ico_default_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(CHEVRON_SIZE),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val ROW_SHAPE = RoundedCornerShape(12.dp)

/** 시안의 줄 높이(위아래 14 + 글자 24). 터치 최소 크기(48)도 넘는다. */
private val ROW_MIN_HEIGHT = 52.dp

private val CHEVRON_SIZE = 16.dp

@Preview(showBackground = true)
@Composable
private fun OnboardingReminderTimeRowPreview() {
    LaimoryTheme {
        OnboardingReminderTimeRow(time = LocalTime.of(21, 0), onClick = {}, modifier = Modifier.padding(Spacing.large))
    }
}
