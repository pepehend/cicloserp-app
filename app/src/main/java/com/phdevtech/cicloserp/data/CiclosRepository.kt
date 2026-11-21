package com.phdevtech.cicloserp.data

import android.os.Build
import com.phdevtech.cicloserp.data.api.ApiClient
import com.phdevtech.cicloserp.data.api.ApiException
import com.phdevtech.cicloserp.data.model.Me
import com.phdevtech.cicloserp.data.model.OrderDetail
import com.phdevtech.cicloserp.data.model.OrderSummary
import com.phdevtech.cicloserp.data.model.TechHome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Estado da sessão do app. */
sealed interface AuthState {
    data object Loading : AuthState
    data class LoggedOut(val serverUrl: String?, val message: String? = null) : AuthState
    data class LoggedIn(val me: Me, val serverUrl: String, val serverVersion: String?) : AuthState
}

/**
 * Acesso ao CiclosERP pelo api.php: as mesmas rotas, regras e permissões do sistema web
 * (e o mesmo banco de dados). Login por token (POST token_login, CiclosERP 3.4.0+).
 */
class CiclosRepository(private val api: ApiClient, private val store: SessionStore) {

    private val _auth = MutableStateFlow<AuthState>(AuthState.Loading)
    val auth: StateFlow<AuthState> = _auth.asStateFlow()

    init {
        api.onUnauthorized = {
            api.token = null
            _auth.value = AuthState.LoggedOut(api.baseUrl?.toString(), "Seu acesso expirou ou foi removido. Entre de novo.")
        }
    }

    /** Abre o app: com token salvo, confere no servidor quem é o usuário. */
    suspend fun restore() {
        val saved = store.load()
        val base = saved.serverUrl?.let(ApiClient::normalizeBaseUrl)
        api.baseUrl = base
        if (base == null || saved.token == null) {
            _auth.value = AuthState.LoggedOut(saved.serverUrl)
            return
        }
        api.token = saved.token
        _auth.value = try {
            val r = api.get("me").asObj()
            val u = r?.obj("user") ?: throw ApiException(401, "Entre de novo.")
            AuthState.LoggedIn(Me.from(u), base.toString(), r.text("version"))
        } catch (e: ApiException) {
            if (e.status == 401) {
                store.clearToken(); api.token = null
                AuthState.LoggedOut(base.toString(), "Seu acesso expirou. Entre de novo.")
            } else {
                // Sem internet: não dá para conferir agora; mostra o login com o aviso
                AuthState.LoggedOut(base.toString(), e.message)
            }
        }
    }

    suspend fun login(serverInput: String, email: String, password: String) {
        val base = ApiClient.normalizeBaseUrl(serverInput) ?: throw ApiException(0, "Endereço do servidor inválido")
        api.baseUrl = base
        api.token = null
        val body = buildJsonObject {
            put("email", email.trim())
            put("password", password)
            put("device", deviceName())
        }
        val r = try {
            api.post("token_login", body).asObj()
        } catch (e: ApiException) {
            if (e.status == 404) throw ApiException(404, "Este servidor ainda não aceita o aplicativo. Ele precisa do CiclosERP 3.4.0 ou mais novo.")
            throw e
        } ?: throw ApiException(0, "Resposta inválida do servidor")
        val token = r.text("token") ?: throw ApiException(0, "O servidor não devolveu o acesso do aplicativo")
        val me = r.obj("user")?.let(Me::from) ?: throw ApiException(0, "Resposta inválida do servidor")
        api.token = token
        store.saveLogin(base.toString(), token)
        _auth.value = AuthState.LoggedIn(me, base.toString(), r.text("version"))
    }

    /** Sair: apaga o token no servidor (logout) e no aparelho. */
    suspend fun logout() {
        runCatching { api.post("logout") }
        store.clearToken()
        api.token = null
        _auth.value = AuthState.LoggedOut(api.baseUrl?.toString())
    }

    // ---------------- Ordens de serviço ----------------

    suspend fun techHome(all: Boolean): TechHome =
        TechHome.from(api.get("tech_home", mapOf("all" to if (all) 1 else null)).asObj() ?: JsonObject(emptyMap()))

    suspend fun techTasks(from: String, to: String, all: Boolean): List<OrderSummary> =
        api.get("tech_tasks", mapOf("from" to from, "to" to to, "all" to if (all) 1 else null))
            .asObj()?.arr("rows").orEmpty().mapNotNull { it.asObj()?.let(OrderSummary::from) }

    /** Lista de OS: status = "abertas", "todas" ou um código; q = busca (cliente, descrição, número). */
    suspend fun orders(status: String, q: String?): List<OrderSummary> =
        api.get("service_orders", mapOf("status" to status, "q" to q?.takeIf { it.isNotBlank() }))
            .asObj()?.arr("rows").orEmpty().mapNotNull { it.asObj()?.let(OrderSummary::from) }

    suspend fun order(id: Int): OrderDetail =
        OrderDetail.from(api.get("service_order_detail", mapOf("id" to id)).asObj() ?: throw ApiException(0, "OS não encontrada"))

    /** A caminho, check-in, pausa (com motivo), retomar e check-out. */
    suspend fun setStatus(id: Int, status: String, reason: String? = null) {
        api.post("service_order_status", buildJsonObject {
            put("id", id); put("status", status)
            if (reason != null) put("reason", reason)
        })
    }

    suspend fun saveAnswers(id: Int, questionnaireId: Int, answers: Map<String, JsonElement>): List<String> {
        val r = api.post("so_answers_save", buildJsonObject {
            put("id", id); put("questionnaire_id", questionnaireId)
            put("answers", JsonObject(answers))
        }).asObj()
        return r?.strList("missing").orEmpty()
    }

    /**
     * Relato, km e quem assinou. [signature]: null = mantém a atual; "" = remove; data URL = nova.
     */
    suspend fun saveReport(id: Int, report: String, km: String, signerName: String, signerDoc: String, signature: String?) {
        api.post("so_report_save", buildJsonObject {
            put("id", id)
            put("report", report); put("km", km)
            put("signer_name", signerName); put("signer_doc", signerDoc)
            if (signature != null) put("signature", signature)
        })
    }

    suspend fun addPhoto(id: Int, dataUrl: String, caption: String) {
        api.post("so_photo_add", buildJsonObject { put("id", id); put("image", dataUrl); put("caption", caption) })
    }

    suspend fun deletePhoto(photoId: Int) {
        api.post("so_photo_delete", buildJsonObject { put("photo_id", photoId) })
    }

    private fun deviceName(): String {
        val m = Build.MODEL.orEmpty()
        val brand = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
        val name = if (m.startsWith(brand, ignoreCase = true)) m else "$brand $m"
        return "App Android · ${name.trim()}".take(120)
    }

    companion object {
        /** Valor JSON de texto (ou null JSON para vazio). */
        fun jsonText(s: String?): JsonElement = if (s == null) JsonNull else JsonPrimitive(s)
    }
}
