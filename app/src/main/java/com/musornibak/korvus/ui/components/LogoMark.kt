package com.musornibak.korvus.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musornibak.korvus.R

@Composable
fun LogoMark(
    size: Dp = 128.dp,
    animated: Boolean = true
) {
    if (!animated) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = "MiaMuy",
            modifier = Modifier.size(size)
        )
        return
    }
    val transition = rememberInfiniteTransition(label = "logo")
    val scale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val tilt by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tilt"
    )
    Box(contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = "MiaMuy",
            modifier = Modifier
                .size(size)
                .scale(scale)
                .graphicsLayer(rotationZ = tilt)
        )
    }
}
