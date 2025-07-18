/*
 * Copyright (C) 2024 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.nooblol.smartnoob.core.domain.model.condition

import android.graphics.Rect
import com.nooblol.smartnoob.core.base.identifier.Identifier
import com.nooblol.smartnoob.core.database.entity.ConditionEntity
import com.nooblol.smartnoob.core.database.entity.CounterComparisonOperation
import com.nooblol.smartnoob.core.database.entity.ConditionType
import com.nooblol.smartnoob.core.database.entity.CounterOperationValueType
import com.nooblol.smartnoob.core.domain.model.CounterOperationValue


/** @return the entity equivalent of this condition. */
internal fun ImageCondition.toEntity() = ConditionEntity(
    id = id.databaseId,
    eventId = eventId.databaseId,
    name = name,
    priority = priority,
    type = ConditionType.ON_IMAGE_DETECTED,
    path = path,
    areaLeft = area.left,
    areaTop = area.top,
    areaRight = area.right,
    areaBottom = area.bottom,
    threshold = threshold,
    detectionType = detectionType,
    shouldBeDetected = shouldBeDetected,
    detectionAreaLeft = detectionArea?.left,
    detectionAreaTop = detectionArea?.top,
    detectionAreaRight = detectionArea?.right,
    detectionAreaBottom = detectionArea?.bottom,
)

internal fun TriggerCondition.toEntity(): ConditionEntity = when (this) {
    is TriggerCondition.OnBroadcastReceived -> toBroadcastReceivedEntity()
    is TriggerCondition.OnCounterCountReached -> toCounterReachedEntity()
    is TriggerCondition.OnTimerReached -> toTimerReachedEntity()
}

private fun TriggerCondition.OnBroadcastReceived.toBroadcastReceivedEntity(): ConditionEntity =
    ConditionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        name = name,
        type = ConditionType.ON_BROADCAST_RECEIVED,
        broadcastAction = intentAction,
        priority = 0,
    )

private fun TriggerCondition.OnCounterCountReached.toCounterReachedEntity(): ConditionEntity {
    val isNumberValue = counterValue is CounterOperationValue.Number

    return ConditionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        name = name,
        type = ConditionType.ON_COUNTER_REACHED,
        counterName = counterName,
        counterComparisonOperation = comparisonOperation.toEntity(),
        counterOperationValueType = if (isNumberValue) CounterOperationValueType.NUMBER else CounterOperationValueType.COUNTER,
        counterValue = if (isNumberValue) counterValue.value as Int else null,
        counterOperationCounterName = if (isNumberValue) null else counterValue.value as String,
        priority = 0,
    )
}

private fun TriggerCondition.OnTimerReached.toTimerReachedEntity(): ConditionEntity =
    ConditionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        name = name,
        type = ConditionType.ON_TIMER_REACHED,
        timerValueMs = durationMs,
        restartWhenReached = restartWhenReached,
        priority = 0,
    )


internal fun ConditionEntity.toDomain(cleanIds: Boolean = false): Condition =
    when (type) {
        ConditionType.ON_IMAGE_DETECTED -> toDomainImageCondition(cleanIds)
        ConditionType.ON_BROADCAST_RECEIVED -> toDomainBroadcastReceived(cleanIds)
        ConditionType.ON_COUNTER_REACHED -> toDomainCounterReached(cleanIds)
        ConditionType.ON_TIMER_REACHED -> toDomainTimerReached(cleanIds)
        else -> throw IllegalArgumentException("Unsupported condition type for a TriggerCondition")
    }

/** @return the condition for this entity. */
private fun ConditionEntity.toDomainImageCondition(cleanIds: Boolean = false): ImageCondition =
    ImageCondition(
        id = Identifier(id = id, asTemporary = cleanIds),
        eventId = Identifier(id = eventId, asTemporary = cleanIds),
        name = name,
        priority = priority,
        path = path!!,
        area = Rect(areaLeft!!, areaTop!!, areaRight!!, areaBottom!!),
        threshold = threshold!!,
        detectionType = detectionType!!,
        detectionArea = getDetectionArea(),
        shouldBeDetected = shouldBeDetected ?: true,
    )

private fun ConditionEntity.toDomainBroadcastReceived(cleanIds: Boolean = false): TriggerCondition =
    TriggerCondition.OnBroadcastReceived(
        id = Identifier(id = id, asTemporary = cleanIds),
        eventId = Identifier(id = eventId, asTemporary = cleanIds),
        name = name,
        intentAction = broadcastAction!!,
    )

private fun ConditionEntity.toDomainCounterReached(cleanIds: Boolean = false): TriggerCondition =
    TriggerCondition.OnCounterCountReached(
        id = Identifier(id = id, asTemporary = cleanIds),
        eventId = Identifier(id = eventId, asTemporary = cleanIds),
        name = name,
        counterName = counterName!!,
        comparisonOperation = counterComparisonOperation!!.toDomain(),
        counterValue = CounterOperationValue.getCounterOperationValue(
            type = counterOperationValueType,
            numberValue = counterValue,
            counterName = counterOperationCounterName,
        ),
    )


private fun ConditionEntity.toDomainTimerReached(cleanIds: Boolean = false): TriggerCondition =
    TriggerCondition.OnTimerReached(
        id = Identifier(id = id, asTemporary = cleanIds),
        eventId = Identifier(id = eventId, asTemporary = cleanIds),
        name = name,
        durationMs = timerValueMs!!,
        restartWhenReached = restartWhenReached!!,
    )

private fun CounterComparisonOperation.toDomain(): TriggerCondition.OnCounterCountReached.ComparisonOperation =
    TriggerCondition.OnCounterCountReached.ComparisonOperation.valueOf(name)

private fun ConditionEntity.getDetectionArea(): Rect? =
    if (detectionAreaLeft != null && detectionAreaTop != null && detectionAreaRight != null && detectionAreaBottom != null)
        Rect(detectionAreaLeft!!, detectionAreaTop!!, detectionAreaRight!!, detectionAreaBottom!!)
    else
        null