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
package com.nooblol.smartnoob.core.domain.model.scenario

import com.nooblol.smartnoob.core.base.ScenarioStats
import com.nooblol.smartnoob.core.base.interfaces.Identifiable
import com.nooblol.smartnoob.core.base.identifier.Identifier

/**
 * Scenario of events.
 *
 * @param id the unique identifier for the scenario.
 * @param name the name of the scenario.
 * @param detectionQuality the quality of the detection algorithm. Lower value means faster detection but poorer
 *                         quality, while higher values means better and slower detection.
 * @param randomize tells if the actions values should be randomized a bit.
 * @param eventCount the number of events in this scenario. Default value is 0.
 */
data class Scenario(
    override val id: Identifier,
    val name: String,
    val detectionQuality: Int,
    val randomize: Boolean = false,
    val eventCount: Int = 0,
    val stats: ScenarioStats? = null,
): Identifiable
