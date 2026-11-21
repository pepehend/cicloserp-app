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
            val base = baseUrl ?: throw ApiException(0, "Servidor não configurado")
            val url = base.newBuilder()
                .addPathSegment("api.php")
                .addQueryParameter("r", route)
                .apply { params.forEach { (k, v) -> if (v != null) addQueryParameter(k, v.toString()) } }
                .build()
            val req = Request.Builder().url(url).header("Accept", "application/json")
            // Os dois headers: hospedagens com PHP em CGI/FPM podem descartar o Authorization; o servidor aceita X-Api-Token também.
            token?.let { req.header("Authorization", "Bearer $it").header("X-Api-Token", it) }
            if (body != null) req.post(body.toString().toRequestBody(JSON))
            val resp = try {
                http.newCall(req.build()).execute()
            } catch (e: IOException) {
                throw ApiException(0, "Sem conexão com o servidor. Confira a sua internet e tente de novo.")
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
                json ?: throw ApiException(r.code, "Resposta inválida do servidor.")
            }
        }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

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
