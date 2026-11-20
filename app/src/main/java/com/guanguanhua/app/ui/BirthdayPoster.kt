package com.guanguanhua.app.ui

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.guanguanhua.app.R
import kotlin.math.sin
import kotlin.random.Random

private val PosterGold = Color(0xFFD9B678)
private val PosterCream = Color(0xFFF4ECDD)

private fun smooth(a: Float, b: Float, x: Float): Float {
    val k = ((x - a) / (b - a)).coerceIn(0f, 1f)
    return k * k * (3 - 2 * k)
}

private fun easeOut(x: Float): Float = 1f - (1f - x) * (1f - x) * (1f - x)

/** 幕布拉开的位置：冲过头一点、回一点、再停稳。 */
private fun curtainProgress(p: Float): Float {
    if (p <= 0f) return 0f
    if (p < .72f) return 1.07f * easeOut(p / .72f)
    if (p < .88f) return 1.07f + (0.967f - 1.07f) * ((p - .72f) / .16f)
    return 0.967f + (1f - 0.967f) * ((p - .88f) / .12f).coerceIn(0f, 1f)
}

/**
 * 生日首屏：剧院舞台。先看到合着的红幕，点一下透光、布幔式拉开，
 * 她站在追光里，最后落「HAPPY BIRTHDAY / 妞妞」。
 */
@Composable
fun BirthdayPoster(onDismiss: () -> Unit) {
    var now by remember { mutableLongStateOf(System.nanoTime()) }
    var opened by remember { mutableStateOf(false) }
    var openedAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { now = it }
        }
    }
    val t = if (opened) (now - openedAt) / 1_000_000_000f else 0f
    val open = curtainProgress(smooth(0f, 1.6f, t))
    val girlAlpha = if (opened) smooth(0.45f, 1.25f, t) else 0f

    val dust = remember {
        val random = Random(1121)
        List(26) {
            floatArrayOf(
                random.nextFloat(),
                random.nextFloat(),
                0.5f + random.nextFloat() * 1.2f,
                0.3f + random.nextFloat() * 0.6f,
                random.nextFloat() * 6.28f,
            )
        }
    }
    val girl = painterResource(R.drawable.girl)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070506))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (!opened) {
                    opened = true
                    openedAt = now
                } else if (t > 2.3f) {
                    onDismiss()
                }
            },
    ) {
        val stageH = maxHeight
        val girlH = stageH * 0.46f
        val girlW = girlH * (328f / 760f)

        // 舞台：追光、地面、两侧暖光、星星
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val floorY = h * 0.845f
            val px = { v: Float -> v * h / 844f }

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2A1A14), Color(0xFF120B0B), Color(0xFF070506)),
                    center = Offset(w / 2f, h * 0.42f),
                    radius = h * 0.7f,
                ),
                size = Size(w, h),
            )

            val beam = 0.35f + 0.65f * smooth(0.3f, 1.5f, t)
            val cone = Path().apply {
                moveTo(w / 2f - w * 0.035f, h * 0.05f)
                lineTo(w / 2f + w * 0.035f, h * 0.05f)
                lineTo(w / 2f + w * 0.36f, floorY)
                lineTo(w / 2f - w * 0.36f, floorY)
                close()
            }
            drawPath(
                path = cone,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFE7B8).copy(alpha = 0.15f * beam),
                        Color(0xFFFFD79A).copy(alpha = 0.07f * beam),
                        Color(0xFFFFC880).copy(alpha = 0.02f * beam),
                    ),
                ),
            )
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD9A0).copy(alpha = 0.22f * beam),
                        Color(0xFFFFC880).copy(alpha = 0f),
                    ),
                    center = Offset(w / 2f, floorY),
                    radius = w * 0.55f,
                ),
                topLeft = Offset(w * 0.06f, floorY - h * 0.07f),
                size = Size(w * 0.88f, h * 0.16f),
            )

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1110), Color(0xFF0A0707)),
                    startY = floorY,
                    endY = h,
                ),
                topLeft = Offset(0f, floorY),
                size = Size(w, h - floorY),
            )
            drawRect(
                color = Color(0xFFFFD9A0).copy(alpha = 0.10f * beam),
                topLeft = Offset(0f, floorY),
                size = Size(w, px(1f)),
            )

            // 两侧暖光
            listOf(0.06f, 0.94f).forEach { fx ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFB86A).copy(alpha = 0.13f * beam),
                            Color(0xFFFFB86A).copy(alpha = 0f),
                        ),
                        center = Offset(w * fx, h * 0.44f),
                        radius = h * 0.27f,
                    ),
                    radius = h * 0.27f,
                    center = Offset(w * fx, h * 0.44f),
                )
            }

            // 星星
            listOf(
                floatArrayOf(0.10f, 0.30f, 0.030f),
                floatArrayOf(0.91f, 0.33f, 0.024f),
                floatArrayOf(0.07f, 0.55f, 0.020f),
                floatArrayOf(0.93f, 0.52f, 0.018f),
            ).forEachIndexed { index, s ->
                val tw = 0.55f + 0.45f * sin(t * 1.7f + index)
                drawStar(
                    cx = w * s[0],
                    cy = h * s[1],
                    r = w * s[2],
                    color = Color(0xFFFFE0A8).copy(alpha = 0.32f * tw * beam),
                )
            }

            // 金色浮尘
            drawIntoCanvas { canvas ->
                dust.forEach { d ->
                    val y = ((d[1] + t * 0.02f * d[3]) % 1f)
                    val x = w * 0.5f + (d[0] - 0.5f) * w * (0.16f + 0.34f * y)
                    val alpha = 0.10f + 0.26f * (0.5f + 0.5f * sin(t * 1.6f + d[4]))
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color(0xFFFFE0A8).copy(alpha = alpha * beam).toArgb()
                    }
                    canvas.nativeCanvas.drawCircle(x, y * floorY, d[2], paint)
                }
            }
        }

        // 她（点开之后才慢慢显出来）
        Image(
            painter = girl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = stageH * 0.385f)
                .height(girlH)
                .width(girlW)
                .alpha(girlAlpha),
        )

        // 前景：脚下的渐隐、礼物、幕布、光溢出、文字
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val floorY = h * 0.845f
            val px = { v: Float -> v * h / 844f }
            val serif = Typeface.create("serif", Typeface.NORMAL)

            // 她脚下融进光里
            if (girlAlpha > 0f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF120B0B).copy(alpha = 0.30f * girlAlpha),
                            Color(0xFF120B0B).copy(alpha = 0.85f * girlAlpha),
                        ),
                        startY = floorY - h * 0.20f,
                        endY = floorY + h * 0.01f,
                    ),
                    topLeft = Offset(0f, floorY - h * 0.20f),
                    size = Size(w, h * 0.21f),
                )
            }

            // 礼物
            if (girlAlpha > 0f) {
                drawGift(w * 0.10f, floorY - h * 0.045f, w * 0.055f, h * 0.040f, girlAlpha)
                drawGift(w * 0.90f, floorY - h * 0.038f, w * 0.048f, h * 0.035f, girlAlpha)
            }

            // 光溢出（先亮，再让幕布退开）
            val spill = smooth(0f, 1.1f, t)
            if (spill > 0f) {
                val sw = w * 0.10f + w * 0.62f * spill
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFE6B8).copy(alpha = 0.30f * (1f - spill) * 0.6f + 0.10f * (1f - open)),
                            Color(0xFFFFD79A).copy(alpha = 0f),
                        ),
                        center = Offset(w / 2f, h * 0.46f),
                        radius = sw * 0.6f,
                    ),
                    radius = sw * 0.6f,
                    center = Offset(w / 2f, h * 0.46f),
                )
            }

            // 幕布（布幔式：带一点波浪内缘和过冲）
            drawCurtains(open, w, h)

            // 片头文字
            val text = { value: String, x: Float, y: Float, sizePx: Float, letter: Float, tint: Color, alpha: Float, typeface: Typeface, bold: Boolean ->
                drawIntoCanvas { canvas ->
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = tint.copy(alpha = alpha).toArgb()
                        textSize = sizePx
                        this.typeface = typeface
                        textAlign = Paint.Align.LEFT
                        isFakeBoldText = bold
                    }
                    paint.letterSpacing = if (sizePx > 0f) letter / sizePx else 0f
                    val total = paint.measureText(value)
                    canvas.nativeCanvas.drawText(value, x - total / 2f, y, paint)
                }
            }

            val show = smooth(1.15f, 1.9f, t)
            text(
                "A GUAN GUAN HUA PICTURE", w / 2f, px(56f), px(8f), px(3.2f),
                PosterGold, show * 0.5f, serif, false,
            )
            text(
                "THE STORY OF YOU", w / 2f, px(96f), px(11f), px(4.6f),
                Color(0xFFE6C68C), show * 0.9f, serif, false,
            )
            val line = smooth(1.35f, 2.0f, t)
            drawRect(
                color = PosterGold.copy(alpha = 0.45f * line),
                topLeft = Offset(w / 2f - px(32f) * line, px(112f)),
                size = Size(px(64f) * line, px(1f)),
            )

            // HAPPY / BIRTHDAY
            val title = smooth(1.45f, 2.2f, t)
            text("HAPPY", w / 2f, px(178f), px(40f), px(7f), PosterCream, title, serif, false)
            text("BIRTHDAY", w / 2f, px(222f), px(40f), px(7f), PosterCream, title, serif, false)

            // 妞妞
            val name = smooth(1.85f, 2.45f, t)
            text("妞妞", w / 2f, px(736f), px(26f), px(6f), PosterGold, name, serif, false)

            val foot = smooth(2.0f, 2.7f, t)
            text("11.21", w / 2f, px(764f), px(10f), px(5.2f), PosterCream, foot * 0.55f, serif, false)
            text("IN THEATERS 2026", w / 2f, px(786f), px(7.5f), px(2.2f), PosterCream, foot * 0.32f, serif, false)
            text("DIRECTED BY 阿哲", w / 2f, px(806f), px(7.5f), px(2.2f), PosterCream, foot * 0.30f, serif, false)

            // 合幕时的提示
            if (!opened) {
                val pulse = 0.55f + 0.45f * sin(now / 1_000_000_000f * 2.4f)
                text(
                    "轻点一下，帷幕拉开", w / 2f, px(700f), px(11f), px(3.2f),
                    PosterCream, 0.85f * pulse, serif, false,
                )
                // 中缝的一道光
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFE0A8).copy(alpha = 0f),
                            Color(0xFFFFE0A8).copy(alpha = 0.35f * pulse),
                            Color(0xFFFFE0A8).copy(alpha = 0f),
                        ),
                        startX = w / 2f - px(8f),
                        endX = w / 2f + px(8f),
                    ),
                    topLeft = Offset(w / 2f - px(8f), 0f),
                    size = Size(px(16f), h),
                )
            }

            // 开演之后的提示
            if (opened) {
                val hint = smooth(2.55f, 3.2f, t) * (0.5f + 0.5f * sin(t * 2.3f))
                if (hint > 0f) {
                    text(
                        "轻点一下，进来", w / 2f, px(832f), px(10f), px(0f),
                        PosterCream, 0.55f * hint, serif, false,
                    )
                }
            }
        }
    }
}

/** 左右两片幕布 + 顶部帷幔，内缘带细波浪。open 0..1。 */
private fun DrawScope.drawCurtains(open: Float, w: Float, h: Float) {
    val panelW = w * 0.52f
    val travel = w * 0.90f * open
    val redTop = Color(0xFF5A1418)
    val redMid = Color(0xFF3A0D10)
    val redBottom = Color(0xFF210708)

    // 从中间往两边拉开：左片向左，右片向右
    drawPanel(left = -travel, panelW = panelW, h = h, top = redTop, mid = redMid, bottom = redBottom)
    scale(scaleX = -1f, scaleY = 1f, pivot = Offset.Zero) {
        drawPanel(left = -w - travel, panelW = panelW, h = h, top = redTop, mid = redMid, bottom = redBottom)
    }

    // 顶部帷幔
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF6B1A1E), Color(0xFF3A0D10), Color(0xFF1A0506)),
        ),
        topLeft = Offset(0f, 0f),
        size = Size(w, h * 0.075f),
    )
    for (i in 0..8) {
        val cx = w * (i + 0.5f) / 9f
        drawCircle(
            color = Color(0xFF4A1014).copy(alpha = 0.55f),
            radius = w * 0.045f,
            center = Offset(cx, h * 0.075f),
        )
    }
}

private fun DrawScope.drawPanel(left: Float, panelW: Float, h: Float, top: Color, mid: Color, bottom: Color) {
    val path = Path().apply {
        moveTo(left, 0f)
        val steps = 16
        for (i in 0..steps) {
            val y = h * i / steps
            val edge = left + panelW + sin(y / h * 6.28f * 2.2f) * panelW * 0.018f
            lineTo(edge, y)
        }
        lineTo(left, h)
        close()
    }
    drawPath(
        path = path,
        brush = Brush.verticalGradient(colors = listOf(top, mid, bottom)),
    )
    // 褶皱
    for (i in 0..5) {
        val fx = left + panelW * (i / 6f)
        drawLine(
            color = Color(0xFF120405).copy(alpha = 0.45f),
            start = Offset(fx, 0f),
            end = Offset(fx, h),
            strokeWidth = panelW * 0.012f,
        )
        drawLine(
            color = Color(0xFFFF8A70).copy(alpha = 0.05f),
            start = Offset(fx + panelW * 0.02f, 0f),
            end = Offset(fx + panelW * 0.02f, h),
            strokeWidth = panelW * 0.010f,
        )
    }
}

private fun DrawScope.drawStar(cx: Float, cy: Float, r: Float, color: Color) {
    val path = Path().apply {
        moveTo(cx, cy - r)
        quadraticBezierTo(cx + r * 0.16f, cy - r * 0.16f, cx + r, cy)
        quadraticBezierTo(cx + r * 0.16f, cy + r * 0.16f, cx, cy + r)
        quadraticBezierTo(cx - r * 0.16f, cy + r * 0.16f, cx - r, cy)
        quadraticBezierTo(cx - r * 0.16f, cy - r * 0.16f, cx, cy - r)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawGift(cx: Float, baseY: Float, w: Float, h: Float, alpha: Float) {
    val left = cx - w / 2f
    val top = baseY - h
    drawRect(
        color = Color(0xFF6E1A20).copy(alpha = alpha),
        topLeft = Offset(left, top),
        size = Size(w, h),
    )
    drawRect(
        color = Color(0xFF2A0709).copy(alpha = alpha),
        topLeft = Offset(left, top),
        size = Size(w, h * 0.22f),
    )
    drawRect(
        color = Color(0xFFD9B678).copy(alpha = 0.65f * alpha),
        topLeft = Offset(cx - w * 0.06f, top),
        size = Size(w * 0.12f, h),
    )
    drawRect(
        color = Color(0xFFD9B678).copy(alpha = 0.40f * alpha),
        topLeft = Offset(left, top + h * 0.12f),
        size = Size(w, h * 0.07f),
    )
}
