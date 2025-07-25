/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.nooblol.smartnoob.core.domain.model

import com.nooblol.smartnoob.core.base.interfaces.Completable
import com.nooblol.smartnoob.core.database.entity.CounterOperationValueType

sealed class CounterOperationValue : Completable {

    abstract val value: Any

    data class Number(override val value: Int): CounterOperationValue() {
        override fun isComplete(): Boolean = value >= 0
    }

    data class Counter(override val value: String): CounterOperationValue() {
        override fun isComplete(): Boolean = value.isNotEmpty()
    }

    internal companion object {

        fun getCounterOperationValue(
            type: CounterOperationValueType?,
            numberValue: Int?,
            counterName: String?,
        ) = when (type ?: CounterOperationValueType.NUMBER) {
            CounterOperationValueType.COUNTER -> Counter(counterName ?: "")
            CounterOperationValueType.NUMBER -> Number(numberValue ?: 0)
        }
    }
}