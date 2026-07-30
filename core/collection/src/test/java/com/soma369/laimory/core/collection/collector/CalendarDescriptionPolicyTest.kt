package com.soma369.laimory.core.collection.collector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.regex.Pattern

internal class CalendarDescriptionPolicyTest {
    @Test
    fun null은_null로_유지한다() {
        assertNull(CalendarDescriptionPolicy.limit(null))
    }

    @Test
    fun 빈_문자열은_빈_문자열로_유지한다() {
        assertEquals("", CalendarDescriptionPolicy.limit(""))
    }

    @Test
    fun 제한_이하의_앞뒤_공백과_줄바꿈은_그대로_유지한다() {
        val description = "  회의 안내\n참석 부탁드립니다.  "

        assertEquals(description, CalendarDescriptionPolicy.limit(description))
    }

    @Test
    fun 정확히_500_grapheme이면_원문을_유지한다() {
        val description = "가".repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT)

        assertEquals(description, CalendarDescriptionPolicy.limit(description))
    }

    @Test
    fun 제한을_초과하면_499_grapheme과_말줄임표로_축약한다() {
        val description = "가".repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT + 1)

        val result = CalendarDescriptionPolicy.limit(description)

        assertEquals("가".repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT - 1) + "…", result)
        assertEquals(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT, result.graphemeCount())
    }

    @Test
    fun 결합_자모를_분리하지_않는다() {
        val combinedJamo = "\u1100\u1161"
        val description = combinedJamo.repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT + 1)

        val result = CalendarDescriptionPolicy.limit(description)

        assertEquals(combinedJamo.repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT - 1) + "…", result)
        assertEquals(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT, result.graphemeCount())
    }

    @Test
    fun 결합_문자를_분리하지_않는다() {
        val combiningCharacter = "e\u0301"
        val description = combiningCharacter.repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT + 1)

        val result = CalendarDescriptionPolicy.limit(description)

        assertEquals(combiningCharacter.repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT - 1) + "…", result)
        assertEquals(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT, result.graphemeCount())
    }

    @Test
    fun 단일_이모지를_분리하지_않는다() {
        val emoji = "👍🏽"
        val description = emoji.repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT + 1)

        val result = CalendarDescriptionPolicy.limit(description)

        assertEquals(emoji.repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT - 1) + "…", result)
        assertEquals(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT, result.graphemeCount())
    }

    @Test
    fun ZWJ_결합_이모지를_분리하지_않는다() {
        val familyEmoji = "👨‍👩‍👧‍👦"
        val description = familyEmoji.repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT + 1)

        val result = CalendarDescriptionPolicy.limit(description)

        assertEquals(familyEmoji.repeat(CalendarDescriptionPolicy.MAX_GRAPHEME_COUNT - 1) + "…", result)
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
