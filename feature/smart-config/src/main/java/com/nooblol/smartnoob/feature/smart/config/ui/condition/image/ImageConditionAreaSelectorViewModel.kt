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
package com.nooblol.smartnoob.feature.smart.config.ui.condition.image

import android.graphics.Rect

import androidx.lifecycle.ViewModel

import com.nooblol.smartnoob.core.domain.model.IN_AREA
import com.nooblol.smartnoob.feature.smart.config.domain.EditionRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

import javax.inject.Inject

class ImageConditionAreaSelectorViewModel @Inject constructor(
    editionRepository: EditionRepository,
) : ViewModel()  {


    /** The condition being configured by the user. */
    private val configuredCondition = editionRepository.editionState.editedImageConditionState
        .mapNotNull { it.value }

    /** The position at which the selector should be initialized. */
    val initialArea: Flow<SelectorUiState> = configuredCondition
        .mapNotNull { condition ->
            if (condition.detectionType != IN_AREA) null
            else SelectorUiState(
                initialArea = condition.detectionArea ?: condition.area,
                minimalArea = condition.area,
            )
        }
}

data class SelectorUiState(
    val initialArea: Rect,
    val minimalArea: Rect,
)