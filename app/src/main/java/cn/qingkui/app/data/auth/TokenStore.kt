package cn.qingkui.app.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import cn.qingkui.app.data.remote.dto.AuthResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.KeyStore

private val Context.authDataStore by preferencesDataStore(name = "auth_session")

class TokenStore(private val context: Context) {
    private val migrationMutex = Mutex()
    private val securePreferences: SharedPreferences by lazy(::createSecurePreferences)

    suspend fun accessToken(): String? = read(ACCESS_TOKEN_NAME)
    suspend fun refreshToken(): String? = read(REFRESH_TOKEN_NAME)
    suspend fun nickname(): String? = read(NICKNAME_NAME)

    suspend fun save(response: AuthResponse) {
        migrateLegacySession()
        check(
            securePreferences.edit()
                .putString(ACCESS_TOKEN_NAME, response.accessToken)
                .putString(REFRESH_TOKEN_NAME, response.refreshToken)
                .putString(NICKNAME_NAME, response.user.nickname)
                .commit()
        ) { "无法安全保存登录会话" }
    }

    private suspend fun read(key: String): String? {
        migrateLegacySession()
        return runCatching { securePreferences.getString(key, null) }
            .getOrElse {
                clearSecureSession()
                null
            }
    }

    private suspend fun migrateLegacySession() {
        if (securePreferences.getBoolean(MIGRATION_COMPLETE, false)) return
        migrationMutex.withLock {
            if (securePreferences.getBoolean(MIGRATION_COMPLETE, false)) return
            val legacy = context.authDataStore.data.first()
            val editor = securePreferences.edit()
                .putBoolean(MIGRATION_COMPLETE, true)
            legacy[ACCESS_TOKEN]?.let { editor.putString(ACCESS_TOKEN_NAME, it) }
            legacy[REFRESH_TOKEN]?.let { editor.putString(REFRESH_TOKEN_NAME, it) }
            legacy[NICKNAME]?.let { editor.putString(NICKNAME_NAME, it) }
            check(editor.commit()) { "无法迁移登录会话" }
            context.authDataStore.edit { it.clear() }
        }
    }

    suspend fun clear() {
        clearSecureSession()
        context.authDataStore.edit { it.clear() }
    }

    private fun clearSecureSession() {
        securePreferences.edit().clear().putBoolean(MIGRATION_COMPLETE, true).commit()
    }

    @Suppress("DEPRECATION")
    private fun createSecurePreferences(): SharedPreferences {
        fun create(): SharedPreferences {
            val masterKey = MasterKey.Builder(context, MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFERENCES,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }

        return runCatching(::create).getOrElse {
            context.deleteSharedPreferences(ENCRYPTED_PREFERENCES)
            runCatching {
                KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(MASTER_KEY_ALIAS)
            }
            create()
        }
    }

    private companion object {
        const val ENCRYPTED_PREFERENCES = "auth_session_encrypted"
        const val MASTER_KEY_ALIAS = "qingkui_auth_master_key"
        const val MIGRATION_COMPLETE = "legacy_migration_complete"
        const val ACCESS_TOKEN_NAME = "access_token"
        const val REFRESH_TOKEN_NAME = "refresh_token"
        const val NICKNAME_NAME = "nickname"
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val NICKNAME = stringPreferencesKey("nickname")
    }
}
