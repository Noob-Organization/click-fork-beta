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
package com.nooblol.smartnoob.core.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.nooblol.smartnoob.core.base.migrations.SQLiteColumn
import com.nooblol.smartnoob.core.base.migrations.getSQLiteTableReference

/**
 * Migration from database v2 to v3.
 * Changes: clicks have now an optional amount of executions before the scenario is stopped.
 */
object Migration2to3 : Migration(2, 3) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.getSQLiteTableReference("click_table")
            .alterTableAddColumn(SQLiteColumn.Int("stop_after", isNotNull = false))
    }
}