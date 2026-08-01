package com.ayni.mobile.data.iot.local

/**
 * A repeated BLE notification may refresh sensor-provided values, but it must
 * not rewrite metadata that was already decided and stored by the edge.
 */
internal fun stabilizeHitRedelivery(
    incoming: HitMeasurementEntity,
    previous: HitMeasurementEntity?,
): HitMeasurementEntity =
    previous?.let {
        incoming.copy(
            receivedAtEpochMs = it.receivedAtEpochMs,
            eventId = it.eventId,
            includedInFinalState = it.includedInFinalState,
            acquisitionMode = it.acquisitionMode,
        )
    } ?: incoming

/**
 * `receivedAtEpochMs` means first reception by this phone, not most recent
 * retransmission through GET_TRACE or BLE synchronization.
 */
internal fun stabilizeTraceRedelivery(
    incoming: MeasurementTraceEntity,
    previous: MeasurementTraceEntity?,
): MeasurementTraceEntity =
    previous?.let {
        incoming.copy(receivedAtEpochMs = it.receivedAtEpochMs)
    } ?: incoming

internal fun stabilizeStructuralStateRedelivery(
    incoming: StructuralStateEntity,
    previous: StructuralStateEntity?,
): StructuralStateEntity =
    previous?.let {
        incoming.copy(receivedAtEpochMs = it.receivedAtEpochMs)
    } ?: incoming
