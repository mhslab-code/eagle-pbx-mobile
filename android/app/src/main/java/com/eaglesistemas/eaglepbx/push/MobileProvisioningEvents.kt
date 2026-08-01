package com.eaglesistemas.eaglepbx.push

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class MobileProvisioningEvent(
    val type: String,
    val deviceId: String
)

object MobileProvisioningEvents {
    private val mutableEvents = MutableSharedFlow<MobileProvisioningEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val events = mutableEvents.asSharedFlow()

    fun publish(type: String, deviceId: String) {
        mutableEvents.tryEmit(MobileProvisioningEvent(type, deviceId))
    }
}
