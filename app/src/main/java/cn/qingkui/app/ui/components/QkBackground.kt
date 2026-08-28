package cn.qingkui.app.ui.components

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
fun QkSvgAsset(
    @RawRes resourceId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.FillBounds,
) {
    AsyncImage(
        model = resourceId,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}
