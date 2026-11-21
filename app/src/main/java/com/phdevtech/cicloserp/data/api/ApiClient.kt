package com.phdevtech.cicloserp.data.api

import com.phdevtech.cicloserp.data.asObj
import com.phdevtech.cicloserp.data.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Erro devolvido pela API ({"error": "..."}) ou de conexão. [status] 0 = sem resposta do servidor. */
class ApiException(val status: Int, message: String) : Exception(message) {
    val unauthorized get() = status == 401
}

/**
 * Cliente do api.php do CiclosERP (as mesmas rotas usadas pelo sistema web).
 * Todo pedido leva "Authorization: Bearer <token>"; com token não há cookie nem CSRF.
 */
class ApiClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build(),
) {
    @Volatile var baseUrl: HttpUrl? = null
    @Volatile var token: String? = null

    /** Chamado quando o servidor recusa o token (expirado, removido em Minha conta ou usuário desativado). */
    @Volatile var onUnauthorized: (() -> Unit)? = null

    suspend fun get(route: String, params: Map<String, Any?> = emptyMap()): JsonElement =
        call(route, params, null)

    suspend fun post(route: String, body: JsonObject = JsonObject(emptyMap())): JsonElement =
        call(route, emptyMap(), body)

    private suspend fun call(route: String, params: Map<String, Any?>, body: JsonObject?): JsonElement =
        withContext(Dispatchers.IO) {
            val base = baseUrl ?: throw ApiException(0, "Informe o endereço do servidor")
            val url = base.newBuilder()
                .addPathSegment("api.php")
                .addQueryParameter("r", route)
                .apply { params.forEach { (k, v) -> if (v != null) addQueryParameter(k, v.toString()) } }
                .build()
            val req = Request.Builder().url(url).header("Accept", "application/json")
            token?.let { req.header("Authorization", "Bearer $it") }
            if (body != null) req.post(body.toString().toRequestBody(JSON))
            val resp = try {
                http.newCall(req.build()).execute()
            } catch (e: IOException) {
                val lan = if (isLocalNetworkHost(base.host)) " Como o servidor está na rede local, confira se o app tem a permissão de rede local (Configurações > Apps > CiclosERP > Permissões)." else ""
                throw ApiException(0, "Sem conexão com o servidor. Confira a internet e o endereço (${base.host}).$lan")
            }
            resp.use { r ->
                val text = r.body.string()
                val json = runCatching { Json.parseToJsonElement(text) }.getOrNull()
                if (!r.isSuccessful) {
                    val msg = json.asObj()?.str("error")
                        ?: "O servidor respondeu com erro ${r.code}."
                    if (r.code == 401 && token != null) onUnauthorized?.invoke()
                    throw ApiException(r.code, msg)
                }
                json ?: throw ApiException(r.code, "Resposta inválida do servidor. O endereço é mesmo do CiclosERP?")
            }
        }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        /** Endereço da rede local (RFC 1918, link-local ou .local): no Android 17+ exige ACCESS_LOCAL_NETWORK. */
        fun isLocalNetworkHost(host: String): Boolean {
            if (host.endsWith(".local", ignoreCase = true)) return true
            val p = host.split('.').mapNotNull { it.toIntOrNull() }
            if (p.size != 4) return false
            return p[0] == 10 || (p[0] == 172 && p[1] in 16..31) || (p[0] == 192 && p[1] == 168) || (p[0] == 169 && p[1] == 254)
        }

        /**
         * Endereço digitado → URL base (com "/" no fim). Sem http(s)://, usa http para IP ou localhost
         * e https para domínios. Aceita também o endereço com "index.php" ou "api.php" no fim.
         */
        fun normalizeBaseUrl(input: String): HttpUrl? {
            var s = input.trim()
            if (s.isEmpty()) return null
            if (!s.contains("://")) {
                val host = s.substringBefore('/').substringBefore(':')
                val plain = host == "localhost" || host.matches(Regex("""\d{1,3}(\.\d{1,3}){3}"""))
                s = (if (plain) "http://" else "https://") + s
            }
            s = s.substringBefore('#').substringBefore('?')
            s = s.removeSuffix("index.php").removeSuffix("api.php")
            if (!s.endsWith("/")) s += "/"
            return s.toHttpUrlOrNull()
        }
    }
}
