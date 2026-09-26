package com.curio.notes.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.curio.notes.domain.model.ApiKeyKind
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart

// User-supplied API keys, encrypted at rest with a Keystore-backed master
// key (no custom crypto). Honest limitation: a client-side key cannot be
// made unextractable — this prevents accidental exposure (plain prefs,
// backups, logs), nothing more.
class ApiKeyStorage(context: Context) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        PREFS_NAME,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getKey(kind: ApiKeyKind): String =
        prefs.getString(kind.prefKey, "").orEmpty()

    fun observeKey(kind: ApiKeyKind): Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == kind.prefKey) trySend(getKey(kind))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.onStart { emit(getKey(kind)) }

    fun setKey(kind: ApiKeyKind, value: String) {
        prefs.edit().putString(kind.prefKey, value.trim()).apply()
    }

    fun clearKey(kind: ApiKeyKind) {
        prefs.edit().remove(kind.prefKey).apply()
    }

    companion object {
        // Also referenced by the backup rules (this file must stay excluded
        // from auto-backup: a restored copy is unreadable without the
        // device Keystore anyway, so the user simply re-enters the key).
        const val PREFS_NAME = "curio_api_keys"
    }
}
