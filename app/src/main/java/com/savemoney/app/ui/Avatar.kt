package com.savemoney.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.savemoney.app.R
import com.savemoney.app.ui.theme.QTheme

object AvatarIds {
    const val CAT = "mascot_cat"
    const val DOG = "mascot_dog"
    val THEME = listOf(
        "kitten",
        "corgi",
        "bunny",
        "panda",
        "duckling",
        "redpanda",
        "penguin",
        "hedgehog",
        "pup",
    )
    val ALL = listOf(CAT, DOG) + THEME

    fun known(id: String?): Boolean = id != null && id in ALL
}

sealed class AvatarView {
    data class Photo(val url: String) : AvatarView()
    data class Preset(val id: String) : AvatarView()
}

fun resolveAvatar(photoUrl: String?, presetId: String?, fallback: String): AvatarView {
    val photo = photoUrl?.trim().orEmpty()
    if (photo.isNotEmpty()) return AvatarView.Photo(photo)
    val preset = presetId?.trim().orEmpty()
    return AvatarView.Preset(if (AvatarIds.known(preset)) preset else fallback)
}

fun avatarPresetRes(id: String): Int = when (id) {
    AvatarIds.CAT -> R.drawable.mascot_cat
    AvatarIds.DOG -> R.drawable.mascot_dog
    "kitten" -> R.drawable.avatar_kitten
    "corgi" -> R.drawable.avatar_corgi
    "bunny" -> R.drawable.avatar_bunny
    "panda" -> R.drawable.avatar_panda
    "duckling" -> R.drawable.avatar_duckling
    "redpanda" -> R.drawable.avatar_redpanda
    "penguin" -> R.drawable.avatar_penguin
    "hedgehog" -> R.drawable.avatar_hedgehog
    "pup" -> R.drawable.avatar_pup
    else -> R.drawable.mascot_cat
}

@Composable
fun MemberAvatar(
    name: String,
    presetId: String?,
    photoUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    fallbackPreset: String = AvatarIds.CAT,
    squared: Boolean = false,
) {
    val colors = QTheme.colors
    val shape = if (squared) RoundedCornerShape(18.dp) else CircleShape
    val view = resolveAvatar(photoUrl, presetId, fallbackPreset)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(if (colors.isDark) colors.sandDeep else colors.skySoft)
            .border(2.dp, colors.canvas, shape),
        contentAlignment = Alignment.Center,
    ) {
        when (view) {
            is AvatarView.Photo -> AsyncImage(
                model = view.url,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
            is AvatarView.Preset -> Image(
                painter = painterResource(avatarPresetRes(view.id)),
                contentDescription = name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size).padding(4.dp),
            )
        }
    }
}

@Composable
fun StackedAvatars(
    meName: String,
    mePreset: String?,
    mePhotoUrl: String?,
    partnerName: String?,
    partnerPreset: String?,
    partnerPhotoUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val overlap = if (partnerName == null) 0.dp else size * 0.42f
    Box(modifier.size(width = size + overlap, height = size)) {
        if (partnerName != null) {
            MemberAvatar(
                name = partnerName,
                presetId = partnerPreset,
                photoUrl = partnerPhotoUrl,
                fallbackPreset = AvatarIds.DOG,
                size = size,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = overlap),
            )
        }
        MemberAvatar(
            name = meName,
            presetId = mePreset,
            photoUrl = mePhotoUrl,
            fallbackPreset = AvatarIds.CAT,
            size = size,
            modifier = Modifier.align(Alignment.CenterStart),
        )
    }
}
