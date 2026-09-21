package com.guanguanhua.app.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.guanguanhua.app.MainActivity
import com.guanguanhua.app.R

class HouseholdWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = WidgetCache.read(context)
        val cover = WidgetCache.decodeCover(context)
        provideContent {
            WidgetCard(context, state, cover)
        }
    }
}

@Composable
private fun WidgetCard(context: Context, state: WidgetState, cover: Bitmap?) {
    val open = Intent(context, MainActivity::class.java)
        .setAction(MainActivity.ACTION_OPEN_WIDGET)
        .putExtra(MainActivity.EXTRA_OPEN_WIDGET, true)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    val caption = WidgetCopy.clampCaption(state.caption)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(20.dp)
            .background(ColorProvider(Color.White))
            .clickable(actionStartActivity(open)),
    ) {
        if (cover != null && !state.localImagePath.isNullOrBlank()) {
            Image(
                provider = ImageProvider(cover),
                contentDescription = caption.ifBlank { context.getString(R.string.widget_name) },
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize().cornerRadius(20.dp),
            )
        } else {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFFD7EAF3))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = WidgetCopy.EMPTY_PHOTO,
                    style = TextStyle(color = ColorProvider(Color(0xFF5C6673)), fontSize = 14.sp),
                )
            }
        }
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ColorProvider(Color(0xCC1C212B)))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                if (caption.isNotBlank()) {
                    Text(
                        text = caption,
                        maxLines = 2,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
                Text(
                    text = WidgetCopy.BRAND,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFF07A5C)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}
