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
package com.nooblol.smartnoob.core.processing.utils

import android.graphics.Rect

import com.nooblol.smartnoob.core.domain.model.AND
import com.nooblol.smartnoob.core.domain.model.ConditionOperator
import com.nooblol.smartnoob.core.domain.model.DetectionType
import com.nooblol.smartnoob.core.base.identifier.Identifier
import com.nooblol.smartnoob.core.domain.model.action.Action
import com.nooblol.smartnoob.core.domain.model.condition.ImageCondition
import com.nooblol.smartnoob.core.domain.model.event.ImageEvent

/** Test data and helpers for the detection tests. */
internal object ProcessingData {

    /** Instantiates a new event with only the useful values for the tests. */
    fun newEvent(
        id: Long = 1L,
        @ConditionOperator operator: Int = AND,
        actions: List<Action> = emptyList(),
        conditions: List<ImageCondition> = emptyList(),
        enableOnStart: Boolean = true,
    ) = ImageEvent(
        id = Identifier(databaseId = id),
        scenarioId = Identifier(databaseId = 1L),
        name = "TOTO",
        conditionOperator = operator,
        priority = 0,
        actions = actions.toMutableList(),
        conditions = conditions.toMutableList(),
        enabledOnStart = enableOnStart,
        keepDetecting = false,
    )

    /** Instantiates a new condition with only the useful values for the tests. */
    fun newCondition(
        path: String,
        area: Rect,
        threshold: Int,
        @DetectionType detectionType: Int,
        shouldBeDetected: Boolean = true,
    ) = ImageCondition(
        Identifier(databaseId = 1L),
        Identifier(databaseId = 1L),
        "TOTO",
        0,
        path,
        area,
        threshold,
        detectionType,
        shouldBeDetected,
        null
    )
}