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
package com.nooblol.smartnoob.core.domain.model.action.toggleevent

import com.nooblol.smartnoob.core.base.identifier.Identifier
import com.nooblol.smartnoob.core.base.interfaces.Completable
import com.nooblol.smartnoob.core.base.interfaces.Identifiable
import com.nooblol.smartnoob.core.domain.model.action.ToggleEvent

data class EventToggle(
    override val id: Identifier,
    val actionId: Identifier,
    val targetEventId: Identifier?,
    val toggleType: ToggleEvent.ToggleType,
): Identifiable, Completable {

    override fun isComplete(): Boolean = true
}