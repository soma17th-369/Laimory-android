package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable

/**
 * 홈 위치 카드가 보여 주는 `가장 오래 머문 곳`.
 *
 * 고르는 기준은 전체 체류 시간이 아니라 **기록 창과 겹친 시간**이다. 창 포함 판정은 구간이
 * 겹치면 참이라, 전날 밤부터 이어져 오늘 창에 10분만 걸친 체류가 오늘 세 시간 머문 장소를
 * 이길 수 있다.
 *
 * 좌표는 다시 해석할 때만 쓰고 **사용자에게 숫자로 보이지 않는다.**
 */
@Immutable
data class HomeStayPlace(
    val rawId: String,
    val latitude: Double,
    val longitude: Double,
    /** 광역 — `경기도`, `서울특별시`. */
    val city: String? = null,
    /** 시·군·구 — `오산시`, `강남구`. */
    val district: String? = null,
    /** 한 줄 전체 주소. 층위가 없을 때 대신 쓴다. */
    val line: String? = null,
) {
    /**
     * 카드에 적을 문구. 층위가 있으면 `경기도 오산시`, 없으면 한 줄 주소, 둘 다 없으면 null.
     *
     * 한쪽 층위만 있으면 그것만 적는다.
     */
    val label: String?
        get() = listOfNotNull(city, district).takeIf { it.isNotEmpty() }?.joinToString(" ") ?: line

    /**
     * 층위를 채우러 다시 해석해야 하는가.
     *
     * 판정 기준이 [line] 유무가 **아니다.** 한 줄만 저장된 기존 항목을 해석 완료로 보면 층위가
     * 영영 비어, 새 체류는 `서울시 · 역삼동` 인데 기존 저장분은 한 줄 전체가 뜨는 두 모양이 섞인다.
     */
    val needsResolution: Boolean
        get() = city == null && district == null
}
