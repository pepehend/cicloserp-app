package com.phdevtech.cicloserp

import com.phdevtech.cicloserp.data.model.Customer
import com.phdevtech.cicloserp.data.model.Product
import com.phdevtech.cicloserp.data.model.QuotesPage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErpModelsTest {
    private fun obj(s: String) = Json.parseToJsonElement(s).jsonObject

    @Test fun customerAceitaNumerosComoTexto() {
        val c = Customer.from(obj("""{"id":"3","name":"Consumidor final","sales_count":"2","active":"1","city":"Arroio","state":"RS"}"""))
        assertEquals(3, c.id); assertEquals(2, c.salesCount); assertTrue(c.active); assertEquals("Arroio/RS", c.cityState)
    }

    @Test fun produtoComEstoqueBaixo() {
        val p = Product.from(obj("""{"id":1,"name":"Filtro","type":"produto","stock":"1","min_stock":"2","sale_price":"10.5","active":1}"""))
        assertTrue(p.lowStock); assertFalse(p.isService)
        assertFalse(Product.from(obj("""{"id":2,"name":"Visita","type":"servico","stock":0,"min_stock":5}""")).lowStock)
    }

    @Test fun orcamentosComIndicadores() {
        val page = QuotesPage.from(obj("""{"rows":[{"id":"5","status":"enviado","total":"100","lead_name":"Lead"}],"total":1,"sum":100,
            "summary":{"pendentes_envio":2,"aguardando_aprovacao":1},"statuses":{"enviado":"Aguardando aprovação"}}"""))
        assertEquals("Lead", page.rows[0].customer); assertEquals(2, page.summary.pendentesEnvio)
        assertEquals("Aguardando aprovação", page.statuses["enviado"])
    }
}
