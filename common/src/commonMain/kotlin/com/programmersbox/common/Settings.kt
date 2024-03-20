package com.programmersbox.common

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import okio.Path.Companion.toPath

class Settings(
    producePath: () -> String,
) {
    private val dataStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })

    companion object {
        const val dataStoreFileName = "yahtzee.preferences_pb"
    }

    val showDotsOnDice by lazy {
        DataStoreTypeDefaultNonNull(
            booleanPreferencesKey("showDiceOnDots"),
            true
        )
    }

    open inner class DataStoreType<T>(
        protected val key: Preferences.Key<T>,
    ) {
        open val flow: Flow<T?> = dataStore.data
            .map { it[key] }
            .distinctUntilChanged()

        open suspend fun update(value: T) {
            dataStore.edit { it[key] = value }
        }
    }

    open inner class DataStoreTypeNonNull<T>(
        key: Preferences.Key<T>,
    ) : DataStoreType<T>(key) {
        override val flow: Flow<T> = dataStore.data
            .mapNotNull { it[key] }
            .distinctUntilChanged()
    }

    inner class DataStoreTypeDefaultNonNull<T>(
        key: Preferences.Key<T>,
        defaultValue: T,
    ) : DataStoreTypeNonNull<T>(key) {
        override val flow: Flow<T> = dataStore.data
            .mapNotNull { it[key] ?: defaultValue }
            .distinctUntilChanged()
    }
}