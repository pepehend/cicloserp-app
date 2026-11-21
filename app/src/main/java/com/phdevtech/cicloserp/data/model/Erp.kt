package com.phdevtech.cicloserp.data.model

import com.phdevtech.cicloserp.data.arr
import com.phdevtech.cicloserp.data.asObj
import com.phdevtech.cicloserp.data.bool
import com.phdevtech.cicloserp.data.dbl
import com.phdevtech.cicloserp.data.int
import com.phdevtech.cicloserp.data.obj
import com.phdevtech.cicloserp.data.str
import com.phdevtech.cicloserp.data.strMap
import com.phdevtech.cicloserp.data.text
import kotlinx.serialization.json.JsonObject

/** Cliente nas listas (GET customers; tabela customers). */
data class Customer(
    val id: Int,
    val name: String,
    val tradeName: String?,
    val document: String?,
    val phone: String?,
    val email: String?,
    val city: String?,
    val state: String?,
    val active: Boolean,
    val salesCount: Int,
    val lastSale: String?,
) {
    val displayName get() = name
    val cityState get() = listOfNotNull(city, state).joinToString("/").ifBlank { null }

    companion object {
        fun from(o: JsonObject) = Customer(
            id = o.int("id") ?: 0,
            name = o.str("name").orEmpty(),
            tradeName = o.text("trade_name"),
            document = o.text("document"),
            phone = o.text("phone"),
            email = o.text("email"),
            city = o.text("city"),
            state = o.text("state"),
            active = o.str("active") != "0",
            salesCount = o.int("sales_count") ?: 0,
            lastSale = o.text("last_sale"),
        )
    }
}

data class CustomerStats(val count: Int, val total: Double, val profit: Double, val lastSale: String?) {
    val ticket get() = if (count > 0) total / count else 0.0
}

data class TopProduct(val name: String, val unit: String?, val qty: Double, val total: Double)

/** Ficha do cliente (GET customer). */
data class CustomerDetail(
    val customer: Customer,
    val personType: String,
    val address: String?,
    val cep: String?,
    val notes: String?,
    val stats: CustomerStats,
    val topProducts: List<TopProduct>,
    val equipments: List<Equipment>,
    val portalStatus: String?,
) {
    companion object {
        fun from(o: JsonObject): CustomerDetail {
            val st = o.obj("stats")
            val street = listOfNotNull(o.text("street"), o.text("number"), o.text("complement"), o.text("district"))
                .joinToString(", ")
            val addr = listOfNotNull(street.ifBlank { null }, o.text("city")?.let { c -> c + (o.text("state")?.let { "/$it" } ?: "") })
                .joinToString(" · ").ifBlank { null }
            return CustomerDetail(
                customer = Customer.from(o),
                personType = if (o.str("type") == "pf") "Pessoa física" else "Pessoa jurídica",
                address = addr,
                cep = o.text("cep"),
                notes = o.text("notes"),
                stats = CustomerStats(st?.int("n") ?: 0, st?.dbl("total") ?: 0.0, st?.dbl("profit") ?: 0.0, st?.text("last_sale")),
                topProducts = o.arr("top_products").mapNotNull { it.asObj() }.map {
                    TopProduct(it.str("name").orEmpty(), it.text("unit"), it.dbl("qty") ?: 0.0, it.dbl("total") ?: 0.0)
                },
                equipments = o.arr("equipments").mapNotNull { it.asObj()?.let(Equipment::from) },
                portalStatus = o.obj("portal")?.text("status") ?: o.obj("portal")?.let { if (it.bool("has_access")) "com acesso" else "sem acesso" },
            )
        }
    }
}

/** Produto ou serviço (tabela products). */
data class Product(
    val id: Int,
    val name: String,
    val type: String,
    val sku: String?,
    val brand: String?,
    val category: String?,
    val unit: String,
    val stock: Double,
    val minStock: Double,
    val salePrice: Double,
    val avgCost: Double,
    val location: String?,
    val description: String?,
    val active: Boolean,
) {
    val isService get() = type == "servico"
    val lowStock get() = !isService && minStock > 0 && stock <= minStock

    companion object {
        fun from(o: JsonObject) = Product(
            id = o.int("id") ?: 0,
            name = o.str("name").orEmpty(),
            type = o.str("type") ?: "produto",
            sku = o.text("sku"),
            brand = o.text("brand"),
            category = o.text("category"),
            unit = o.text("unit") ?: "un",
            stock = o.dbl("stock") ?: 0.0,
            minStock = o.dbl("min_stock") ?: 0.0,
            salePrice = o.dbl("sale_price") ?: 0.0,
            avgCost = o.dbl("avg_cost") ?: 0.0,
            location = o.text("location"),
            description = o.text("description"),
            active = o.str("active") != "0",
        )
    }
}

/** Equipamento de um cliente (tabela equipments). */
data class Equipment(
    val id: Int,
    val customerId: Int,
    val name: String,
    val brand: String?,
    val model: String?,
    val serial: String?,
    val category: String?,
    val warrantyUntil: String?,
    val customerName: String?,
    val notes: String?,
    val active: Boolean,
) {
    /** "XAS 420 · BQD238761", como na ficha do cliente do sistema web. */
    val label get() = listOfNotNull(name, serial).joinToString(" · ")

    companion object {
        fun from(o: JsonObject) = Equipment(
            id = o.int("id") ?: 0,
            customerId = o.int("customer_id") ?: 0,
            name = o.str("name").orEmpty(),
            brand = o.text("brand"),
            model = o.text("model"),
            serial = o.text("serial"),
            category = o.text("category"),
            warrantyUntil = o.text("warranty_until"),
            customerName = o.text("customer_trade_name") ?: o.text("customer_name"),
            notes = o.text("notes"),
            active = o.str("active") != "0",
        )
    }
}

/** Orçamento nas listas (GET quotes). */
data class QuoteRow(
    val id: Int,
    val status: String,
    val customer: String,
    val total: Double,
    val requestDate: String?,
    val expiresAt: String?,
    val externalCode: String?,
    val userName: String?,
    val items: Int,
) {
    companion object {
        fun from(o: JsonObject) = QuoteRow(
            id = o.int("id") ?: 0,
            status = o.str("status").orEmpty(),
            customer = o.text("customer_trade_name") ?: o.text("customer_name") ?: o.text("lead_name") ?: "Sem cliente",
            total = o.dbl("total") ?: 0.0,
            requestDate = o.text("request_date"),
            expiresAt = o.text("expires_at"),
            externalCode = o.text("external_code"),
            userName = o.text("user_name"),
            items = o.int("items") ?: 0,
        )
    }
}

/** Indicadores do topo da tela de orçamentos (Quotes::summary) e filtros de atalho da lista. */
data class QuoteSummary(
    val pendentesEnvio: Int, val aguardandoAprovacao: Int, val expiramMes: Int, val recusadosMes: Int,
    val aprovadosMes: Int, val aguardandoVenda: Int, val vendidosMes: Int,
) {
    companion object {
        val EMPTY = QuoteSummary(0, 0, 0, 0, 0, 0, 0)
        fun from(o: JsonObject?) = if (o == null) EMPTY else QuoteSummary(
            o.int("pendentes_envio") ?: 0, o.int("aguardando_aprovacao") ?: 0, o.int("expiram_mes") ?: 0,
            o.int("recusados_mes") ?: 0, o.int("aprovados_mes") ?: 0, o.int("aguardando_venda") ?: 0, o.int("vendidos_mes") ?: 0,
        )
    }
}

data class QuotesPage(val rows: List<QuoteRow>, val total: Int, val sum: Double, val summary: QuoteSummary, val statuses: Map<String, String>) {
    companion object {
        fun from(o: JsonObject) = QuotesPage(
            rows = o.arr("rows").mapNotNull { it.asObj()?.let(QuoteRow::from) },
            total = o.int("total") ?: 0,
            sum = o.dbl("sum") ?: 0.0,
            summary = QuoteSummary.from(o.obj("summary")),
            statuses = o.strMap("statuses"),
        )
    }
}

data class QuoteItem(val description: String, val kind: String, val unit: String?, val quantity: Double, val unitPrice: Double, val total: Double)

/** Orçamento completo (GET quote). */
data class QuoteDetail(
    val id: Int,
    val status: String,
    val statusLabel: String,
    val customer: String,
    val customerPhone: String?,
    val customerEmail: String?,
    val requestDate: String?,
    val expiresAt: String?,
    val total: Double,
    val notes: String?,
    val userName: String?,
    val items: List<QuoteItem>,
    val extras: List<Pair<String, Double>>,
    val publicUrl: String?,
) {
    companion object {
        fun from(o: JsonObject) = QuoteDetail(
            id = o.int("id") ?: 0,
            status = o.str("status").orEmpty(),
            statusLabel = o.text("status_label") ?: o.str("status").orEmpty(),
            customer = o.text("customer_trade_name") ?: o.text("customer_name") ?: o.text("lead_name") ?: "Sem cliente",
            customerPhone = o.text("customer_phone"),
            customerEmail = o.text("customer_email"),
            requestDate = o.text("request_date"),
            expiresAt = o.text("expires_at"),
            total = o.dbl("total") ?: 0.0,
            notes = o.text("notes"),
            userName = o.text("user_name"),
            items = o.arr("items").mapNotNull { it.asObj() }.map {
                QuoteItem(it.str("description").orEmpty(), it.str("kind").orEmpty(), it.text("unit") ?: it.text("product_unit"),
                    it.dbl("quantity") ?: 0.0, it.dbl("unit_price") ?: 0.0, it.dbl("total") ?: 0.0)
            },
            extras = o.arr("extras").mapNotNull { it.asObj() }.map { it.str("description").orEmpty() to (it.dbl("amount") ?: 0.0) },
            publicUrl = o.text("public_url"),
        )
    }
}
