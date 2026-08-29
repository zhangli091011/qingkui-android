package cn.qingkui.app.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cn.qingkui.app.data.remote.dto.AuthResponse
import kotlinx.coroutines.flow.first

private val Context.authDataStore by preferencesDataStore(name = "auth_session")

class TokenStore(private val context: Context) {
    suspend fun accessToken(): String? = context.authDataStore.data.first()[ACCESS_TOKEN]
    suspend fun refreshToken(): String? = context.authDataStore.data.first()[REFRESH_TOKEN]
    suspend fun nickname(): String? = context.authDataStore.data.first()[NICKNAME]

    suspend fun save(response: AuthResponse) {
        context.authDataStore.edit { values ->
            values[ACCESS_TOKEN] = response.accessToken
            values[REFRESH_TOKEN] = response.refreshToken
            values[NICKNAME] = response.user.nickname
        }
    }

    suspend fun clear() {
        context.authDataStore.edit { it.clear() }
    }

    private companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val NICKNAME = stringPreferencesKey("nickname")
    }
}
