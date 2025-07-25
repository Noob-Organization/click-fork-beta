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
package com.nooblol.smartnoob.scenarios.list

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup
import android.view.WindowManager

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.nooblol.smartnoob.R
import com.nooblol.smartnoob.core.base.extensions.applySafeContentInsets
import com.nooblol.smartnoob.databinding.FragmentScenariosBinding
import com.nooblol.smartnoob.feature.backup.ui.BackupDialogFragment
import com.nooblol.smartnoob.feature.backup.ui.BackupDialogFragment.Companion.FRAGMENT_TAG_BACKUP_DIALOG
import com.nooblol.smartnoob.scenarios.migration.ConditionsMigrationFragment
import com.nooblol.smartnoob.scenarios.creation.ScenarioCreationDialog
import com.nooblol.smartnoob.scenarios.list.adapter.ScenarioAdapter
import com.nooblol.smartnoob.scenarios.list.copy.ScenarioCopyDialog
import com.nooblol.smartnoob.scenarios.list.copy.ScenarioCopyDialog.Companion.FRAGMENT_TAG_COPY_DIALOG
import com.nooblol.smartnoob.scenarios.list.model.ScenarioListUiState
import com.nooblol.smartnoob.scenarios.migration.ConditionsMigrationFragment.Companion.FRAGMENT_RESULT_KEY_COMPLETED
import com.nooblol.smartnoob.scenarios.migration.ConditionsMigrationFragment.Companion.FRAGMENT_TAG_CONDITION_MIGRATION_DIALOG
import com.nooblol.smartnoob.settings.SettingsActivity

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.MaterialShapeDrawable
import dagger.hilt.android.AndroidEntryPoint

import kotlinx.coroutines.launch

/**
 * Fragment displaying the list of click scenario and the creation dialog.
 * If the list is empty, it will hide the list and displays the empty list view.
 */
@AndroidEntryPoint
class ScenarioListFragment : Fragment() {

    interface Listener {
        fun startScenario(item: ScenarioListUiState.Item.ScenarioItem)
    }

    /** ViewModel providing the scenarios data to the UI. */
    private val scenarioListViewModel: ScenarioListViewModel by viewModels()

    /** ViewBinding containing the views for this fragment. */
    private lateinit var viewBinding: FragmentScenariosBinding
    /** Adapter displaying the click scenarios as a list. */
    private lateinit var scenariosAdapter: ScenarioAdapter


    /** The current dialog being displayed. Null if not displayed. */
    private var dialog: AlertDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentScenariosBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scenariosAdapter = ScenarioAdapter(
            bitmapProvider = scenarioListViewModel::getConditionBitmap,
            startScenarioListener = ::onStartClicked,
            deleteScenarioListener = ::onDeleteClicked,
            exportClickListener = ::onExportClicked,
            copyClickedListener = ::showCopyScenarioDialog,
            expandCollapseListener = scenarioListViewModel::expandCollapseItem,
            onSortTypeClicked = scenarioListViewModel::updateSortType,
            onSmartChipClicked = scenarioListViewModel::updateSmartVisible,
            onDumbChipClicked = scenarioListViewModel::updateDumbVisible,
            onSortOrderClicked = scenarioListViewModel::updateSortOrder,
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.apply {
            list.adapter = scenariosAdapter

            emptyCreateButton.setOnClickListener { onCreateClicked() }
            add.setOnClickListener { onCreateClicked() }

            appBarLayout.statusBarForeground = MaterialShapeDrawable.createWithElevationOverlay(context)

            topAppBar.setOnMenuItemClickListener { onMenuItemSelected(it) }

            val fabHorizontalMarginInset = resources.getDimensionPixelSize(R.dimen.margin_horizontal_mini)
            val fabHorizontalMargin = resources.getDimensionPixelSize(R.dimen.margin_horizontal_large)
            val fabBottomMarginInset = resources.getDimensionPixelSize(R.dimen.margin_vertical_default)
            val fabBottomMargin = resources.getDimensionPixelSize(R.dimen.margin_vertical_extra_large)
            add.applySafeContentInsets(
                marginsIfInset = Rect(fabHorizontalMarginInset, 0, fabHorizontalMarginInset, fabBottomMarginInset),
                marginIfNot =  Rect(fabHorizontalMargin, 0, fabHorizontalMargin, fabBottomMargin),
            )
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { scenarioListViewModel.uiState.collect(::updateUiState) }
                launch { scenarioListViewModel.needsConditionMigration.collect(::onConditionMigrationRequired) }
            }
        }
    }

    private fun onMenuItemSelected(item: MenuItem): Boolean {
        val uiState = scenarioListViewModel.uiState.value ?: return false

        when (item.itemId) {
            R.id.action_export -> when {
                uiState.type == ScenarioListUiState.Type.EXPORT -> showBackupDialog(
                    isImport = false,
                    smartScenariosToBackup = scenarioListViewModel.getSmartScenariosSelectedForBackup(),
                    dumbScenariosToBackup = scenarioListViewModel.getDumbScenariosSelectedForBackup(),
                )
                else -> scenarioListViewModel.setUiState(ScenarioListUiState.Type.EXPORT)
            }

            R.id.action_import -> showBackupDialog(true)
            R.id.action_cancel -> scenarioListViewModel.setUiState(ScenarioListUiState.Type.SELECTION)
            R.id.action_search -> scenarioListViewModel.setUiState(ScenarioListUiState.Type.SEARCH)
            R.id.action_select_all -> scenarioListViewModel.toggleAllScenarioSelectionForBackup()
            R.id.action_settings -> startSettingsActivity()
            else -> return false
        }

        return true
    }

    private fun updateUiState(uiState: ScenarioListUiState?) {
        uiState ?: return

        updateMenu(uiState.menuUiState)
        updateScenarioList(uiState)
    }

    /**
     * Update the display of the action menu.
     * @param menuState the new ui state for the menu.
     */
    private fun updateMenu(menuState: ScenarioListUiState.Menu) {
        viewBinding.topAppBar.menu.apply {
            findItem(R.id.action_select_all)?.bind(menuState.selectAllItemState)
            findItem(R.id.action_cancel)?.bind(menuState.cancelItemState)
            findItem(R.id.action_import)?.bind(menuState.importItemState)
            findItem(R.id.action_export)?.bind(menuState.exportItemState)
            findItem(R.id.action_search)?.apply {
                bind(menuState.searchItemState)
                actionView?.let { actionView ->
                    (actionView as SearchView).apply {
                        setIconifiedByDefault(true)
                        setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                            override fun onQueryTextSubmit(query: String?) = false
                            override fun onQueryTextChange(newText: String?): Boolean {
                                scenarioListViewModel.updateSearchQuery(newText)
                                return true
                            }
                        })
                        addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
                            override fun onViewDetachedFromWindow(arg0: View) {
                                scenarioListViewModel.updateSearchQuery(null)
                                scenarioListViewModel.setUiState(ScenarioListUiState.Type.SELECTION)
                            }

                            override fun onViewAttachedToWindow(arg0: View) {
                                scenarioListViewModel.updateSearchQuery("")
                            }
                        })
                    }
                }
            }
            findItem(R.id.action_settings)?.bind(menuState.settingsItemState)
        }
    }

    /**
     * Observer upon the list of click scenarios.
     * Will update the list/empty view according to the current click scenarios
     */
    private fun updateScenarioList(uiState: ScenarioListUiState) {
        viewBinding.apply {
            loading.visibility = View.GONE
            if (uiState.listContent.isEmpty() && uiState.type == ScenarioListUiState.Type.SELECTION) {
                list.visibility = View.GONE
                add.visibility = View.GONE
                layoutEmpty.visibility = View.VISIBLE
            } else {
                list.visibility = View.VISIBLE
                add.visibility =
                    if (uiState.type == ScenarioListUiState.Type.SELECTION) View.VISIBLE
                    else View.GONE
                layoutEmpty.visibility = View.GONE
            }
        }

        scenariosAdapter.submitList(uiState.listContent)
    }

    private fun onConditionMigrationRequired(isRequired: Boolean) {
        if (!isRequired) return
        if (requireActivity().supportFragmentManager.findFragmentByTag(FRAGMENT_TAG_CONDITION_MIGRATION_DIALOG) != null)
            return

        val fragmentManager = requireActivity().supportFragmentManager
        fragmentManager.setFragmentResultListener(FRAGMENT_RESULT_KEY_COMPLETED, this) { _, _ ->
            scenarioListViewModel.refreshScenarioList()
        }
        ConditionsMigrationFragment
            .newInstance()
            .show(fragmentManager, FRAGMENT_TAG_CONDITION_MIGRATION_DIALOG)
    }

    /**
     * Show an AlertDialog from this fragment.
     * This method will ensure that only one dialog is shown at the same time.
     *
     * @param newDialog the new dialog to be shown.
     */
    private fun showDialog(newDialog: AlertDialog) {
        dialog.let {
            Log.w(TAG, "Requesting show dialog while another one is one screen.")
            it?.dismiss()
        }

        dialog = newDialog
        newDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        newDialog.setOnDismissListener { dialog = null }
        newDialog.show()
    }

    /**
     * Called when the user clicks on a scenario.
     * @param scenario the scenario clicked.
     */
    private fun onStartClicked(scenario: ScenarioListUiState.Item.ScenarioItem) {
        (requireActivity() as? Listener)?.startScenario(scenario)
    }

    /**
     * Called when the user clicks on the export button of a scenario.
     *
     * @param item the scenario clicked.
     */
    private fun onExportClicked(item: ScenarioListUiState.Item) {
        scenarioListViewModel.toggleScenarioSelectionForBackup(item)
    }

    /**
     * Called when the user clicks on the add scenario button.
     * Create and show the [dialog]. Upon Ok press, creates the scenario.
     */
    private fun onCreateClicked() {
        ScenarioCreationDialog()
            .show(requireActivity().supportFragmentManager, ScenarioCreationDialog.FRAGMENT_TAG)
    }

    /**
     * Called when the user clicks on the delete button of a scenario.
     * Create and show the [dialog]. Upon Ok press, delete the scenario.
     *
     * @param item the scenario to delete.
     */
    private fun onDeleteClicked(item: ScenarioListUiState.Item.ScenarioItem) {
        showDialog(MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_title_delete_scenario)
            .setMessage(resources.getString(R.string.message_delete_scenario, item.displayName))
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                scenarioListViewModel.deleteScenario(item)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create())
    }

    /**
     * Shows the backup dialog fragment.
     *
     * @param isImport true to display in import mode, false for export.
     * @param smartScenariosToBackup the list of identifiers for the smart scenarios to export. Null if isImport = true.
     * @param dumbScenariosToBackup the list of identifiers for the dumb scenarios to export. Null if isImport = true.
     *
     */
    private fun showBackupDialog(
        isImport: Boolean,
        smartScenariosToBackup: Collection<Long>? = null,
        dumbScenariosToBackup: Collection<Long>? = null,
    ) {
        BackupDialogFragment
            .newInstance(isImport, smartScenariosToBackup, dumbScenariosToBackup)
            .show(requireActivity().supportFragmentManager, FRAGMENT_TAG_BACKUP_DIALOG)
        scenarioListViewModel.setUiState(ScenarioListUiState.Type.SELECTION)
    }

    private fun showCopyScenarioDialog(scenarioItem: ScenarioListUiState.Item.ScenarioItem.Valid) {
        ScenarioCopyDialog
            .newInstance(
                scenarioId = scenarioItem.getScenarioId(),
                isSmart = scenarioItem is ScenarioListUiState.Item.ScenarioItem.Valid.Smart,
                defaultName = scenarioItem.displayName,
            )
            .show(requireActivity().supportFragmentManager, FRAGMENT_TAG_COPY_DIALOG)
    }

    private fun startSettingsActivity() {
        requireContext().startActivity(Intent(context, SettingsActivity::class.java))
    }
}

private fun MenuItem.bind(state: ScenarioListUiState.Menu.Item) {
    isVisible = state.visible
    isEnabled = state.enabled
    icon = icon?.mutate()?.apply {
        alpha = state.iconAlpha
    }
}

/** Tag for logs. */
private const val TAG = "ScenarioListFragment"