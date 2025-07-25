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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.nooblol.smartnoob.core.common.overlays.base.viewModels
import com.nooblol.smartnoob.core.common.overlays.menu.OverlayMenu
import com.nooblol.smartnoob.core.ui.views.areaselector.AreaSelectorView
import com.nooblol.smartnoob.feature.smart.config.R
import com.nooblol.smartnoob.feature.smart.config.databinding.OverlayValidationMenuBinding
import com.nooblol.smartnoob.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint

import kotlinx.coroutines.launch

class ImageConditionAreaSelectorMenu(
    private val onAreaSelected: (Rect) -> Unit
) : OverlayMenu() {

    /** The view model for this dialog. */
    private val viewModel: ImageConditionAreaSelectorViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { imageConditionAreaSelectorViewModel() },
    )

    /** The view binding for the overlay menu. */
    private lateinit var viewBinding: OverlayValidationMenuBinding
    /** The view displaying selector for the area. */
    private lateinit var selectorView: AreaSelectorView

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        selectorView = AreaSelectorView(context, displayConfigManager)
        viewBinding = OverlayValidationMenuBinding.inflate(layoutInflater)
        return viewBinding.root
    }

    override fun onCreateOverlayView(): View = selectorView

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.initialArea.collect { selectorState ->
                    selectorView.setSelection(selectorState.initialArea, selectorState.minimalArea)
                }
            }
        }
    }

    override fun onMenuItemClicked(viewId: Int) {
        when (viewId) {
            R.id.btn_confirm -> onConfirm()
            R.id.btn_cancel -> onCancel()
        }
    }

    /** Called when the user press the confirmation button. */
    private fun onConfirm() {
        onAreaSelected(selectorView.getSelection())
        back()
    }

    /** Called when the user press the cancel button. */
    private fun onCancel() {
        back()
    }
}