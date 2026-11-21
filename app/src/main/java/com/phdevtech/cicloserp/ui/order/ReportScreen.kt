package com.phdevtech.cicloserp.ui.order

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phdevtech.cicloserp.data.model.Me
import com.phdevtech.cicloserp.ui.common.ErrorBox
import com.phdevtech.cicloserp.ui.common.Load
import com.phdevtech.cicloserp.ui.common.LoadingBox
import com.phdevtech.cicloserp.ui.common.SectionTitle
import com.phdevtech.cicloserp.ui.common.SignaturePad
import com.phdevtech.cicloserp.ui.common.appViewModel
import com.phdevtech.cicloserp.ui.common.rememberSignatureState
import com.phdevtech.cicloserp.util.Fmt
import com.phdevtech.cicloserp.util.Images

/** Relato, km rodado e assinatura de quem recebeu o serviço (POST so_report_save). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(me: Me, id: Int, onBack: () -> Unit) {
    val vm = appViewModel(key = "rep$id") { OrderViewModel(it, id) }
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(vm) { vm.messages.collect { snack.showSnackbar(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Relato e assinatura") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar") } },
            )
        },
        snackbarHost = { SnackbarHost(snack) },
    ) { pad ->
        when (val s = state) {
            Load.Loading -> LoadingBox(Modifier.padding(pad))
            is Load.Error -> ErrorBox(s.message, onRetry = vm::load, modifier = Modifier.padding(pad))
            is Load.Ok -> {
                val o = s.data
                val editable = canChange(me, o)
                var report by rememberSaveable(o.id) { mutableStateOf(o.report.orEmpty()) }
                var km by rememberSaveable(o.id) { mutableStateOf(o.km?.removeSuffix(".00")?.replace('.', ',').orEmpty()) }
                var signer by rememberSaveable(o.id) { mutableStateOf(o.signerName.orEmpty()) }
                var doc by rememberSaveable(o.id) { mutableStateOf(Fmt.doc(o.signerDoc)) }
                // null = mantém a assinatura salva; "" = remover; desenhando = nova
                var signing by rememberSaveable(o.id) { mutableStateOf(o.signature == null) }
                var removeSig by rememberSaveable(o.id) { mutableStateOf(false) }
                val pad2 = rememberSignatureState()

                Column(
                    Modifier.fillMaxSize().padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SectionTitle("Relato do que foi feito")
                    OutlinedTextField(
                        value = report, onValueChange = { report = it.take(10000) }, enabled = editable,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp), placeholder = { Text("Descreva o serviço realizado") },
                    )
                    OutlinedTextField(
                        value = km, onValueChange = { v -> km = v.filter { it.isDigit() || it == ',' || it == '.' }.take(10) }, enabled = editable,
                        label = { Text("Km rodado") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    SectionTitle("Quem recebeu o serviço")
                    OutlinedTextField(value = signer, onValueChange = { signer = it.take(120) }, enabled = editable, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = doc, onValueChange = { v -> doc = Fmt.doc(v.filter(Char::isDigit).take(14)) }, enabled = editable,
                        label = { Text("CPF ou CNPJ (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    SectionTitle("Assinatura")
                    if (!signing && o.signature != null && !removeSig) {
                        Images.decodeDataUrl(o.signature)?.let {
                            Image(it, "Assinatura salva", Modifier.fillMaxWidth().height(180.dp).background(Color.White), contentScale = ContentScale.Fit)
                        }
                        if (editable) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { signing = true }) { Text("Colher de novo") }
                            OutlinedButton(onClick = { removeSig = true }) { Text("Remover") }
                        }
                    } else if (editable) {
                        Text("Peça para o cliente assinar com o dedo no quadro abaixo.", style = MaterialTheme.typography.bodySmall)
                        SignaturePad(pad2)
                        OutlinedButton(onClick = pad2::clear) { Text("Limpar") }
                    } else {
                        Text("Sem assinatura", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (editable) Button(
                        enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp),
                        onClick = {
                            // Desenhou: assinatura nova; pediu para remover sem desenhar: apaga; senão mantém a salva
                            val sig: String? = when {
                                !pad2.isEmpty -> pad2.toDataUrl()
                                removeSig -> ""
                                else -> null
                            }
                            vm.run("Relatório salvo", then = onBack) {
                                saveReport(id, report, km, signer, doc.filter(Char::isDigit), sig)
                            }
                        },
                    ) { Text("Salvar") }
                }
            }
        }
    }
}
