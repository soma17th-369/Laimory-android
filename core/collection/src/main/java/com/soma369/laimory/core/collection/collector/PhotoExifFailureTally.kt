package com.soma369.laimory.core.collection.collector

import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger

/**
 * 사진 한 묶음을 읽는 동안 EXIF 위치 읽기 실패를 모았다가 한 줄로 남긴다.
 *
 * 실패는 대개 한 사진의 사정이 아니라 권한(`ACCESS_MEDIA_LOCATION`)처럼 묶음 전체의 사정이다. 한 건씩
 * 남기면 같은 줄이 사진 수만큼 반복돼 브레드크럼 창을 밀어낸다. 건수와 첫 실패의 종류면 충분하다.
 */
internal class PhotoExifFailureTally {
    private var count = 0
    private var firstType: String? = null

    fun record(error: Throwable) {
        count++
        if (firstType == null) firstType = error::class.simpleName
    }

    /** 실패가 있었으면 [total] 건 중 몇 건이었는지 남긴다. */
    fun report(total: Int) {
        if (count == 0) return
        Logger.w(LogDomain.COLLECTION, "사진 EXIF 위치 읽기 실패 ${count}건/${total}건: $firstType")
    }
}
