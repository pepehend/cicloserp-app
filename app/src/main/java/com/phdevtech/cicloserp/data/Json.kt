package com.phdevtech.cicloserp.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/*
 * Leitura tolerante do JSON da API PHP: no MySQL os números chegam como texto ("12", "3.50"),
 * no SQLite como número, e booleanos podem vir como 0/1. Estes atalhos aceitam os dois formatos.
 */

fun JsonElement?.asObj(): JsonObject? = this as? JsonObject
fun JsonElement?.asArr(): JsonArray = this as? JsonArray ?: JsonArray(emptyList())

fun JsonElement?.asStr(): String? = (this as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content

fun JsonObject.str(key: String): String? = this[key].asStr()

/** Texto não vazio (null se vazio ou ausente). */
fun JsonObject.text(key: String): String? = str(key)?.takeIf { it.isNotBlank() }

fun JsonObject.int(key: String): Int? = str(key)?.let { it.toIntOrNull() ?: it.toDoubleOrNull()?.toInt() }

fun JsonObject.long(key: String): Long? = str(key)?.let { it.toLongOrNull() ?: it.toDoubleOrNull()?.toLong() }

fun JsonObject.dbl(key: String): Double? = str(key)?.toDoubleOrNull()

fun JsonObject.bool(key: String): Boolean = when (str(key)?.lowercase()) {
    "1", "true" -> true
    else -> false
}

fun JsonObject.obj(key: String): JsonObject? = this[key].asObj()

fun JsonObject.arr(key: String): JsonArray = this[key].asArr()

fun JsonObject.strList(key: String): List<String> = arr(key).mapNotNull { it.asStr() }

/** Objeto JSON "chave → texto" (ex.: STATUSES do PHP) como mapa. */
fun JsonObject.strMap(key: String): Map<String, String> =
    obj(key)?.mapNotNull { (k, v) -> v.asStr()?.let { k to it } }?.toMap().orEmpty()
