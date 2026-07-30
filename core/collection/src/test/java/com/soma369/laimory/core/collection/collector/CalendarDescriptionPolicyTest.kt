package com.soma369.laimory.core.collection.collector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.regex.Pattern

internal class CalendarDescriptionPolicyTest {
    @Test
    fun `null은 null로 유지한다`() {
        assertNull(CalendarDescriptionPolicy.limit(null))
    }

    @Test
    fun `빈 문자열은 빈 문자열로 유지한다`() {
        assertEquals("", CalendarDescriptionPolicy.limit(""))
    }

    @Test
    fun `제한 이하의 앞뒤 공백과 줄바꿈은 그대로 유지한다`() {
        val description = "  회의 안내\n참석 부탁드립니다.  "

        assertEquals(description, CalendarDescriptionPolicy.limit(description))
    }

    @Test
    fun `정확히 500 grapheme이면 원문을 유지한다`() {
        val description = "가".repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT)

        assertEquals(description, CalendarDescriptionPolicy.limit(description))
    }

    @Test
    fun `제한을 초과하면 499 grapheme과 말줄임표로 축약한다`() {
        val description = "가".repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT + 1)

        val result = CalendarDescriptionPolicy.limit(description)

        assertEquals("가".repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT - 1) + "…", result)
        assertEquals(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT, result.graphemeCount())
    }

    @Test
    fun `결합 자모를 분리하지 않는다`() {
        val combinedJamo = "\u1100\u1161"
        val description = combinedJamo.repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT + 1)

        val result = CalendarDescriptionPolicy.limit(description)

        assertEquals(combinedJamo.repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT - 1) + "…", result)
        assertEquals(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT, result.graphemeCount())
    }

    @Test
    fun `결합 문자를 분리하지 않는다`() {
        val combiningCharacter = "e\u0301"
        val description = combiningCharacter.repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT + 1)

        val result = CalendarDescriptionPolicy.limit(description)

        assertEquals(combiningCharacter.repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT - 1) + "…", result)
        assertEquals(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT, result.graphemeCount())
    }

    @Test
    fun `단일 이모지를 분리하지 않는다`() {
        val emoji = "👍🏽"
        val description = emoji.repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT + 1)

        val result = CalendarDescriptionPolicy.limit(description)

        assertEquals(emoji.repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT - 1) + "…", result)
        assertEquals(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT, result.graphemeCount())
    }

    @Test
    fun `ZWJ 결합 이모지를 분리하지 않는다`() {
        val familyEmoji = "👨‍👩‍👧‍👦"
        val description = familyEmoji.repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT + 1)

        val result = CalendarDescriptionPolicy.limit(description)

        assertEquals(familyEmoji.repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT - 1) + "…", result)
        assertEquals(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT, result.graphemeCount())
    }

    @Test
    fun `서로 다른 grapheme이 절단 경계에 있어도 ZWJ 이모지를 온전히 유지한다`() {
        val familyEmoji = "👨‍👩‍👧‍👦"
        val description = "a".repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT - 2) + familyEmoji + "나" + "뒤"

        val result = CalendarDescriptionPolicy.limit(description)

        assertEquals("a".repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT - 2) + familyEmoji + "…", result)
        assertEquals(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT, result.graphemeCount())
    }

    @Test
    fun `500번째 ZWJ 이모지는 일부 코드 유닛을 남기지 않고 제외한다`() {
        val familyEmoji = "👨‍👩‍👧‍👦"
        val description = "a".repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT - 1) + familyEmoji + "뒤"

        val result = CalendarDescriptionPolicy.limit(description)

        assertEquals("a".repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT - 1) + "…", result)
        assertEquals(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT, result.graphemeCount())
    }

    private fun String?.graphemeCount(): Int {
        requireNotNull(this)
        val matcher = GRAPHEME_PATTERN.matcher(this)
        var count = 0
        while (matcher.find()) count += 1
        return count
    }

    private companion object {
        val GRAPHEME_PATTERN: Pattern = Pattern.compile("\\X")
    }
}
