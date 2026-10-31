package com.soma369.laimory.feature.timeline.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.timeline.PlaceCandidate

/**
 * 장소 고르기 목록의 한 줄. 서버 후보면 [candidate] 가 있고, 후보에 없는 지금 장소(예전에 직접 입력한 값)면 null 이다.
 *
 * 이름이 같은 후보가 여럿일 수 있어(좌표만 다름) 이름으로 줄을 가리지 않는다. 고른 줄은 목록 위치로 든다.
 */
@Immutable
data class PlaceSheetOption(
    val name: String,
    val candidate: PlaceCandidate?,
)
