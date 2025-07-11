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
package com.nooblol.smartnoob.feature.revenue.ui.purchase

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import android.view.View

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.nooblol.smartnoob.core.base.extensions.safeStartWebBrowserActivity
import com.nooblol.smartnoob.core.ui.bindings.buttons.LoadableButtonState
import com.nooblol.smartnoob.core.ui.bindings.buttons.setOnClickListener
import com.nooblol.smartnoob.core.ui.bindings.buttons.setState
import com.nooblol.smartnoob.feature.revenue.R
import com.nooblol.smartnoob.feature.revenue.databinding.FragmentPurchaseProModeBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior

import com.google.android.material.bottomsheet.BottomSheetDialog

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class PurchaseProModeFragment : DialogFragment() {

    companion object {
        /** Tag for ads loading dialog fragment. */
        const val FRAGMENT_TAG = "PurchaseProModeFragment"
    }

    private val viewModel: PurchaseProModeViewModel by viewModels()
    /** The view binding on the views of this dialog. */
    private lateinit var viewBinding: FragmentPurchaseProModeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.dialogState.collect(::updateDialogState) }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewBinding = FragmentPurchaseProModeBinding.inflate(layoutInflater).apply {
            buttonSource.setOnClickListener {
                context?.safeStartWebBrowserActivity("https://github.com/Nain57/Smart-AutoClicker")
            }

            buttonBuy.setOnClickListener {
                viewModel.launchPlayStoreBillingFlow(requireActivity())
            }
        }

        return BottomSheetDialog(requireContext()).apply {
            setContentView(viewBinding.root)
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    this@PurchaseProModeFragment.dismiss()
                    true
                } else {
                    false
                }
            }

            create()
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        activity?.finish()
    }

    private fun updateDialogState(state: PurchaseDialogState): Unit = when (state) {
        PurchaseDialogState.Loading -> toLoadingState()
        is PurchaseDialogState.Loaded -> toLoadedState(state.price)
        PurchaseDialogState.Purchased -> toPurchasedState()
        PurchaseDialogState.Error -> toErrorState()
        PurchaseDialogState.Pending -> toPendingState()
    }

    private fun toLoadingState() {
        viewBinding.apply {
            purchaseText.visibility = View.VISIBLE
            purchasedText.visibility = View.GONE
            buttonBuy.setState(LoadableButtonState.Loading())
        }
    }

    private fun toLoadedState(price: String) {
        viewBinding.apply {
            purchaseText.visibility = View.VISIBLE
            purchasedText.visibility = View.GONE
            buttonBuy.setState(
                LoadableButtonState.Loaded.Enabled(requireContext().getString(R.string.button_text_buy_pro, price))
            )
        }
    }

    private fun toPurchasedState() {
        viewBinding.apply {
            purchaseText.visibility = View.INVISIBLE
            purchasedText.visibility = View.VISIBLE
            buttonBuy.setState(
                LoadableButtonState.Loaded.Enabled(requireContext().getString(R.string.button_text_understood))
            )
        }
    }

    private fun toPendingState() {
        viewBinding.apply {
            purchaseText.visibility = View.VISIBLE
            purchasedText.visibility = View.GONE
            buttonBuy.setState(
                LoadableButtonState.Loaded.Disabled(requireContext().getString(R.string.button_text_buy_pro_pending))
            )
        }
    }

    private fun toErrorState() {
        viewBinding.apply {
            purchaseText.visibility = View.VISIBLE
            purchasedText.visibility = View.GONE
            buttonBuy.setState(
                LoadableButtonState.Loaded.Disabled(requireContext().getString(R.string.button_text_buy_pro_error))
            )
        }
    }
}