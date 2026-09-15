package com.savemoney.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.savemoney.app.R

enum class MascotKind { Cat, Dog }

@Composable
fun Mascot(kind: MascotKind, modifier: Modifier = Modifier, size: Dp = 88.dp) {
    Image(
        painter = painterResource(
            when (kind) {
                MascotKind.Cat -> R.drawable.mascot_cat
                MascotKind.Dog -> R.drawable.mascot_dog
            }
        ),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size),
    )
}
