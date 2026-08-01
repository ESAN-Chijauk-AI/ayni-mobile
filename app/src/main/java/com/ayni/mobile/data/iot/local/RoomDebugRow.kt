package com.ayni.mobile.data.iot.local

data class RoomDebugRow(
    val tableName: String,
    val rowKey: String,
    val summary: String,
    val sortAtEpochMs: Long,
)
