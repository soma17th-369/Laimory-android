package com.soma369.laimory.feature.timeline.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.domain.model.timeline.PlaceSubtitle
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.R as UiR

/**
 * 장소 자리를 칩으로 둔 부제 문장. 칩을 누르면 장소 고르기 시트가 열린다.
 *
 * 앞뒤 문장을 **어절 단위로 흘려** 칩과 한 흐름으로 잇는다. 앞 문장을 통째로 한 덩어리로 두면 문장이
 * 조금만 길어도 칩이 다음 줄로 밀려 문장이 끊겨 읽힌다. 칩 바로 뒤 어절(`에서` 등)은 띄우지 않고
 * 붙인다 — 문장 원문에 공백이 없는 자리다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TimelinePlaceSentence(
    sentence: PlaceSubtitle,
    onPlaceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        itemVerticalAlignment = Alignment.CenterVertically,
        verticalArrangement = Arrangement.spacedBy(SentenceLineGap),
    ) {
        sentence.before.words().forEach { SentenceWord(it) }
        TimelinePlaceChip(
            place = sentence.place,
            onClick = onPlaceClick,
            // 칩 앞뒤 공백은 원문이 정한다. 원문에 공백이 있으면 그 어절이 이미 품고 있다.
            modifier = Modifier.padding(end = if (sentence.after.startsWith(" ")) ChipSpacing else 0.dp),
        )
        sentence.after.words().forEach { SentenceWord(it) }
    }
}

/** Figma `PlaceChip`(3497:8976) — 핀 + 지금 장소 + 펼침 화살표, 완전히 둥근 칩. */
@Composable
internal fun TimelinePlaceChip(
    place: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = "장소 $place, 바꾸기" }
                .padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(UiR.drawable.ico_setting_datasource_location),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = contentColor,
        )
        Text(
            text = place,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // 긴 장소명이 한 줄을 다 차지해 문장이 칩만 남지 않게 한다.
            modifier = Modifier.widthIn(max = ChipTextMaxWidth),
        )
        Icon(
            painter = painterResource(UiR.drawable.ico_default_caret_down),
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = contentColor,
        )
    }
}

@Composable
private fun SentenceWord(word: String) {
    Text(
        text = word,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** 공백을 앞 어절 끝에 붙여 나눈다. 앞 공백은 칩 여백으로 대신하므로 버린다. */
private fun String.words(): List<String> = Regex("""\S+\s*""").findAll(this).map { it.value }.toList()

private val ChipSpacing = 4.dp
private val SentenceLineGap = 4.dp
private val ChipTextMaxWidth = 180.dp

@Preview(showBackground = true, widthDp = 284)
@Composable
private fun TimelinePlaceSentencePreview() {
    LaimoryTheme {
        TimelinePlaceSentence(
            sentence = PlaceSubtitle(before = "", place = "어니언 성수", after = "에서 친구와 파스타를 먹고 한참 이야기했다"),
            onPlaceClick = {},
        )
    }
}
