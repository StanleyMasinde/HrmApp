package com.stanleymasinde.hrmapp.ble

import java.util.UUID

object HrmUuids {
    val HEART_RATE_SERVICE: UUID = UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB")
    val HR_MEASUREMENT: UUID     = UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB")
    val CCCD: UUID               = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
}
