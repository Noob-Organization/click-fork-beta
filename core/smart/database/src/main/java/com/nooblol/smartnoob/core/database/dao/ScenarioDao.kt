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
package com.nooblol.smartnoob.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nooblol.smartnoob.core.database.SCENARIO_USAGE_TABLE

import com.nooblol.smartnoob.core.database.entity.CompleteScenario
import com.nooblol.smartnoob.core.database.entity.ScenarioEntity
import com.nooblol.smartnoob.core.database.entity.ScenarioStatsEntity
import com.nooblol.smartnoob.core.database.entity.ScenarioWithEvents

import kotlinx.coroutines.flow.Flow

/** Allows to access and edit the scenario in the database. */
@Dao
interface ScenarioDao {

    /**
     * Get all events scenario and their events.
     *
     * @return the live data on the list of scenarios.
     */
    @Transaction
    @Query("SELECT * FROM scenario_table ORDER BY name ASC")
    fun getScenariosWithEvents(): Flow<List<ScenarioWithEvents>>

    /**
     * Get a scenario
     *
     * @return the scenario.
     */
    @Transaction
    @Query("SELECT * FROM scenario_table WHERE id=:scenarioId ORDER BY name ASC")
    suspend fun getScenario(scenarioId: Long): ScenarioWithEvents?

    /**
     * Get a scenario
     *
     * @return the scenario.
     */
    @Transaction
    @Query("SELECT * FROM scenario_table WHERE id=:scenarioId ORDER BY name ASC")
    fun getScenarioFlow(scenarioId: Long): Flow<ScenarioWithEvents?>

    /**
     * Get a complete scenario
     *
     * @return the scenario.
     */
    @Transaction
    @Query("SELECT * FROM scenario_table WHERE id=:scenarioId")
    suspend fun getCompleteScenario(scenarioId: Long): CompleteScenario?

    /**
     * Add a new scenario to the database.
     *
     * @param scenarioEntity the scenario to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(scenarioEntity: ScenarioEntity): Long

    /**
     * Update the selected scenario.
     *
     * @param scenarioEntity the scenario to be updated.
     */
    @Update
    suspend fun update(scenarioEntity: ScenarioEntity)

    /**
     * Delete the provided click scenario from the database.
     *
     * Any associated entity will be removed from the database due to the [androidx.room.ForeignKey.CASCADE]
     * deletion of this parent scenario.
     *
     * @param scenarioId the identifier of the scenario to be deleted.
     */
    @Query("DELETE FROM scenario_table WHERE id = :scenarioId")
    suspend fun delete(scenarioId: Long)

    /**
     * Get a scenario stats
     *
     * @return the scenario stats.
     */
    @Query("SELECT * FROM $SCENARIO_USAGE_TABLE WHERE id=:scenarioId")
    suspend fun getScenarioStats(scenarioId: Long): ScenarioStatsEntity?

    /**
     * Add the stats for a scenario.
     *
     * @param stats the stats to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addScenarioStats(stats: ScenarioStatsEntity)

    /**
     * Update the stats for a scenario.
     *
     * @param stats the stats to be updated.
     */
    @Update
    suspend fun updateScenarioStats(stats: ScenarioStatsEntity)
}