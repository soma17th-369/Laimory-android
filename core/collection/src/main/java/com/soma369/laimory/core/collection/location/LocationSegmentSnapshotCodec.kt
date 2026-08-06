package com.soma369.laimory.core.collection.location

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object LocationSegmentSnapshotCodec {
    private val json =
        Json {
            ignoreUnknownKeys = true
            classDiscriminator = "segmentState"
        }

    fun encode(snapshot: LocationSegmentSnapshot): String = json.encodeToString(snapshot)

    fun decode(value: String): LocationSegmentSnapshot = json.decodeFromString(value)
}
