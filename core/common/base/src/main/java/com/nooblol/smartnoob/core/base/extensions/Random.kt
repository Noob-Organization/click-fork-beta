/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.nooblol.smartnoob.core.base.extensions

import android.graphics.PointF
import android.graphics.RectF

import kotlin.random.Random

fun Random.nextFloat(from: Float, until: Float): Float =
    (until - from) * nextFloat()

fun Random.nextPositionIn(area: RectF): PointF =
    PointF(nextFloat(area.left, area.right), nextFloat(area.top, area.bottom))

fun Random.nextIntInOffset(value: Int, offset: Int): Int = nextInt(
    from = value - offset,
    until = value + offset + 1,
)

fun Random.nextLongInOffset(value: Long, offset: Long): Long = nextLong(
    from = value - offset,
    until = value + offset + 1,
)
