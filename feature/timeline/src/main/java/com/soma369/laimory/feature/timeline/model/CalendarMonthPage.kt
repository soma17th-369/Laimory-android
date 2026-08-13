package com.soma369.laimory.feature.timeline.model

import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * 월간 pager 의 페이지 번호 ↔ 월 변환.
 *
 * 페이지 번호는 [PAGER_EPOCH_MONTH] 기준 월 오프셋과 1:1 이라 앵커 상태를 따로 들지 않아도
 * 양방향으로 옮길 수 있다. 가운데를 기준 월로 잡아 앞뒤로 사실상 제한 없이 열린다.
 */
internal fun YearMonth.toPagerPage(): Int = MONTH_PAGE_CENTER + ChronoUnit.MONTHS.between(PAGER_EPOCH_MONTH, this).toInt()

internal fun monthOfPagerPage(page: Int): YearMonth = PAGER_EPOCH_MONTH.plusMonths((page - MONTH_PAGE_CENTER).toLong())

/** 페이지 번호의 기준 월. 이 월이 [MONTH_PAGE_CENTER] 페이지에 놓인다. */
internal val PAGER_EPOCH_MONTH: YearMonth = YearMonth.of(1970, 1)

internal const val MONTH_PAGE_COUNT = Int.MAX_VALUE
internal const val MONTH_PAGE_CENTER = Int.MAX_VALUE / 2
