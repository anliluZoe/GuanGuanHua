package com.savemoney.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.savemoney.app.ui.theme.Cute

enum class MascotKind { Coin, Piggy, Wallet }

@Composable
fun Mascot(kind: MascotKind, modifier: Modifier = Modifier, size: Dp = 160.dp) {
    Canvas(modifier = modifier.size(size)) {
        when (kind) {
            MascotKind.Coin -> drawCoin()
            MascotKind.Piggy -> drawPiggy()
            MascotKind.Wallet -> drawWallet()
        }
    }
}

private fun DrawScope.drawCoin() {
    val cx = size.width / 2f
    val cy = size.height / 2f + size.height * 0.04f
    val r = size.minDimension * 0.34f
    drawCircle(Color(0xFFFFE08A), radius = r + size.minDimension * 0.05f, center = Offset(cx, cy + 6f))
    drawCircle(Cute.Gold, radius = r, center = Offset(cx, cy))
    drawCircle(Color(0xFFFFF6C8), radius = r * 0.78f, center = Offset(cx, cy))
    drawCircle(Cute.Gold, radius = r * 0.62f, center = Offset(cx, cy), style = Stroke(width = r * 0.08f))
    drawSleepyFace(Offset(cx, cy - r * 0.04f), r * 0.72f)
    drawCircle(Color.White.copy(alpha = 0.55f), radius = r * 0.12f, center = Offset(cx - r * 0.38f, cy - r * 0.38f))
}

private fun DrawScope.drawPiggy() {
    val w = size.width
    val h = size.height
    val body = Rect(w * 0.18f, h * 0.34f, w * 0.82f, h * 0.78f)
    val pink = Color(0xFFFFB3C4)
    val dark = Color(0xFFE889A0)
    drawRoundRect(pink, Offset(body.left, body.top), Size(body.width, body.height), CornerRadius(body.height / 2f))
    val ear = Path().apply {
        moveTo(w * 0.28f, h * 0.40f)
        quadraticTo(w * 0.22f, h * 0.18f, w * 0.40f, h * 0.34f)
        close()
    }
    val earR = Path().apply {
        moveTo(w * 0.72f, h * 0.40f)
        quadraticTo(w * 0.78f, h * 0.18f, w * 0.60f, h * 0.34f)
        close()
    }
    drawPath(ear, pink)
    drawPath(earR, pink)
    drawRoundRect(dark, Offset(w * 0.44f, h * 0.30f), Size(w * 0.12f, h * 0.05f), CornerRadius(8f))
    drawCircle(pink, radius = w * 0.09f, center = Offset(w * 0.82f, h * 0.56f))
    drawCircle(dark, radius = w * 0.025f, center = Offset(w * 0.86f, h * 0.56f))
    drawRoundRect(Color(0xFFFFC9D4), Offset(w * 0.40f, h * 0.52f), Size(w * 0.20f, h * 0.12f), CornerRadius(20f))
    drawCircle(dark, radius = w * 0.016f, center = Offset(w * 0.46f, h * 0.58f))
    drawCircle(dark, radius = w * 0.016f, center = Offset(w * 0.54f, h * 0.58f))
    drawSleepyFace(Offset(w * 0.50f, h * 0.50f), w * 0.22f, blush = false)
    drawRoundRect(Color(0xFFFF9A6B), Offset(w * 0.30f, h * 0.76f), Size(w * 0.10f, h * 0.08f), CornerRadius(10f))
    drawRoundRect(Color(0xFFFF9A6B), Offset(w * 0.60f, h * 0.76f), Size(w * 0.10f, h * 0.08f), CornerRadius(10f))
}

private fun DrawScope.drawWallet() {
    val w = size.width
    val h = size.height
    drawRoundRect(Cute.Sky, Offset(w * 0.18f, h * 0.32f), Size(w * 0.64f, h * 0.42f), CornerRadius(36f))
    drawRoundRect(Color(0xFF5EC4E0), Offset(w * 0.18f, h * 0.32f), Size(w * 0.64f, h * 0.16f), CornerRadius(36f))
    drawRoundRect(Cute.Gold, Offset(w * 0.28f, h * 0.22f), Size(w * 0.44f, h * 0.16f), CornerRadius(16f))
    drawRoundRect(Color(0xFFFFF6C8), Offset(w * 0.32f, h * 0.25f), Size(w * 0.36f, h * 0.10f), CornerRadius(10f))
    drawCircle(Color.White, radius = w * 0.045f, center = Offset(w * 0.70f, h * 0.58f))
    drawCircle(Cute.Peach, radius = w * 0.022f, center = Offset(w * 0.70f, h * 0.58f))
    drawSleepyFace(Offset(w * 0.44f, h * 0.58f), w * 0.18f)
}

private fun DrawScope.drawSleepyFace(center: Offset, faceR: Float, blush: Boolean = true) {
    val stroke = Stroke(width = faceR * 0.10f, cap = StrokeCap.Round)
    val eyeY = center.y - faceR * 0.08f
    drawArc(
        color = Cute.Ink,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(center.x - faceR * 0.46f, eyeY - faceR * 0.22f),
        size = Size(faceR * 0.28f, faceR * 0.28f),
        style = stroke,
    )
    drawArc(
        color = Cute.Ink,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(center.x + faceR * 0.18f, eyeY - faceR * 0.22f),
        size = Size(faceR * 0.28f, faceR * 0.28f),
        style = stroke,
    )
    drawArc(
        color = Cute.Ink,
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(center.x - faceR * 0.18f, center.y + faceR * 0.02f),
        size = Size(faceR * 0.36f, faceR * 0.28f),
        style = stroke,
    )
    if (blush) {
        drawOval(Cute.Blush.copy(alpha = 0.7f), Offset(center.x - faceR * 0.62f, center.y + faceR * 0.06f), Size(faceR * 0.22f, faceR * 0.12f))
        drawOval(Cute.Blush.copy(alpha = 0.7f), Offset(center.x + faceR * 0.40f, center.y + faceR * 0.06f), Size(faceR * 0.22f, faceR * 0.12f))
    }
}
