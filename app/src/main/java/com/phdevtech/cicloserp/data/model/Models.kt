package com.phdevtech.cicloserp.data.model

import com.phdevtech.cicloserp.data.arr
import com.phdevtech.cicloserp.data.asObj
import com.phdevtech.cicloserp.data.bool
import com.phdevtech.cicloserp.data.dbl
import com.phdevtech.cicloserp.data.int
import com.phdevtech.cicloserp.data.long
import com.phdevtech.cicloserp.data.obj
import com.phdevtech.cicloserp.data.str
import com.phdevtech.cicloserp.data.strList
import com.phdevtech.cicloserp.data.strMap
import com.phdevtech.cicloserp.data.text
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Usuário logado e as permissões dele (mesmos códigos do Auth::MODULES do sistema web). */
data class Me(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String?,
    val role: String,
    val profileName: String?,
    val permissions: Set<String>,
) {
    val isAdmin get() = role == "admin"
    fun can(perm: String) = isAdmin || perm in permissions

    companion object {
        fun from(o: JsonObject) = Me(
            id = o.int("id") ?: 0,
            name = o.str("name").orEmpty(),
            email = o.str("email").orEmpty(),
            phone = o.text("phone"),
            role = o.str("role").orEmpty(),
            profileName = o.text("profile_name"),
            permissions = o.strList("permissions").toSet(),
        )
    }
}

/** Situações da OS (ServiceOrders::STATUSES). */
object OrderStatus {
    const val ABERTA = "aberta"
    const val AGENDADA = "agendada"
    const val A_CAMINHO = "a_caminho"
    const val EM_EXECUCAO = "em_execucao"
    const val PAUSADA = "pausada"
    const val CONCLUIDA = "concluida"

    val LABELS = linkedMapOf(
        ABERTA to "Aberta", AGENDADA to "Agendada", A_CAMINHO to "A caminho",
        EM_EXECUCAO to "Em execução", PAUSADA to "Pausada", CONCLUIDA to "Concluída",
    )
    val OPEN = listOf(ABERTA, AGENDADA, A_CAMINHO, EM_EXECUCAO, PAUSADA)

    fun label(code: String?) = LABELS[code] ?: code.orEmpty()
}

/** OS nas listas (Início, agenda e lista de ordens). */
data class OrderSummary(
    val id: Int,
    val status: String,
    val statusLabel: String,
    val scheduledAt: String?,
    val dueDate: String?,
    val description: String,
    val customer: String,
    val technicianName: String?,
    val taskTypeName: String?,
    val pending: Int,
) {
    companion object {
        fun from(o: JsonObject) = OrderSummary(
            id = o.int("id") ?: 0,
            status = o.str("status").orEmpty(),
            statusLabel = o.text("status_label") ?: OrderStatus.label(o.str("status")),
            scheduledAt = o.text("scheduled_at"),
            dueDate = o.text("due_date"),
            description = o.str("description").orEmpty(),
            customer = o.text("customer_trade_name") ?: o.str("customer_name").orEmpty(),
            technicianName = o.text("technician_name"),
            taskTypeName = o.text("task_type_name"),
            pending = o.int("pending") ?: 0,
        )
    }
}

data class TechHome(
    val today: List<OrderSummary>,
    val done: Int,
    val late: Int,
    val running: Int,
    val quotesCount: Int,
    val quotesTotal: Double,
    val canQuotes: Boolean,
) {
    companion object {
        fun from(o: JsonObject) = TechHome(
            today = o.arr("today").mapNotNull { it.asObj()?.let(OrderSummary::from) },
            done = o.int("done") ?: 0,
            late = o.int("late") ?: 0,
            running = o.int("running") ?: 0,
            quotesCount = o.obj("quotes")?.int("count") ?: 0,
            quotesTotal = o.obj("quotes")?.dbl("total") ?: 0.0,
            canQuotes = o.bool("can_quotes"),
        )
    }
}

/** Pergunta de questionário (Questionnaires::normalizeQuestions). */
data class Question(
    val id: String,
    val label: String,
    val type: String,
    val required: Boolean,
    val description: String?,
    val options: List<String>,
    val geo: Boolean,
    val cond: Cond?,
) {
    data class Cond(val question: String, val op: String, val value: JsonElement?)

    companion object {
        fun from(o: JsonObject) = Question(
            id = o.str("id").orEmpty(),
            label = o.str("label").orEmpty(),
            type = o.str("type").orEmpty(),
            required = o.bool("required"),
            description = o.text("description"),
            options = o.strList("options"),
            geo = o.bool("geo"),
            cond = o.obj("cond")?.let { c -> Cond(c.str("question").orEmpty(), c.str("op").orEmpty(), c["value"]) },
        )
    }
}

data class Questionnaire(
    val id: Int,
    val name: String,
    val questions: List<Question>,
    val linked: Boolean,
    val filled: Int,
    val total: Int,
    val answered: Boolean,
    val answers: JsonObject,
    val missing: List<String>,
    val answeredAt: String? = null,
) {
    companion object {
        fun from(o: JsonObject) = Questionnaire(
            id = o.int("id") ?: 0,
            name = o.str("name").orEmpty(),
            questions = o.arr("questions").mapNotNull { it.asObj()?.let(Question::from) },
            linked = o.bool("linked"),
            filled = o.int("filled") ?: 0,
            total = o.int("total") ?: 0,
            answered = o.bool("answered"),
            answers = o.obj("answers") ?: JsonObject(emptyMap()),
            missing = o.strList("missing"),
            answeredAt = o.text("answered_at"),
        )
    }
}

data class Photo(val id: Int, val image: String, val caption: String?, val createdAt: String?, val userName: String?)

data class OrderItem(
    val id: Int,
    val kind: String,
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val total: Double,
    val review: Boolean,
)

/** Orçamento ligado à OS (página Orçamentos): número, relação (aberto pela OS…), situação e total. */
data class OrderQuote(val id: Int, val relation: String?, val statusLabel: String?, val total: Double)

data class HourRow(val label: String, val at: String, val userName: String?, val reason: String?)

/** Ficha completa da OS (OrderExec::detail). */
data class OrderDetail(
    val id: Int,
    val status: String,
    val statusLabel: String,
    val transitions: List<String>,
    val description: String,
    val notes: String?,
    val scheduledAt: String?,
    val dueDate: String?,
    val startedAt: String?,
    val finishedAt: String?,
    val customerId: Int? = null,
    val customerName: String,
    val customerPhone: String?,
    val customerEmail: String?,
    val customerAddress: String?,
    val equipment: String?,
    val technicianName: String?,
    val taskTypeName: String?,
    val report: String?,
    val km: String?,
    val signerName: String?,
    val signerDoc: String?,
    val signature: String?,
    val publicUrl: String?,
    val questionnaires: List<Questionnaire>,
    val photos: List<Photo>,
    val items: List<OrderItem>,
    val itemsTotal: Double,
    val itemsReview: Int,
    val hours: List<HourRow>,
    val hoursTravel: Long,
    val hoursWork: Long,
    val hoursPause: Long,
    val pending: List<String>,
    val itemKinds: Map<String, String>,
    val equipmentName: String? = null,
    val taskTypeDuration: Int? = null,
    val quotes: List<OrderQuote> = emptyList(),
) {
    val isDone get() = status == OrderStatus.CONCLUIDA

    companion object {
        fun from(root: JsonObject): OrderDetail {
            val o = root.obj("order") ?: JsonObject(emptyMap())
            val address = listOfNotNull(
                listOfNotNull(o.text("customer_street"), o.text("customer_number")).joinToString(", ").ifBlank { null },
                o.text("customer_complement"),
                o.text("customer_district"),
                listOfNotNull(o.text("customer_city"), o.text("customer_state")).joinToString(" - ").ifBlank { null },
            ).joinToString(" · ").ifBlank { null }
            val equipment = listOfNotNull(o.text("equipment_name"), o.text("equipment_serial")).joinToString(" · ").ifBlank { null }
            val items = root.obj("items")
            val hours = root.obj("hours")
            return OrderDetail(
                id = o.int("id") ?: 0,
                status = o.str("status").orEmpty(),
                statusLabel = o.text("status_label") ?: OrderStatus.label(o.str("status")),
                transitions = o.strList("transitions"),
                description = o.str("description").orEmpty(),
                notes = o.text("notes"),
                scheduledAt = o.text("scheduled_at"),
                dueDate = o.text("due_date"),
                startedAt = o.text("started_at"),
                finishedAt = o.text("finished_at"),
                customerId = o.int("customer_id"),
                customerName = o.text("customer_trade_name") ?: o.str("customer_name").orEmpty(),
                customerPhone = o.text("customer_phone"),
                customerEmail = o.text("customer_email"),
                customerAddress = address,
                equipment = equipment,
                equipmentName = o.text("equipment_name"),
                taskTypeDuration = o.int("task_type_duration"),
                quotes = root.arr("quotes").mapNotNull { e -> e.asObj()?.let { OrderQuote(it.int("id") ?: 0, it.text("relation"), it.text("status_label"), it.dbl("total") ?: 0.0) } },
                technicianName = o.text("technician_name"),
                taskTypeName = o.text("task_type_name"),
                report = o.text("report"),
                km = o.text("km"),
                signerName = o.text("signer_name"),
                signerDoc = o.text("signer_doc"),
                signature = o.text("signature"),
                publicUrl = o.text("public_url"),
                questionnaires = root.arr("questionnaires").mapNotNull { it.asObj()?.let(Questionnaire::from) },
                photos = root.arr("photos").mapNotNull { e ->
                    e.asObj()?.let { Photo(it.int("id") ?: 0, it.str("image").orEmpty(), it.text("caption"), it.text("created_at"), it.text("user_name")) }
                },
                items = items?.arr("rows").orEmpty().mapNotNull { e ->
                    e.asObj()?.let {
                        OrderItem(
                            id = it.int("id") ?: 0, kind = it.str("kind").orEmpty(), description = it.str("description").orEmpty(),
                            quantity = it.dbl("quantity") ?: 0.0, unitPrice = it.dbl("unit_price") ?: 0.0,
                            total = it.dbl("total") ?: 0.0, review = it.bool("review"),
                        )
                    }
                },
                itemsTotal = items?.obj("totals")?.dbl("total") ?: 0.0,
                itemsReview = items?.int("review") ?: 0,
                hours = hours?.arr("rows").orEmpty().mapNotNull { e ->
                    e.asObj()?.let { HourRow(it.str("label").orEmpty(), it.str("at").orEmpty(), it.text("user_name"), it.text("reason")) }
                },
                hoursTravel = hours?.long("travel") ?: 0,
                hoursWork = hours?.long("work") ?: 0,
                hoursPause = hours?.long("pause") ?: 0,
                pending = root.strList("pending"),
                itemKinds = root.strMap("kinds"),
            )
        }
    }
}
