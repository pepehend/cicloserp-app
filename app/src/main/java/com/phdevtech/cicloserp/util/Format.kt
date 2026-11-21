package com.phdevtech.cicloserp.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Datas e valores no formato brasileiro. A API usa "Y-m-d H:i:s" (horário de Brasília). */
object Fmt {
    private val BR = Locale.forLanguageTag("pt-BR")
    private val API_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", BR)
    private val D = DateTimeFormatter.ofPattern("dd/MM/yyyy", BR)
    private val HM = DateTimeFormatter.ofPattern("HH:mm", BR)
    private val WEEKDAY = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", BR)
    val API_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun parse(s: String?): LocalDateTime? = s?.let { v ->
        runCatching { LocalDateTime.parse(v.take(19).replace('T', ' '), API_DT) }.getOrNull()
            ?: runCatching { LocalDate.parse(v.take(10)).atStartOfDay() }.getOrNull()
    }

    fun dateTime(s: String?): String = parse(s)?.format(DT) ?: s.orEmpty()
    fun date(s: String?): String = parse(s)?.format(D) ?: s.orEmpty()
    fun time(s: String?): String = parse(s)?.format(HM) ?: ""
    fun weekday(d: LocalDate): String = d.format(WEEKDAY).replaceFirstChar { it.uppercase() }

    fun money(v: Double): String = NumberFormat.getCurrencyInstance(BR).format(v)

    /** Segundos → "1h 05min" / "12min". */
    fun duration(seconds: Long): String {
        val m = (seconds + 30) / 60
        return if (m >= 60) "${m / 60}h ${(m % 60).toString().padStart(2, '0')}min" else "${m}min"
    }

    /** Só os dígitos de CPF/CNPJ → com máscara. */
    fun doc(digits: String?): String {
        val d = digits.orEmpty().filter(Char::isDigit)
        return when (d.length) {
            11 -> "${d.substring(0, 3)}.${d.substring(3, 6)}.${d.substring(6, 9)}-${d.substring(9)}"
            14 -> "${d.substring(0, 2)}.${d.substring(2, 5)}.${d.substring(5, 8)}/${d.substring(8, 12)}-${d.substring(12)}"
            else -> digits.orEmpty()
        }
    }
}
