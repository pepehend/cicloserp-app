package com.phdevtech.cicloserp.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phdevtech.cicloserp.data.CiclosRepository
import com.phdevtech.cicloserp.data.model.OrderDetail
import com.phdevtech.cicloserp.data.model.OrderStatus
import com.phdevtech.cicloserp.ui.common.Load
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Ficha da OS (GET service_order_detail) e as ações de execução. Usado também pelas telas do relatório. */
class OrderViewModel(private val repo: CiclosRepository, val id: Int) : ViewModel() {
    private val _state = MutableStateFlow<Load<OrderDetail>>(Load.Loading)
    val state = _state.asStateFlow()
    val busy = MutableStateFlow(false)
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = try { Load.Ok(repo.order(id)) } catch (e: Exception) {
                (_state.value as? Load.Ok) ?: Load.Error(e.message ?: "Erro ao carregar a OS")
            }
        }
    }

    /** Executa uma ação no servidor, recarrega a ficha e mostra a mensagem. [then] roda só se deu certo. */
    fun run(success: String?, then: () -> Unit = {}, action: suspend CiclosRepository.() -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                repo.action()
                success?.let { _messages.send(it) }
                runCatching { _state.value = Load.Ok(repo.order(id)) }
                then()
            } catch (e: Exception) {
                _messages.send(e.message ?: "Não foi possível concluir")
            } finally {
                busy.value = false
            }
        }
    }

    fun setStatus(to: String, reason: String? = null) {
        val cur = (state.value as? Load.Ok)?.data?.status
        val msg = when (to) {
            OrderStatus.A_CAMINHO -> "A caminho da tarefa"
            OrderStatus.EM_EXECUCAO -> if (cur == OrderStatus.PAUSADA) "Atendimento retomado" else "Check-in feito: atendimento iniciado"
            OrderStatus.PAUSADA -> "Atendimento pausado"
            OrderStatus.CONCLUIDA -> "Check-out feito: ordem concluída"
            else -> "Situação atualizada"
        }
        run(msg) { setStatus(id, to, reason) }
    }

    fun message(text: String) { viewModelScope.launch { _messages.send(text) } }
}
