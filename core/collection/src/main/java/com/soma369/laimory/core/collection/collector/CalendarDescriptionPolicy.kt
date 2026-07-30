package com.soma369.laimory.core.collection.collector

import java.util.regex.Pattern

/**
 * 캘린더 설명을 사용자 인지 문자(grapheme cluster) 기준으로 제한한다.
 *
 * 최대 [MAX_GRAPHEME_COUNT]개를 허용하며, 초과하면 최대치에서 말줄임표 한 자리만큼 뺀
 * grapheme 뒤에 말줄임표를 붙인다.
 * 제목·장소와 달리 장문의 초대문이 들어오는 description만 대상으로 하며,
 * null·빈 문자열·앞뒤 공백과 줄바꿈은 원문 그대로 유지한다.
 */
internal object CalendarDescriptionPolicy {
    const val MAX_GRAPHEME_COUNT = 500

    private const val ELLIPSIS = "…"
    private const val CONTENT_GRAPHEME_COUNT = MAX_GRAPHEME_COUNT - 1
    private val graphemePattern = Pattern.compile("\\X")

    fun limit(description: String?): String? {
        description ?: return null

        val matcher = graphemePattern.matcher(description)
        var graphemeCount = 0
        var contentEndIndex = 0

        while (matcher.find()) {
            graphemeCount += 1
            if (graphemeCount == CONTENT_GRAPHEME_COUNT) {
                contentEndIndex = matcher.end()
            }
            if (graphemeCount > MAX_GRAPHEME_COUNT) {
                return description.substring(0, contentEndIndex) + ELLIPSIS
            }
        }

        return description
    }
}
