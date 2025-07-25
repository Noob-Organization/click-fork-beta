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
package com.nooblol.smartnoob.core.dumb.domain

import com.nooblol.smartnoob.core.base.identifier.Identifier
import com.nooblol.smartnoob.core.dumb.data.DumbScenarioDataSource
import com.nooblol.smartnoob.core.dumb.data.database.DumbScenarioWithActions
import com.nooblol.smartnoob.core.dumb.domain.model.DumbAction
import com.nooblol.smartnoob.core.dumb.domain.model.DumbScenario

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DumbRepository @Inject constructor(
    private val dumbScenarioDataSource: DumbScenarioDataSource,
) : IDumbRepository {

    override val dumbScenarios: Flow<List<DumbScenario>> =
        dumbScenarioDataSource.getAllDumbScenarios

    override suspend fun getDumbScenario(dbId: Long): DumbScenario? =
        dumbScenarioDataSource.getDumbScenario(dbId)

    override fun getDumbScenarioFlow(dbId: Long): Flow<DumbScenario?> =
        dumbScenarioDataSource.getDumbScenarioFlow(dbId)

    override fun getAllDumbActionsFlowExcept(scenarioDbId: Long): Flow<List<DumbAction>> =
        dumbScenarioDataSource.getAllDumbActionsExcept(scenarioDbId)

    override suspend fun addDumbScenario(scenario: DumbScenario) {
        dumbScenarioDataSource.addDumbScenario(scenario)
    }

    override suspend fun addDumbScenarioCopy(scenario: DumbScenarioWithActions): Long? =
        dumbScenarioDataSource.addDumbScenarioCopy(scenario)

    override suspend fun addDumbScenarioCopy(scenarioId: Long, copyName: String): Long? =
        dumbScenarioDataSource.addDumbScenarioCopy(scenarioId, copyName)

    override suspend fun updateDumbScenario(scenario: DumbScenario) {
        dumbScenarioDataSource.updateDumbScenario(scenario)
    }

    override suspend fun deleteDumbScenario(scenario: DumbScenario) {
        dumbScenarioDataSource.deleteDumbScenario(scenario)
    }

    override suspend fun markAsUsed(scenarioId: Identifier) {
        dumbScenarioDataSource.markAsUsed(scenarioId.databaseId)
    }
}