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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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

/**
 * 生日当天的开屏：她站在剧院舞台中央，红幕拉开、追光亮起，再落片名。
 * 角色图是 drawable-nodpi/girl.png（白底已抠掉），其余全部 Canvas 绘制。
 */
@Composable
fun BirthdayPoster(
    name: String,
    director: String,
    season: Int?,
    year: Int,
    onDismiss: () -> Unit,
) {
    var t by remember { mutableFloatStateOf(0f) }
    val start = remember { System.nanoTime() }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { now -> t = (now - start) / 1_000_000_000f }
        }
    }
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
                onClick = onDismiss,
            ),
    ) {
        val stageH = maxHeight
        val girlH = stageH * 0.56f
        val girlW = girlH * (313f / 760f)

        // 舞台：幕布后面的空间、追光、地面
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

            // 追光
            val beam = smooth(0.5f, 1.5f, t)
            val cone = Path().apply {
                moveTo(w / 2f - w * 0.035f, h * 0.05f)
                lineTo(w / 2f + w * 0.035f, h * 0.05f)
                lineTo(w * 0.86f, floorY)
                lineTo(w * 0.14f, floorY)
                close()
            }
            drawPath(
                path = cone,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFE7B8).copy(alpha = 0.16f * beam),
                        Color(0xFFFFD79A).copy(alpha = 0.07f * beam),
                        Color(0xFFFFC880).copy(alpha = 0.03f * beam),
                    ),
                ),
            )

            // 光池
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD9A0).copy(alpha = 0.20f * beam),
                        Color(0xFFFFC880).copy(alpha = 0f),
                    ),
                    center = Offset(w / 2f, floorY),
                    radius = w * 0.5f,
                ),
                topLeft = Offset(w * 0.12f, floorY - h * 0.05f),
                size = Size(w * 0.76f, h * 0.13f),
            )

            // 地面
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

            // 金色浮尘
            drawIntoCanvas { canvas ->
                dust.forEach { d ->
                    val y = ((d[1] - t * 0.03f * d[3]) % 1f + 1f) % 1f
                    val x = w * 0.5f + (d[0] - 0.5f) * w * (0.18f + 0.4f * y)
                    val alpha = 0.10f + 0.28f * (0.5f + 0.5f * sin(t * 1.6f + d[4]))
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color(0xFFFFE0A8).copy(alpha = alpha * beam).toArgb()
                    }
                    canvas.nativeCanvas.drawCircle(x, y * floorY, d[2], paint)
                }
            }
        }

        // 她
        Image(
            painter = girl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = stageH * 0.245f)
                .height(girlH)
                .width(girlW),
        )

        // 前景：脚下的渐隐、幕布、文字
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val floorY = h * 0.845f
            val px = { v: Float -> v * h / 844f }
            val serif = Typeface.create("serif", Typeface.NORMAL)
            val serifBold = Typeface.create("serif", Typeface.BOLD)

            // 脚踩进光里
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF120B0B).copy(alpha = 0.35f),
                        Color(0xFF120B0B).copy(alpha = 0.9f),
                    ),
                    startY = floorY - h * 0.20f,
                    endY = floorY + h * 0.02f,
                ),
                topLeft = Offset(0f, floorY - h * 0.20f),
                size = Size(w, h * 0.22f),
            )

            // 幕布
            val open = smooth(0f, 1.5f, t)
            drawCurtains(open, w, h)

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

            val show = smooth(1.25f, 2.1f, t)
            text(
                "A GUAN GUAN HUA PICTURE", w / 2f, px(64f), px(8f), px(3.2f),
                PosterGold, show * 0.5f, serif, false,
            )
            text(
                "THE STORY OF YOU", w / 2f, px(96f), px(11f), px(4.6f),
                Color(0xFFE6C68C), show * 0.9f, serif, false,
            )
            val line = smooth(1.5f, 2.2f, t)
            drawRect(
                color = PosterGold.copy(alpha = 0.45f * line),
                topLeft = Offset(w / 2f - px(32f) * line, px(112f)),
                size = Size(px(64f) * line, px(1f)),
            )
            if (season != null) {
                text(
                    "SEASON $season", w / 2f, px(136f), px(9f), px(2.6f),
                    PosterCream, show * 0.6f, serif, false,
                )
            }

            // 片名：一束光扫过
            val reveal = smooth(1.55f, 2.75f, t)
            if (reveal > 0f) {
                val sweep = w * (0.19f + 0.62f * reveal)
                clipRect(right = sweep) {
                    drawIntoCanvas { canvas ->
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            textSize = px(52f)
                            typeface = serifBold
                            textAlign = Paint.Align.CENTER
                            isFakeBoldText = true
                            shader = LinearGradient(
                                0f, px(150f), 0f, px(196f),
                                intArrayOf(
                                    Color(0xFFFBF1DC).toArgb(),
                                    Color(0xFFE8D0A2).toArgb(),
                                    Color(0xFFC9A468).toArgb(),
                                ),
                                null, Shader.TileMode.CLAMP,
                            )
                        }
                        canvas.nativeCanvas.drawText(name, w / 2f, px(184f), paint)
                    }
                }
                if (t < 2.95f) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFFF0C8).copy(alpha = 0f),
                                Color(0xFFFFF0C8).copy(alpha = 0.20f),
                                Color(0xFFFFF0C8).copy(alpha = 0f),
                            ),
                            startX = sweep - px(24f),
                            endX = sweep + px(24f),
                        ),
                        topLeft = Offset(sweep - px(24f), px(146f)),
                        size = Size(px(48f), px(52f)),
                    )
                    drawRect(
                        color = Color(0xFFFFF4D6).copy(alpha = 0.30f),
                        topLeft = Offset(sweep, px(150f)),
                        size = Size(px(1f), px(44f)),
                    )
                }
            }

            text(
                "生 日 快 乐", w / 2f, px(724f), px(12f), px(2.4f),
                Color(0xFFE9D0A2), smooth(2.5f, 3.2f, t) * 0.85f, serif, false,
            )

            val foot = smooth(3.0f, 3.9f, t)
            text(
                "11.21", w / 2f, px(766f), px(10f), px(5.2f),
                PosterCream, foot * 0.55f, serif, false,
            )
            text(
                "IN THEATERS $year", w / 2f, px(786f), px(7.5f), px(2.2f),
                PosterCream, foot * 0.32f, serif, false,
            )
            text(
                "DIRECTED BY $director", w / 2f, px(812f), px(7.5f), px(2.2f),
                PosterCream, foot * 0.30f, serif, false,
            )

            val hint = smooth(4.6f, 5.3f, t) * (0.5f + 0.5f * sin(t * 2.3f))
            if (hint > 0f) {
                drawIntoCanvas { canvas ->
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = PosterCream.copy(alpha = 0.55f * hint).toArgb()
                        textSize = px(10f)
                        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.nativeCanvas.drawText("轻点一下，进来", w / 2f, px(832f), paint)
                }
            }

            // 片头黑场
            val fade = 1f - smooth(0f, 0.5f, t)
            if (fade > 0f) drawRect(color = Color.Black.copy(alpha = fade))
        }
    }
}

/** 左右两片幕布 + 顶部帷幔；open 从 0 到 1 拉开。 */
private fun DrawScope.drawCurtains(open: Float, w: Float, h: Float) {
    val panelW = w * 0.56f
    val slide = open * w * 0.5f
    val redTop = Color(0xFF5A1418)
    val redBottom = Color(0xFF210708)
    val fold = Color(0xFF120405)

    fun panel(left: Float, mirrored: Boolean) {
        val x0 = if (mirrored) w - left - panelW else left
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(redTop, Color(0xFF3A0D10), redBottom),
            ),
            topLeft = Offset(x0, 0f),
            size = Size(panelW, h),
        )
        for (i in 0..5) {
            val fx = x0 + panelW * (i / 6f) + if (mirrored) 0f else 0f
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        fold.copy(alpha = 0.55f),
                        Color.Transparent,
                        Color(0xFFFF8A70).copy(alpha = 0.05f),
                        Color.Transparent,
                    ),
                    startX = fx,
                    endX = fx + panelW / 6f,
                ),
                topLeft = Offset(fx, 0f),
                size = Size(panelW / 6f, h),
            )
        }
    }

    panel(-slide, false)
    panel(-slide, true)

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
