package com.soma369.laimory.feature.home.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.ui.permission.DataSourceStatus
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.core.ui.theme.laimoryColors
import com.soma369.laimory.feature.home.state.HomeSourceKind

/**
 * 홈 원천 카드(Figma `Home / SourceCard` 2512:1136).
 *
 * 구성은 **분류 행 → 내용 슬롯 → 본문 한 줄** 이다. 내용 슬롯만 원천마다 다르고 나머지는 같다.
 *
 * 카드 **전체가 탭 대상**이되 어디로 가는지는 데이터가 정한다 — 도트가 꺼져도 볼 것이 있으면
 * 상세를 연다. 권한을 더 받는 일은 본문 오른쪽 [permissionAction] 이 따로 맡는다.
 */
@Composable
internal fun HomeSourceCard(
    kind: HomeSourceKind,
    status: DataSourceStatus,
    body: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    permissionAction: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = onClick ?: {},
        enabled = onClick != null,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.large, vertical = Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            CategoryRow(kind = kind, status = status)
            content()
            BodyRow(body = body, permissionAction = permissionAction)
        }
    }
}

@Composable
private fun CategoryRow(
    kind: HomeSourceKind,
    status: DataSourceStatus,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(kind.iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = kind.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StatusDot(status = status)
    }
}

/**
 * 권한 도트.
 *
 * 색만으로 나누지 않는다 — 스크린 리더가 읽을 이름을 붙이고, 본문 문구도 함께 바뀐다.
 * 허용/미허용 이분법을 쓰지 않는 이유는 [DataSourceStatus] 에 적혀 있다.
 */
@Composable
private fun StatusDot(status: DataSourceStatus) {
    Box(
        modifier =
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(status.dotColor())
                .clearAndSetSemantics { contentDescription = status.dotLabel() },
    )
}

@Composable
private fun DataSourceStatus.dotColor(): Color =
    when (this) {
        DataSourceStatus.GRANTED -> MaterialTheme.laimoryColors.success
        DataSourceStatus.LIMITED -> MaterialTheme.laimoryColors.warning
        DataSourceStatus.DENIED, DataSourceStatus.UNSUPPORTED -> MaterialTheme.colorScheme.outline
    }

private fun DataSourceStatus.dotLabel(): String =
    when (this) {
        DataSourceStatus.GRANTED -> "권한 허용됨"
        DataSourceStatus.LIMITED -> "권한 일부 허용됨"
        DataSourceStatus.DENIED -> "권한 꺼짐"
        DataSourceStatus.UNSUPPORTED -> "이 기기에서는 지원하지 않음"
    }

@Composable
private fun BodyRow(
    body: String,
    permissionAction: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (permissionAction != null) {
            // 카드 본체는 상세를 열고, 권한을 더 받는 일은 여기서 따로 맡는다.
            Text(
                text = "허용 →",
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(Spacing.small))
                        .clickable(onClick = permissionAction)
                        .padding(horizontal = Spacing.extraSmall),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.laimoryColors.primaryText,
            )
        }
    }
}
