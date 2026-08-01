package com.ayni.mobile.data.proximity

import java.util.UUID

object ProximityProtocol {
    val SERVICE_UUID: UUID = UUID.fromString("7c7b5d20-74f1-4d76-a8c9-41594e594e49")

    const val PROTOCOL_VERSION: Byte = 1
    const val MESSAGE_TYPE_SOS: Byte = 1
    const val PEER_ID_SIZE = 8
    const val PAYLOAD_SIZE = 2 + PEER_ID_SIZE
}

