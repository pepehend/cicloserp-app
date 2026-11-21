package com.phdevtech.cicloserp.ui.order

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phdevtech.cicloserp.data.model.Me
import com.phdevtech.cicloserp.data.model.Photo
import com.phdevtech.cicloserp.ui.common.ErrorBox
import com.phdevtech.cicloserp.ui.common.Load
import com.phdevtech.cicloserp.ui.common.LoadingBox
import com.phdevtech.cicloserp.ui.common.appViewModel
import com.phdevtech.cicloserp.ui.theme.Web
import com.phdevtech.cicloserp.util.Fmt
import com.phdevtech.cicloserp.util.Images
import kotlinx.coroutines.launch

/**
 * Página Fotos (.tk-photos): título, aviso de só consulta, botão "Adicionar fotos" (câmera ou galeria, várias de uma vez),
 * grade de 3 colunas com o botão de remover em cada foto e o vazio "Nenhuma foto adicionada" (POST so_photo_add / so_photo_delete).
 */
@Composable
fun PhotosScreen(me: Me, id: Int, onBack: () -> Unit) {
    val vm = appViewModel(key = "fotos$id") { OrderViewModel(it, id) }
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(vm) { vm.messages.collect { snack.showSnackbar(it) } }

    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var chooser by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<Photo?>(null) }
    var removing by remember { mutableStateOf<Photo?>(null) }

    // Envia uma ou várias fotos (a web não pede legenda)
    fun upload(uris: List<Uri>) {
        if (uris.isEmpty()) return
        scope.launch {
            val datas = uris.mapNotNull { u -> runCatching { Images.photoDataUrl(ctx, u) }.getOrNull() }
            if (datas.isEmpty()) { vm.message("Não consegui ler a foto"); return@launch }
            vm.run(if (datas.size > 1) "${datas.size} fotos adicionadas" else "Foto adicionada") { datas.forEach { addPhoto(id, it, "") } }
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) cameraUri?.let { upload(listOf(it)) } }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris -> upload(uris) }

    Scaffold(containerColor = Web.bg, snackbarHost = { SnackbarHost(snack) }) { pad ->
        when (val s = state) {
            Load.Loading -> LoadingBox(Modifier.padding(pad))
            is Load.Error -> ErrorBox(s.message, onRetry = vm::load, modifier = Modifier.padding(pad))
            is Load.Ok -> {
                val o = s.data
                val editable = canChange(me, o)
                Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 14.dp)) {
                    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar", tint = Web.text) }
                    }
                    Text("Fotos", color = Web.text, fontSize = 20.25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
                    if (!editable) Row(
                        Modifier.fillMaxWidth().padding(bottom = 10.dp).background(Web.surface2, RoundedCornerShape(12.dp)).border(1.dp, Web.line, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Outlined.Lock, null, Modifier.size(20.dp), tint = Web.muted)
                        Text("Ordem concluída: só consulta. Para alterar, use o computador.", color = Web.muted, fontSize = 13.4.sp)
                    }
                    if (editable) Row(
                        Modifier.clip(RoundedCornerShape(10.dp)).background(Web.accent).clickable(enabled = !busy) { chooser = true }.padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.PhotoCamera, null, Modifier.size(20.dp), tint = Web.accentInk)
                        Spacer(Modifier.size(8.dp))
                        Text("Adicionar fotos", color = Web.accentInk, fontSize = 13.8.sp, fontWeight = FontWeight.Bold)
                    }
                    if (o.photos.isEmpty()) Column(Modifier.fillMaxWidth().padding(top = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Image, null, Modifier.size(44.dp), tint = Web.muted)
                        Text("Nenhuma foto adicionada", color = Web.muted, fontSize = 15.sp)
                    } else LazyVerticalGrid(
                        columns = GridCells.Fixed(3), contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize(),
                    ) {
                        items(o.photos, key = { it.id }) { p ->
                            val img = remember(p.id) { Images.decodeDataUrl(p.image, 400) }
                            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(Web.surface2).clickable { viewing = p }) {
                                if (img != null) Image(img, p.caption, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                if (editable) Box(
                                    Modifier.align(Alignment.TopEnd).padding(4.dp).size(30.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.55f)).clickable { removing = p },
                                    contentAlignment = Alignment.Center,
                                ) { Icon(Icons.Outlined.DeleteOutline, "Remover foto", Modifier.size(18.dp), tint = Color.White) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (chooser) AlertDialog(
        onDismissRequest = { chooser = false }, containerColor = Web.surface,
        title = { Text("Adicionar fotos") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Tirar foto" to { chooser = false; Images.newCameraUri(ctx).also { cameraUri = it; camera.launch(it) }; Unit },
                    "Escolher da galeria" to { chooser = false; gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }).forEach { (t, a) ->
                    Text(t, Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, Web.line, RoundedCornerShape(12.dp)).clickable { a() }.padding(12.dp), color = Web.text, fontSize = 14.4.sp)
                }
            }
        },
        confirmButton = {}, dismissButton = { TextButton({ chooser = false }) { Text("Cancelar", color = Web.accent) } },
    )

    removing?.let { p ->
        AlertDialog(
            onDismissRequest = { removing = null }, containerColor = Web.surface,
            title = { Text("Remover esta foto?") },
            confirmButton = { TextButton({ removing = null; vm.run("Foto removida") { deletePhoto(p.id) } }) { Text("Remover", color = Web.danger) } },
            dismissButton = { TextButton({ removing = null }) { Text("Cancelar", color = Web.accent) } },
        )
    }

    viewing?.let { p ->
        val img = remember(p.id) { Images.decodeDataUrl(p.image) }
        AlertDialog(
            onDismissRequest = { viewing = null }, containerColor = Web.surface,
            title = { Text(p.caption ?: "Foto") },
            text = {
                Column {
                    if (img != null) Image(img, p.caption, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
                    Text(listOfNotNull(Fmt.dateTime(p.createdAt), p.userName).joinToString(" · "), color = Web.muted, fontSize = 12.8.sp)
                }
            },
            confirmButton = { TextButton({ viewing = null }) { Text("Fechar", color = Web.accent) } },
        )
    }
}
