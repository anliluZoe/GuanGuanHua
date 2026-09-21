package com.guanguanhua.app.widget

object WidgetCopy {
    const val EMPTY_PHOTO = "还没有照片"
    const val EMPTY_PHOTO_HINT = "上传一张放上桌面吧"
    const val COMPRESS_HINT = "上传会压缩为 JPG"
    const val MAX_CAPTION = 40
    const val MAX_IMAGE_PX = 720
    const val DEFAULT_CAPTION_COLOR = "#FFFFFF"
    val CAPTION_COLOR_PRESETS = listOf(
        "#FFFFFF",
        "#000000",
        "#F07A5C",
        "#7EB8D8",
        "#5ECFB8",
        "#B89AD8",
    )

    fun clampCaption(value: String): String = value.trim().take(MAX_CAPTION)

    fun summaryLine(hasPhoto: Boolean, caption: String): String {
        val text = clampCaption(caption)
        return when {
            !hasPhoto && text.isEmpty() -> "未设置"
            text.isEmpty() -> "已设置"
            else -> "已设置 · $text"
        }
    }

    fun normalizeCaptionColor(value: String?): String =
        parseCaptionColorRgb(value)?.let { rgb -> "#%06X".format(rgb) } ?: DEFAULT_CAPTION_COLOR

    fun captionColorArgb(value: String?): Int =
        0xFF000000.toInt() or (parseCaptionColorRgb(value) ?: parseCaptionColorRgb(DEFAULT_CAPTION_COLOR)!!)

    fun bitmapSampleSize(width: Int, height: Int, maxPx: Int = MAX_IMAGE_PX): Int {
        if (width <= 0 || height <= 0 || maxPx <= 0) return 1
        var sample = 1
        while (width / sample > maxPx || height / sample > maxPx) sample *= 2
        return sample
    }

    fun parseCaptionColorRgb(value: String?): Int? {
        if (value.isNullOrBlank()) return null
        val hex = value.trim().removePrefix("#")
        if (hex.any { it !in "0123456789abcdefABCDEF" }) return null
        return when (hex.length) {
            3 -> {
                val r = hex[0].digitToInt(16)
                val g = hex[1].digitToInt(16)
                val b = hex[2].digitToInt(16)
                (r shl 20) or (r shl 16) or (g shl 12) or (g shl 8) or (b shl 4) or b
            }
            6 -> hex.toIntOrNull(16)
            else -> null
        }
    }
}
