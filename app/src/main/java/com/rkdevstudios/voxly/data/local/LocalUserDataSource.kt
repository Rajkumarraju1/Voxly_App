package com.rkdevstudios.voxly.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rkdevstudios.voxly.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalUserDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")
        val KEY_AVATAR_URL = stringPreferencesKey("avatar_url")
        val KEY_IS_SPEAKER = booleanPreferencesKey("is_speaker")
        val KEY_COINS = doublePreferencesKey("coins")
    }

    val userData: Flow<User?> = dataStore.data.map { preferences ->
        val userId = preferences[KEY_USER_ID]
        if (userId != null) {
            User(
                id = userId,
                displayName = preferences[KEY_DISPLAY_NAME] ?: "",
                avatarUrl = preferences[KEY_AVATAR_URL] ?: "",
                isSpeaker = preferences[KEY_IS_SPEAKER] ?: false,
                coins = preferences[KEY_COINS] ?: 0.0
            )
        } else {
            null
        }
    }

    suspend fun saveUser(user: User) {
        dataStore.edit { preferences ->
            preferences[KEY_USER_ID] = user.id
            preferences[KEY_DISPLAY_NAME] = user.displayName
            preferences[KEY_AVATAR_URL] = user.avatarUrl
            preferences[KEY_IS_SPEAKER] = user.isSpeaker
            preferences[KEY_COINS] = user.coins
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
    
    suspend fun updateCoins(coins: Double) {
        dataStore.edit { preferences ->
            preferences[KEY_COINS] = coins
        }
    }
}
