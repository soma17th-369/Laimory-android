package com.soma369.laimory.core.data.model.timeline.request

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateTimelineEventPlaceRequestTest {
    /** 요청을 보낼 때 쓰는 설정과 같다(NetworkModule 의 send 쪽). */
    @OptIn(ExperimentalSerializationApi::class)
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = true
            coerceInputValues = true
        }

    @Test
    fun `좌표가 있으면 장소와 함께 보낸다`() {
        val body = json.encodeToString(UpdateTimelineEventPlaceRequest("△△공원", 37.5444, 127.0374))

        assertEquals("""{"place":"△△공원","latitude":37.5444,"longitude":127.0374}""", body)
    }

    @Test
    fun `좌표가 없으면 null 로 드러내 보낸다`() {
        val body = json.encodeToString(UpdateTimelineEventPlaceRequest("직접 쓴 장소", null, null))

        assertEquals("""{"place":"직접 쓴 장소","latitude":null,"longitude":null}""", body)
    }
}
