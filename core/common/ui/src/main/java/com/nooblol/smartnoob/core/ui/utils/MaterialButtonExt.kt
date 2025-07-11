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
package com.nooblol.smartnoob.core.ui.utils

import androidx.annotation.ColorInt
import com.nooblol.smartnoob.core.ui.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable


fun MaterialButton.showProgress(@ColorInt tintColor: Int = iconTint.defaultColor) {
    icon = IndeterminateDrawable.createCircularDrawable(
        context,
        CircularProgressIndicatorSpec(
            context,
            null,
            0,
            R.style.Widget_Material3_CircularProgressIndicator_ExtraSmall
        ).apply { indicatorColors = intArrayOf(tintColor) },
    )
}

fun MaterialButton.hideProgress() {
    icon = null
}