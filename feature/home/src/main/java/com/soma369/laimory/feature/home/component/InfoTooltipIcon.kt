package com.soma369.laimory.feature.home.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.ui.theme.Spacing
import kotlinx.coroutines.launch
import com.soma369.laimory.core.ui.R as UiR

/**
 * 누르면 안내 말풍선을 띄우는 `i` 아이콘.
 *
 * 한 줄에 다 적으면 길어지는 설명을 접어 둘 때 쓴다. 말풍선은 다른 곳을 누를 때까지 남는다 —
 * 여러 문장을 읽을 시간이 있어야 한다. 낭독은 [label] 과 안내를 한 번에 읽는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InfoTooltipIcon(
    label: String,
    guide: String,
    modifier: Modifier = Modifier,
) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(guide) } },
        state = tooltipState,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(UiR.drawable.ico_setting_info),
            contentDescription = "$label: $guide",
            modifier =
                Modifier
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { scope.launch { tooltipState.show() } }
                    .padding(Spacing.extraSmall)
                    .size(InfoIconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val InfoIconSize = 16.dp
