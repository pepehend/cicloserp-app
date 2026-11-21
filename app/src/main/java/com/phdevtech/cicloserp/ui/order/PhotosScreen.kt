package com.phdevtech.cicloserp.ui.order

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phdevtech.cicloserp.data.model.Me
import com.phdevtech.cicloserp.data.model.Photo
import com.phdevtech.cicloserp.ui.common.EmptyBox
import com.phdevtech.cicloserp.ui.common.ErrorBox
import com.phdevtech.cicloserp.ui.common.Load
import com.phdevtech.cicloserp.ui.common.LoadingBox
import com.phdevtech.cicloserp.ui.common.appViewModel
import com.phdevtech.cicloserp.util.Fmt
import com.phdevtech.cicloserp.util.Images
import kotlinx.coroutines.launch

/** Fotos da OS (POST so_photo_add / so_photo_delete). Câmera do aparelho ou galeria. */
@OptIn(ExperimentalMaterial3Api::class)
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
    var pending by rememberSaveable { mutableStateOf<Uri?>(null) }  // foto escolhida, esperando a legenda
    var viewing by remember { mutableStateOf<Photo?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) pending = cameraUri }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) pending = uri }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fotos") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar") } },
            )
        },
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            val o = (state as? Load.Ok)?.data
            if (o != null && canChange(me, o)) Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { Images.newCameraUri(ctx).also { cameraUri = it; camera.launch(it) } }, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.PhotoCamera, null); Text("  Tirar foto")
                }
                OutlinedButton(onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.PhotoLibrary, null); Text("  Galeria")
                }
            }
        },
    ) { pad ->
        when (val s = state) {
            Load.Loading -> LoadingBox(Modifier.padding(pad))
            is Load.Error -> ErrorBox(s.message, onRetry = vm::load, modifier = Modifier.padding(pad))
            is Load.Ok -> {
                val o = s.data
                if (o.photos.isEmpty()) EmptyBox("Nenhuma foto nesta OS.", Modifier.padding(pad))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp), contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize().padding(pad),
                ) {
                    items(o.photos, key = { it.id }) { p ->
                        val img = remember(p.id) { Images.decodeDataUrl(p.image, 400) }
                        Column(Modifier.clickable { viewing = p }) {
                            if (img != null) Image(img, p.caption, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                            Text(p.caption ?: Fmt.dateTime(p.createdAt), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                    }
                }
            }
        }
    }

    pending?.let { uri ->
        var caption by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Adicionar foto") },
            text = { OutlinedTextField(value = caption, onValueChange = { caption = it.take(200) }, label = { Text("Legenda (opcional)") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    scope.launch {
                        val data = runCatching { Images.photoDataUrl(ctx, uri) }.getOrElse { vm.message("Não consegui ler a foto: ${it.message}"); return@launch }
                        vm.run("Foto adicionada") { addPhoto(id, data, caption.trim()) }
                    }
                }) { Text("Enviar") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancelar") } },
        )
    }

    viewing?.let { p ->
        val o = (state as? Load.Ok)?.data
        val img = remember(p.id) { Images.decodeDataUrl(p.image) }
        AlertDialog(
            onDismissRequest = { viewing = null },
            title = { Text(p.caption ?: "Foto") },
            text = {
                Column {
                    if (img != null) Image(img, p.caption, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
                    Text(listOfNotNull(Fmt.dateTime(p.createdAt), p.userName).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { viewing = null }) { Text("Fechar") } },
            dismissButton = {
                if (o != null && canChange(me, o)) TextButton(onClick = { viewing = null; vm.run("Foto excluída") { deletePhoto(p.id) } }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }
}
