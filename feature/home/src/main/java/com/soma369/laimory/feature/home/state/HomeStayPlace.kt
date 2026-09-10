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
    /** 시·군·구 — `오산시`, `강남구`. */
    val city: String? = null,
    /** 읍·면·동 — `부산동`, `역삼동`. */
    val district: String? = null,
    /** 한 줄 전체 주소. 층위가 없을 때 대신 쓴다. */
    val line: String? = null,
) {
    /**
     * 카드에 적을 문구. 층위가 있으면 `오산시 부산동`, 없으면 한 줄 주소, 둘 다 없으면 null.
     *
     * 한쪽 층위만 있으면 그것만 적는다. 한 줄 주소로 물러설 때는 나라 이름을 뗀다 — 국내
     * 사용자에게 `대한민국` 은 알려 주는 것이 없으면서 반쪽 카드의 가로폭만 먹는다.
     */
    val label: String?
        get() =
            listOfNotNull(city, district).takeIf { it.isNotEmpty() }?.joinToString(" ")
                ?: line?.removePrefix(COUNTRY_PREFIX)

    /**
     * 층위를 채우러 다시 해석해야 하는가.
     *
     * 판정 기준이 [line] 유무가 **아니다.** 한 줄만 저장된 기존 항목을 해석 완료로 보면 층위가
     * 영영 비어, 새 체류는 `강남구 역삼동` 인데 기존 저장분은 한 줄 전체가 뜨는 두 모양이 섞인다.
     *
     * 광역 이름만 들어 있는 값도 다시 묻는다 — 층위를 시·군·구로 내리기 전에 저장된 값이라,
     * 해석 완료로 보면 `서울특별시` 한 마디가 카드에 남는다.
     */
    val needsResolution: Boolean
        get() = (city == null && district == null) || city?.isProvinceName() == true

    /** `경기도`·`서울특별시` 처럼 광역 이름인가. 시·군·구는 `시`·`군`·`구` 로 끝난다. */
    private fun String.isProvinceName(): Boolean = PROVINCE_SUFFIXES.any { endsWith(it) }

    private companion object {
        /** `Geocoder` 의 한 줄 주소는 나라 이름으로 시작한다. */
        const val COUNTRY_PREFIX = "대한민국 "

        /** `오산시`·`강남구` 와 겹치지 않도록 광역만 골라내는 꼬리말. */
        val PROVINCE_SUFFIXES = listOf("특별시", "광역시", "특별자치시", "특별자치도", "도")
    }
}
