package com.phdevtech.cicloserp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/** Fotos e assinaturas como data URL (o formato que o CiclosERP guarda). */
object Images {
    /** Limite do servidor para uma imagem em data URL (OrderExec::MAX_IMAGE = 1.200.000), com folga. */
    private const val MAX_DATA_URL = 1_150_000

    /** Arquivo temporário para a câmera gravar a foto (cache/fotos, liberado no FileProvider). */
    fun newCameraUri(context: Context): Uri {
        val dir = File(context.cacheDir, "fotos").apply { mkdirs() }
        dir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 86_400_000 }?.forEach { it.delete() }
        val file = File.createTempFile("os_", ".jpg", dir)
        return FileProvider.getUriForFile(context, "${context.packageName}.fotos", file)
    }

    /**
     * Lê a foto (câmera ou galeria), corrige a rotação, reduz e devolve "data:image/jpeg;base64,…"
     * dentro do limite do servidor.
     */
    suspend fun photoDataUrl(context: Context, uri: Uri, maxSide: Int = 1600): String = withContext(Dispatchers.IO) {
        val src = ImageDecoder.createSource(context.contentResolver, uri)
        var side = maxSide
        var bmp = decode(src, side)
        var quality = 82
        var url = toDataUrl(bmp, Bitmap.CompressFormat.JPEG, quality)
        // Grande demais: baixa a qualidade até 60 e depois o tamanho
        while (url.length > MAX_DATA_URL) {
            if (quality > 60) quality -= 10 else {
                side = (side * 0.75).roundToInt()
                check(side >= 480) { "Não consegui reduzir a foto" }
                bmp = decode(src, side); quality = 75
            }
            url = toDataUrl(bmp, Bitmap.CompressFormat.JPEG, quality)
        }
        url
    }

    private fun decode(src: ImageDecoder.Source, maxSide: Int): Bitmap =
        ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
            val w = info.size.width; val h = info.size.height
            val scale = maxSide.toFloat() / max(w, h)
            if (scale < 1f) decoder.setTargetSize((w * scale).roundToInt(), (h * scale).roundToInt())
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }

    fun toDataUrl(bmp: Bitmap, format: Bitmap.CompressFormat, quality: Int = 90): String {
        val out = ByteArrayOutputStream()
        bmp.compress(format, quality, out)
        val mime = if (format == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg"
        return "data:$mime;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    /** data URL → imagem para mostrar na tela (null se inválida). */
    fun decodeDataUrl(dataUrl: String?, maxSide: Int = 1200): ImageBitmap? {
        val b64 = dataUrl?.substringAfter("base64,", "")?.takeIf { it.isNotEmpty() } ?: return null
        return runCatching {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
        }.getOrNull()
    }
}
