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
package com.nooblol.smartnoob.feature.dumb.config.ui.actions.copy

import android.content.Context

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel

import com.nooblol.smartnoob.core.base.extensions.mapList
import com.nooblol.smartnoob.feature.dumb.config.R
import com.nooblol.smartnoob.feature.dumb.config.domain.DumbEditionRepository

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** View model for the [DumbActionCopyDialog]. */
class DumbActionCopyModel @Inject constructor(
    @ApplicationContext context: Context,
    dumbEditionRepository: DumbEditionRepository,
) : ViewModel() {

    /** The currently searched action name. Null if no is. */
    private val searchQuery = MutableStateFlow<String?>(null)

    /** List of all actions available for copy */
    private val allCopyItems: Flow<List<DumbActionCopyItem>> = dumbEditionRepository.actionsToCopy
        .mapList { dumbAction -> DumbActionCopyItem.DumbActionItem(dumbAction.toDumbActionDetails(context)) }
        .filterIdentical()
        .combine(dumbEditionRepository.editedDumbScenario) { actionsToCopy, scenario ->
            scenario ?: return@combine emptyList()

            val thisActions = mutableListOf<DumbActionCopyItem.DumbActionItem>()
            val otherActions = mutableListOf<DumbActionCopyItem.DumbActionItem>()
            actionsToCopy.forEach { item ->
                if (item.dumbActionDetails.action.scenarioId == scenario.id) thisActions.add(item)
                else otherActions.add(item)
            }

            buildList {
                if (thisActions.isNotEmpty()) {
                    add(DumbActionCopyItem.HeaderItem(R.string.list_header_copy_dumb_action_this))
                    addAll(thisActions.sortedBy { it.dumbActionDetails.name })
                }

                if (otherActions.isNotEmpty()) {
                    add(DumbActionCopyItem.HeaderItem(R.string.list_header_copy_dumb_action_all))
                    addAll(otherActions.sortedBy { it.dumbActionDetails.name })
                }
            }
        }

    /**
     * List of displayed action items.
     * This list can contains all events with headers, or the search result depending on the current search query.
     */
    val dumbActionList: Flow<List<DumbActionCopyItem>> =
        allCopyItems.combine(searchQuery) { allItems, query ->
            if (query.isNullOrEmpty()) allItems
            else allItems
                .filterIsInstance<DumbActionCopyItem.DumbActionItem>()
                .filter { item -> item.dumbActionDetails.name.contains(query, true) }
        }

    /**
     * Update the action search query.
     * @param query the new query.
     */
    fun updateSearchQuery(query: String?) {
        searchQuery.value = query
    }

    /** Remove all identical items from the list. */
    private fun Flow<List<DumbActionCopyItem.DumbActionItem>>.filterIdentical() =
        map { list ->
            list.distinctBy { item ->
                item.dumbActionDetails.name.hashCode() +
                    item.dumbActionDetails.detailsText.hashCode() +
                    item.dumbActionDetails.icon.hashCode() +
                    item.dumbActionDetails.haveError.hashCode() +
                    (item.dumbActionDetails.repeatCountText?.hashCode() ?: 0)
            }
        }
}

/** Types of items in the action copy list. */
sealed class DumbActionCopyItem {

    /**
     * Header item, delimiting sections.
     * @param title the title for the header.
     */
    data class HeaderItem(@StringRes val title: Int) : DumbActionCopyItem()

    /**
     * Action item.
     * @param dumbActionDetails the details for the action.
     */
    data class DumbActionItem(
        val dumbActionDetails: DumbActionDetails,
    ) : DumbActionCopyItem()
}