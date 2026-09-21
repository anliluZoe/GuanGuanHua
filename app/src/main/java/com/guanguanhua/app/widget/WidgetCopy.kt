package com.guanguanhua.app.widget

object WidgetCopy {
    const val EMPTY_PHOTO = "还没有照片"
    const val BRAND = "管管花"
    const val MAX_CAPTION = 40
    const val MAX_IMAGE_PX = 720

    fun clampCaption(value: String): String = value.trim().take(MAX_CAPTION)

    fun bitmapSampleSize(width: Int, height: Int, maxPx: Int = MAX_IMAGE_PX): Int {
        if (width <= 0 || height <= 0 || maxPx <= 0) return 1
        var sample = 1
        while (width / sample > maxPx || height / sample > maxPx) sample *= 2
        return sample
    }
}
