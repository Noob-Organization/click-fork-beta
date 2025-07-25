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
package com.nooblol.smartnoob.feature.smart.config.ui.common.bindings

import android.content.ComponentName
import com.nooblol.smartnoob.core.android.application.AndroidApplicationInfo

import com.nooblol.smartnoob.feature.smart.config.databinding.ItemApplicationBinding

/** Binds to the provided activity. */
fun ItemApplicationBinding.bind(activity: AndroidApplicationInfo, listener: ((ComponentName) -> Unit)? = null) {
    textApp.text = activity.name
    iconApp.setImageDrawable(activity.icon)

    listener?.let { root.setOnClickListener { it(activity.componentName) } }
}