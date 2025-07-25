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
package com.nooblol.smartnoob.feature.review

import android.content.Context
import android.content.Intent

import com.nooblol.smartnoob.core.base.Dumpable
import com.nooblol.smartnoob.core.base.addDumpTabulationLvl

import java.io.PrintWriter


interface ReviewRepository : Dumpable {

    fun isUserCandidateForReview(): Boolean
    fun getReviewActivityIntent(context: Context): Intent?

    fun onUserSessionStarted()
    fun onUserSessionStopped()

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        val contentPrefix = prefix.addDumpTabulationLvl()

        writer.apply {
            append(prefix).println("* ReviewRepository:")
            append(contentPrefix)
                .append("- isUserCandidateForReview=${isUserCandidateForReview()}; ")
                .println()
        }
    }
}