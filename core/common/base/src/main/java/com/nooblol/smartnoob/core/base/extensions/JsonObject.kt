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
package com.nooblol.smartnoob.core.base.extensions

import android.graphics.Rect
import android.util.Log

import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.long

// This file contains a set of safe getter for the JsonObject class.

/**
 * Safely get the [JsonObject] of this JsonElement.
 * @param shouldLogError true if any error should be logged, false if not.
 * @return the JsonObject value or null if not found.
 */
fun JsonElement.getJsonObject(shouldLogError: Boolean = false): JsonObject? =
    try {
        jsonObject
    } catch (iaEx: IllegalArgumentException) {
        if (shouldLogError) Log.w(TAG, "No JsonObject for this JsonElement")
        null
    }

/**
 * Safely get the [JsonObject] child value
 * @param key the key for the expected value
 * @param shouldLogError true if any error should be logged, false if not.
 * @return the child value for the given key, or null if not found.
 */
fun JsonObject.getJsonObject(key: String, shouldLogError: Boolean = false): JsonObject? =
    getValue(key, shouldLogError)?.let {
        try {
            it.jsonObject
        } catch (iaEx: IllegalArgumentException) {
            if (shouldLogError) Log.w(TAG, "Value for $key is not a JsonObject")
            null
        }
    }

/**
 * Safely get the [JsonArray] child value
 * @param key the key for the expected value
 * @param shouldLogError true if any error should be logged, false if not.
 * @return the child value for the given key, or null if not found.
 */
fun JsonObject.getJsonArray(key: String, shouldLogError: Boolean = false): JsonArray? =
    getValue(key, shouldLogError)?.let {
        try {
            it.jsonArray
        } catch (iaEx: IllegalArgumentException) {
            if (shouldLogError) Log.w(TAG, "Value for $key is not a JsonArray")
            null
        }
    }

/**
 * Safely get the boolean child value
 * @param key the key for the expected value
 * @param shouldLogError true if any error should be logged, false if not.
 * @return the child value for the given key, or null if not found.
 */
fun JsonObject.getBoolean(key: String, shouldLogError: Boolean = false): Boolean? =
    getValue(key, shouldLogError)?.let {
        try {
            it.jsonPrimitive.boolean
        } catch (iaEx: IllegalArgumentException) {
            if (shouldLogError) Log.w(TAG, "Value for $key is not a primitive")
            null
        } catch (isEx: IllegalStateException) {
            if (shouldLogError) Log.w(TAG, "Value for $key is not a boolean")
            null
        }
    }

/**
 * Safely get the integer child value
 * @param key the key for the expected value
 * @param shouldLogError true if any error should be logged, false if not.
 * @return the child value for the given key, or null if not found.
 */
fun JsonObject.getInt(key: String, shouldLogError: Boolean = false): Int? =
    getValue(key, shouldLogError)?.let {
        try {
            it.jsonPrimitive.int
        } catch (iaEx: IllegalArgumentException) {
            if (shouldLogError) Log.w(TAG, "Value for $key is not a primitive")
            null
        } catch (isEx: NumberFormatException) {
            if (shouldLogError) Log.w(TAG, "Value for $key is not a int")
            null
        }
    }

/**
 * Safely get the long child value
 * @param key the key for the expected value
 * @param shouldLogError true if any error should be logged, false if not.
 * @return the child value for the given key, or null if not found.
 */
fun JsonObject.getLong(key: String, shouldLogError: Boolean = false): Long? =
    getValue(key, shouldLogError)?.let {
        try {
            it.jsonPrimitive.long
        } catch (iaEx: IllegalArgumentException) {
            if (shouldLogError) Log.w(TAG, "Value for $key is not a primitive")
            null
        } catch (nfEx: NumberFormatException) {
            if (shouldLogError) Log.w(TAG, "Value for $key is not a long")
            null
        }
    }

/**
 * Safely get the string child value
 * @param key the key for the expected value
 * @param shouldLogError true if any error should be logged, false if not.
 * @return the child value for the given key, or null if not found.
 */
fun JsonObject.getString(key: String, shouldLogError: Boolean = false): String? =
    getValue(key, shouldLogError)?.let {
        try {
            if (it.jsonPrimitive.isString) it.jsonPrimitive.content else null
        } catch (iaEx: IllegalArgumentException) {
            if (shouldLogError) Log.w(TAG, "Value for $key is not a primitive")
            null
        }
    }

/**
 * Safely get the enum child value
 * @param T the enum type.
 * @param key the key for the expected value
 * @param shouldLogError true if any error should be logged, false if not.
 * @return the child value for the given key, or null if not found.
 */
inline fun <reified T : Enum<T>> JsonObject.getEnum(key: String, shouldLogError: Boolean = false): T? =
    getString(key, shouldLogError)?.let { stringEnum ->
        try {
            enumValueOf<T>(stringEnum)
        } catch (iae: IllegalArgumentException) {
            if (shouldLogError) Log.w("JsonObject", "Can't create IntentExtraType, value $stringEnum is invalid")
            null
        }
    }

fun JsonObject.getRect(keyLeft: String, keyTop: String, keyRight: String, keyBottom: String, shouldLogError: Boolean = false): Rect? {
    return Rect(
        getInt(keyLeft, shouldLogError) ?: return null,
        getInt(keyTop, shouldLogError) ?: return null,
        getInt(keyRight, shouldLogError) ?: return null,
        getInt(keyBottom, shouldLogError) ?: return null,
    )
}

inline fun <T : Any> JsonArray.getListOf(transform: (JsonObject) -> T?): List<T> =
    mapNotNull { transform(it.jsonObject) }

/**
 * Safely get the child value
 * @param key the key for the expected value
 * @param shouldLogError true if any error should be logged, false if not.
 * @return the child value for the given key, or null if not found.
 */
private fun JsonObject.getValue(key: String, shouldLogError: Boolean = false) = get(key) ?:let {
    if (shouldLogError) Log.w(TAG, "Can't find $key")
    null
}


/** Tag for logs. */
private const val TAG = "JsonObject"