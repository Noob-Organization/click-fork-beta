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
package com.nooblol.smartnoob.feature.tutorial.domain

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import android.util.Log

import com.nooblol.smartnoob.core.base.di.Dispatcher
import com.nooblol.smartnoob.core.base.di.HiltCoroutineDispatchers.Main
import com.nooblol.smartnoob.core.domain.IRepository
import com.nooblol.smartnoob.core.base.identifier.Identifier
import com.nooblol.smartnoob.core.processing.domain.DetectionRepository
import com.nooblol.smartnoob.feature.tutorial.data.TutorialDataSource
import com.nooblol.smartnoob.feature.tutorial.data.TutorialEngine
import com.nooblol.smartnoob.feature.tutorial.data.TutorialStateDataSource
import com.nooblol.smartnoob.feature.tutorial.data.getTutorialPreferences
import com.nooblol.smartnoob.feature.tutorial.data.isStopVolumeDownDontShowAgain
import com.nooblol.smartnoob.feature.tutorial.data.putStopVolumeDownDontShowAgain
import com.nooblol.smartnoob.feature.tutorial.domain.model.Tutorial
import com.nooblol.smartnoob.feature.tutorial.domain.model.TutorialStep
import com.nooblol.smartnoob.feature.tutorial.domain.model.game.TutorialGame
import com.nooblol.smartnoob.feature.tutorial.domain.model.game.TutorialGameTargetType
import com.nooblol.smartnoob.feature.tutorial.domain.model.game.toDomain
import com.nooblol.smartnoob.feature.tutorial.domain.model.toDomain

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class TutorialRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val scenarioRepository: IRepository,
    private val detectionRepository: DetectionRepository,
    private val dataSource: TutorialDataSource,
    private val stateDataSource: TutorialStateDataSource,
    private val tutorialEngine: TutorialEngine,
    @Dispatcher(Main) private val dispatcherMain: CoroutineDispatcher,
) {

    private val coroutineScopeMain: CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatcherMain)

    private val sharedPrefs: SharedPreferences = context.getTutorialPreferences()

    /**
     * The identifier of the user scenario when he enters the tutorial mode.
     * Kept to be restored once he quit the tutorial.
     */
    private var scenarioId: Identifier? = null
    /** The identifier for the scenario used during a tutorial. */
    private var tutorialScenarioId: Identifier? = null
    /** The coroutine job executing the stop tutorial logic. Null if not stopping. */
    private var stopTutorialJob: Job? = null

    private val activeTutorialIndex: MutableStateFlow<Int?> = MutableStateFlow(null)

    val tutorials: Flow<List<Tutorial>> = stateDataSource.tutorialSuccessList
        .map { successList ->
            dataSource.tutorialsInfo.mapIndexedNotNull { index, tutorialData ->
                if (index > 2) return@mapIndexedNotNull null
                tutorialData.toDomain(index == 0 || index <= successList.size)
            }
        }

    val activeStep: Flow<TutorialStep?> = tutorialEngine.currentStep
        .map { step ->
            Log.d(TAG, "Update overlay state for step $step")
            step?.toDomain()
        }

    val activeGame: Flow<TutorialGame?> = tutorialEngine.tutorial
        .map { tutorial -> tutorial?.game?.toDomain() }

    private val isGameWon: StateFlow<Boolean> = activeGame
        .flatMapLatest { it?.state ?: flowOf(null) }
        .map { it?.isWon == true }
        .stateIn(coroutineScopeMain, SharingStarted.WhileSubscribed(3_000), false)

    init {
        isGameWon
            .onEach { isWon -> if (isWon) setTutorialSuccess() }
            .launchIn(coroutineScopeMain)
    }

    fun shouldShowStopWithVolumeDownTutorialDialog(): Boolean =
        !sharedPrefs.isStopVolumeDownDontShowAgain()

    fun setStopWithVolumeDownDontShowAgain() =
        sharedPrefs.edit().putStopVolumeDownDontShowAgain(true).apply()

    fun setupTutorialMode() {
        if (scenarioId != null) return

        scenarioId = detectionRepository.getScenarioId()

        Log.d(TAG, "Setup tutorial mode, user scenario is $scenarioId")
        scenarioRepository.startTutorialMode()
    }

    fun startTutorial(index: Int) {
        if (tutorialEngine.isStarted()) return
        if (scenarioId == null) {
            Log.e(TAG, "Tutorial mode is not setup, can't start tutorial $index")
            return
        }
        if (index < 0 || index >= dataSource.tutorialsInfo.size) {
            Log.e(TAG, "Can't start tutorial, index is invalid $index")
            return
        }

        val tutorialData = dataSource.getTutorialData(index) ?: return
        coroutineScopeMain.launch {
            val tutoScenarioDbId = withContext(Dispatchers.IO) {
                stateDataSource.initTutorialScenario(index)
            } ?: return@launch
            val tutoScenarioId = Identifier(databaseId = tutoScenarioDbId)

            Log.d(TAG, "Start tutorial $index, set current scenario to $tutoScenarioDbId")

            activeTutorialIndex.value = index
            tutorialScenarioId = tutoScenarioId
            detectionRepository.setScenarioId(tutoScenarioId)

            tutorialEngine.startTutorial(tutorialData)
        }
    }

    fun stopTutorial() {
        if (!tutorialEngine.isStarted()) return
        val tutorialIndex = activeTutorialIndex.value ?: return

        stopTutorialJob = coroutineScopeMain.launch {
            Log.d(TAG, "Stop tutorial $tutorialIndex")

            tutorialEngine.stopTutorial()
            detectionRepository.stopDetection()
            cleanupTutorialState()

            activeTutorialIndex.value = null
        }
    }

    fun stopTutorialMode() {
        scenarioId ?: return

        Log.d(TAG, "Stop tutorial mode, restoring user scenario $scenarioId")

        stopTutorial()
        coroutineScopeMain.launch {
            stopTutorialJob?.join()
            stopTutorialJob = null
            scenarioId?.let { detectionRepository.setScenarioId(it) }
            scenarioId = null
            activeTutorialIndex.value = null
            scenarioRepository.stopTutorialMode()
        }
    }

    fun nextTutorialStep() {
        tutorialEngine.nextStep()
    }

    fun lastTutorialStep() {
        tutorialEngine.lastStep()
    }

    fun startGame(area: Rect, targetSize: Int) {
        tutorialEngine.startGame(area, targetSize)
    }

    fun onGameTargetHit(targetType: TutorialGameTargetType) {
        tutorialEngine.onGameTargetHit(targetType)
    }

    private suspend fun setTutorialSuccess() {
        val tutorialIndex = activeTutorialIndex.value ?: return
        val tutoScenarioIdentifier = tutorialScenarioId ?: return

        stateDataSource.setTutorialSuccess(tutorialIndex, tutoScenarioIdentifier)
    }

    private suspend fun cleanupTutorialState() {
        val tutorialIndex = activeTutorialIndex.value ?: return
        val tutoScenarioIdentifier = tutorialScenarioId ?: return

        stateDataSource.cleanupTutorialState(tutorialIndex, tutoScenarioIdentifier)
    }
}

private const val TAG = "TutorialRepository"