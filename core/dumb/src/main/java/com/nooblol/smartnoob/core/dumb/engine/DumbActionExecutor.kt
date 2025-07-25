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
package com.nooblol.smartnoob.core.dumb.engine

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log

import com.nooblol.smartnoob.core.base.AndroidExecutor
import com.nooblol.smartnoob.core.base.extensions.buildSingleStroke
import com.nooblol.smartnoob.core.base.extensions.nextIntInOffset
import com.nooblol.smartnoob.core.base.extensions.nextLongInOffset
import com.nooblol.smartnoob.core.base.extensions.safeLineTo
import com.nooblol.smartnoob.core.base.extensions.safeMoveTo
import com.nooblol.smartnoob.core.base.workarounds.UnblockGestureScheduler
import com.nooblol.smartnoob.core.base.workarounds.buildUnblockGesture
import com.nooblol.smartnoob.core.dumb.domain.model.DumbAction
import com.nooblol.smartnoob.core.dumb.domain.model.Repeatable

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

internal class DumbActionExecutor(
    private val androidExecutor: AndroidExecutor,
    unblockWorkaroundEnabled: Boolean,
) {

    private val random: Random = Random(System.currentTimeMillis())
    private var randomize: Boolean = false

    private val unblockGestureScheduler: UnblockGestureScheduler? =
        if (unblockWorkaroundEnabled) UnblockGestureScheduler()
        else null


    suspend fun onScenarioLoopFinished() {
        if (unblockGestureScheduler?.shouldTrigger() == true) {
            withContext(Dispatchers.Main) {
                Log.i(TAG, "Injecting unblock gesture")
                androidExecutor.executeGesture(
                    GestureDescription.Builder().buildUnblockGesture()
                )
            }
        }
    }

    suspend fun executeDumbAction(action: DumbAction, randomize: Boolean) {
        this.randomize = randomize
        when (action) {
            is DumbAction.DumbClick -> executeDumbClick(action)
            is DumbAction.DumbSwipe -> executeDumbSwipe(action)
            is DumbAction.DumbPause -> executeDumbPause(action)
        }
    }

    private suspend fun executeDumbClick(dumbClick: DumbAction.DumbClick) {
        val clickGesture = GestureDescription.Builder().buildSingleStroke(
            path = Path().apply { moveTo(dumbClick.position.x, dumbClick.position.y) },
            durationMs = dumbClick.pressDurationMs.randomizeDurationIfNeeded(),
        )

        executeRepeatableGesture(clickGesture, dumbClick)
    }

    private suspend fun executeDumbSwipe(dumbSwipe: DumbAction.DumbSwipe) {
        val swipeGesture = GestureDescription.Builder().buildSingleStroke(
            path = Path().apply {
                moveTo(dumbSwipe.fromPosition.x, dumbSwipe.fromPosition.y)
                lineTo(dumbSwipe.toPosition.x, dumbSwipe.toPosition.y)
            },
            durationMs = dumbSwipe.swipeDurationMs.randomizeDurationIfNeeded(),
        )

        executeRepeatableGesture(swipeGesture, dumbSwipe)
    }

    private suspend fun executeDumbPause(dumbPause: DumbAction.DumbPause) {
        delay(dumbPause.pauseDurationMs.randomizeDurationIfNeeded())
    }

    private suspend fun executeRepeatableGesture(gesture: GestureDescription, repeatable: Repeatable) {
        repeatable.repeat {
            withContext(Dispatchers.Main) {
                androidExecutor.executeGesture(gesture)
            }
        }
    }
    private fun Path.moveTo(x: Int, y: Int) {
        if (!randomize) safeMoveTo(x, y)
        else safeMoveTo(
            random.nextIntInOffset(x, RANDOMIZATION_POSITION_MAX_OFFSET_PX),
            random.nextIntInOffset(y, RANDOMIZATION_POSITION_MAX_OFFSET_PX),
        )
    }

    private fun Path.lineTo(x: Int, y: Int) {
        if (!randomize) safeLineTo(x, y)
        else safeLineTo(
            random.nextIntInOffset(x, RANDOMIZATION_POSITION_MAX_OFFSET_PX),
            random.nextIntInOffset(y, RANDOMIZATION_POSITION_MAX_OFFSET_PX),
        )
    }

    private fun Long.randomizeDurationIfNeeded(): Long =
        if (randomize) random.nextLongInOffset(this, RANDOMIZATION_DURATION_MAX_OFFSET_MS)
        else this
}


private const val RANDOMIZATION_POSITION_MAX_OFFSET_PX = 5
private const val RANDOMIZATION_DURATION_MAX_OFFSET_MS = 5L

private const val TAG = "DumbActionExecutor"