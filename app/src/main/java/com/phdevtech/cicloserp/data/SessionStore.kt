package com.phdevtech.cicloserp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

// Fica fora do backup (res/xml/backup_rules.xml e data_extraction_rules.xml): o token vale como a senha.
private val Context.sessionDataStore by preferencesDataStore(name = "sessao")

/** Token de acesso do aparelho (o servidor é fixo: AppConfig.SERVER_URL). */
class SessionStore(private val context: Context) {

    data class Saved(val token: String?)

    suspend fun load(): Saved {
        val p = context.sessionDataStore.data.first()
        return Saved(p[KEY_TOKEN])
    }

    suspend fun saveToken(token: String) {
        context.sessionDataStore.edit { it[KEY_TOKEN] = token }
    }

    /** Sai da conta: apaga o token. */
    suspend fun clearToken() {
        context.sessionDataStore.edit { it.remove(KEY_TOKEN) }
    }

    private companion object {
        val KEY_TOKEN = stringPreferencesKey("token")
    }
}
