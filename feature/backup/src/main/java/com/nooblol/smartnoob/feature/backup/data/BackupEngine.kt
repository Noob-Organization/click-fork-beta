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
package com.nooblol.smartnoob.feature.backup.data

import android.content.ContentResolver
import android.graphics.Point
import android.net.Uri
import android.util.Log

import com.nooblol.smartnoob.core.database.entity.CompleteScenario
import com.nooblol.smartnoob.core.dumb.data.database.DumbScenarioWithActions
import com.nooblol.smartnoob.feature.backup.data.dumb.DumbBackupDataSource
import com.nooblol.smartnoob.feature.backup.data.smart.SmartBackupDataSource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.io.File
import java.io.IOException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** [BackupEngine] internal implementation. */
internal class BackupEngine(appDataDir: File, private val contentResolver: ContentResolver) {

    private val dumbBackupDataSource: DumbBackupDataSource = DumbBackupDataSource(appDataDir)
    private val smartBackupDataSource: SmartBackupDataSource = SmartBackupDataSource(appDataDir)

    /**
     * Creates a new backup file.
     *
     * @param zipFileUri the uri of the file to write the backup into. Must be retrieved using the DocumentProvider.
     * @param smartScenarios the scenarios to backup.
     * @param screenSize the size of this device screen.
     * @param progress the object notified about the backup progress.
     */
    suspend fun createBackup(
        zipFileUri: Uri,
        smartScenarios: List<CompleteScenario>,
        dumbScenarios: List<DumbScenarioWithActions>,
        screenSize: Point,
        progress: BackupProgress,
    ) {
        Log.d(TAG, "Create backup: $zipFileUri for scenarios: $smartScenarios")
        dumbBackupDataSource.reset()
        smartBackupDataSource.reset()

        var currentProgress = 0
        progress.onProgressChanged(currentProgress, smartScenarios.size)

        // Create the zip file containing the scenarios and their events conditions.
        withContext(Dispatchers.IO) {
            try {
                ZipOutputStream(contentResolver.openOutputStream(zipFileUri)).use { zipStream ->
                    dumbScenarios.forEach { dumbScenario ->
                        Log.d(TAG, "Backup dumb scenario ${dumbScenario.scenario.id}")

                        dumbBackupDataSource.addScenarioToZipFile(zipStream, dumbScenario, screenSize)

                        currentProgress++
                        progress.onProgressChanged(currentProgress, smartScenarios.size)
                    }

                    smartScenarios.forEach { completeScenario ->
                        Log.d(TAG, "Backup smart scenario ${completeScenario.scenario.id}")

                        smartBackupDataSource.addScenarioToZipFile(zipStream, completeScenario, screenSize)

                        currentProgress++
                        progress.onProgressChanged(currentProgress, smartScenarios.size)
                    }

                    progress.onCompleted(dumbScenarios, smartScenarios, 0, false)
                }
            } catch (ioEx: IOException) {
                Log.e(TAG, "Error while creating backup archive.")
                progress.onError()
            } catch (isEx: IllegalStateException) {
                Log.e(TAG, "Error while creating backup archive, target folder can't be written")
                progress.onError()
            } catch (secEx: SecurityException) {
                Log.e(TAG, "Error while creating backup archive, permission is denied")
                progress.onError()
            }
        }
    }

    /**
     * Loads a backup file.
     *
     * @param zipFileUri the uri of the file to load the backup from. Must be retrieved using the DocumentProvider.
     * @param screenSize the size of this device screen.
     * @param progress the object notified about the backup import progress.
     */
    suspend fun loadBackup(zipFileUri: Uri, screenSize: Point, progress: BackupProgress) {
        Log.i(TAG, "Load backup: $zipFileUri")

        dumbBackupDataSource.reset()
        smartBackupDataSource.reset()

        var currentProgress = 0
        progress.onProgressChanged(currentProgress, null)

        withContext(Dispatchers.IO) {
            try {
                ZipInputStream(contentResolver.openInputStream(zipFileUri)).use { zipStream ->
                    generateSequence { zipStream.nextEntry }
                        .forEach { zipEntry ->
                            if (zipEntry.isDirectory) return@forEach

                            Log.d(TAG, "Extracting file ${zipEntry.name}")
                            when {
                                dumbBackupDataSource.extractFromZip(zipStream, zipEntry.name) -> {
                                    Log.d(TAG, "Dumb scenario file ${zipEntry.name} extracted.")

                                    currentProgress++
                                    progress.onProgressChanged(currentProgress, null)
                                }

                                smartBackupDataSource.extractFromZip(zipStream, zipEntry.name) -> {
                                    if (smartBackupDataSource.isScenarioBackupFileZipEntry(zipEntry.name)) {
                                        Log.d(TAG, "Smart scenario file ${zipEntry.name} extracted")

                                        currentProgress++
                                        progress.onProgressChanged(currentProgress, null)
                                    }
                                }

                                else -> Log.w(TAG, "Nothing found to handle zip entry ${zipEntry.name}")
                            }
                        }
                }

                progress.onVerification?.invoke()
                dumbBackupDataSource.verifyExtractedScenarios(screenSize)
                smartBackupDataSource.verifyExtractedScenarios(screenSize)

                Log.i(TAG, "Backup loading completed: $zipFileUri")
                Log.i(TAG, "Inserting extracted scenarios into database")

                progress.onCompleted(
                    dumbBackupDataSource.validBackups,
                    smartBackupDataSource.validBackups,
                    dumbBackupDataSource.failureCount + smartBackupDataSource.failureCount,
                    smartBackupDataSource.screenCompatWarning,
                )
            } catch (ioEx: IOException) {
                Log.e(TAG, "Error while loading backup archive", ioEx)
                progress.onError()
            } catch (secEx: SecurityException) {
                Log.e(TAG, "Error while loading backup archive, permission is denied", secEx)
                progress.onError()
            } catch (iaEx: IllegalArgumentException) {
                Log.e(TAG, "Error while loading backup archive, file is invalid", iaEx)
                progress.onError()
            } catch (npEx: NullPointerException) {
                Log.e(TAG, "Error while loading backup archive, file path is null", npEx)
                progress.onError()
            }
        }
    }
}

/** Tag for logs. */
private const val TAG = "BackupEngine"