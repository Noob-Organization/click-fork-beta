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
package com.nooblol.smartnoob.core.ui.bindings.lists

import android.view.View
import androidx.annotation.StringRes
import com.nooblol.smartnoob.core.ui.databinding.IncludeLoadableListBinding

fun IncludeLoadableListBinding.setEmptyText(@StringRes id: Int, @StringRes secondaryId: Int? = null) {
    emptyText.setText(id)

    if (secondaryId == null) {
        emptySecondary.visibility = View.GONE
    } else {
        emptySecondary.visibility = View.VISIBLE
        emptySecondaryText.setText(secondaryId)
    }
}

fun IncludeLoadableListBinding.updateState(items: Collection<Any>?) {
    when {
        items == null -> {
            loading.visibility = View.VISIBLE
            list.visibility = View.GONE
            empty.visibility = View.GONE
        }
        items.isEmpty() -> {
            loading.visibility = View.GONE
            list.visibility = View.GONE
            empty.visibility = View.VISIBLE
        }
        else -> {
            loading.visibility = View.GONE
            list.visibility = View.VISIBLE
            empty.visibility = View.GONE
        }
    }
}