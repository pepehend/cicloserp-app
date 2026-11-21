package com.phdevtech.cicloserp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

// Fica fora do backup (res/xml/backup_rules.xml e data_extraction_rules.xml): o token vale como a senha.
private val Context.sessionDataStore by preferencesDataStore(name = "sessao")

/** Endereço do servidor e token de acesso do aparelho. */
class SessionStore(private val context: Context) {

    data class Saved(val serverUrl: String?, val token: String?)

    suspend fun load(): Saved {
        val p = context.sessionDataStore.data.first()
        return Saved(p[KEY_SERVER], p[KEY_TOKEN])
    }

    suspend fun saveLogin(serverUrl: String, token: String) {
        context.sessionDataStore.edit {
            it[KEY_SERVER] = serverUrl
            it[KEY_TOKEN] = token
        }
    }

    /** Sai da conta: apaga o token e mantém o endereço do servidor para o próximo login. */
    suspend fun clearToken() {
        context.sessionDataStore.edit { it.remove(KEY_TOKEN) }
    }

    private companion object {
        val KEY_SERVER = stringPreferencesKey("server_url")
        val KEY_TOKEN = stringPreferencesKey("token")
    }
}
