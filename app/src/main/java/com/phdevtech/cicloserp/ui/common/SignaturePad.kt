package com.phdevtech.cicloserp.ui.common

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.phdevtech.cicloserp.util.Images

/** Traços da assinatura; vira PNG (data URL) para o servidor. */
@Stable
class SignatureState {
    internal val strokes = mutableStateListOf<List<Offset>>()
    internal val current = mutableStateListOf<Offset>()
    internal val size = mutableStateOf(IntSize.Zero)

    val isEmpty get() = strokes.isEmpty() && current.isEmpty()

    fun clear() { strokes.clear(); current.clear() }

    /** PNG com fundo branco (null se vazia). */
    fun toDataUrl(): String? {
        val s = size.value
        if (isEmpty || s.width == 0 || s.height == 0) return null
        val bmp = createBitmap(s.width, s.height)
        val c = Canvas(bmp)
        c.drawColor(android.graphics.Color.WHITE)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = s.width / 160f + 2f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        strokes.forEach { pts ->
            val path = android.graphics.Path()
            pts.firstOrNull()?.let { path.moveTo(it.x, it.y) }
            pts.drop(1).forEach { path.lineTo(it.x, it.y) }
            if (pts.size == 1) c.drawPoint(pts[0].x, pts[0].y, p) else c.drawPath(path, p)
        }
        return Images.toDataUrl(bmp, Bitmap.CompressFormat.PNG)
    }
}

@Composable
fun rememberSignatureState() = remember { SignatureState() }

@Composable
fun SignaturePad(state: SignatureState, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val shape = RoundedCornerShape(12.dp)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, Color.Gray, shape)
            .onSizeChanged { state.size.value = it }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { state.current.clear(); state.current.add(it) },
                    onDrag = { change, _ -> change.consume(); state.current.add(change.position) },
                    onDragEnd = { state.strokes.add(state.current.toList()); state.current.clear() },
                    onDragCancel = { state.strokes.add(state.current.toList()); state.current.clear() },
                )
            },
    ) {
        val stroke = Stroke(width = size.width / 160f + 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        (state.strokes + listOf(state.current.toList())).forEach { pts ->
            if (pts.isEmpty()) return@forEach
            val path = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                pts.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, Color.Black, style = stroke)
        }
    }
}
