package com.guanguanhua.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guanguanhua.app.ui.theme.QTheme

object HouseholdCodeCopy {
    const val SNACKBAR = "已复制家庭码"

    fun clipText(code: String): String? = code.trim().takeIf { it.isNotEmpty() }
}

@Composable
fun CopyableHouseholdCode(code: String, onCopied: () -> Unit) {
    val copyText = HouseholdCodeCopy.clipText(code)
    val clipboard = LocalClipboardManager.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = copyText != null) {
                val value = copyText ?: return@clickable
                clipboard.setText(AnnotatedString(value))
                onCopied()
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = copyText ?: "还未加入",
            style = MaterialTheme.typography.displaySmall.copy(
                fontFeatureSettings = "tnum",
                letterSpacing = 6.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = QTheme.colors.sky,
        )
        if (copyText != null) {
            Spacer(Modifier.width(10.dp))
            Icon(
                Icons.Outlined.ContentCopy,
                contentDescription = "复制家庭码",
                tint = QTheme.colors.sky,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
