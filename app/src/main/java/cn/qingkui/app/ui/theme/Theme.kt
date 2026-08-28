package cn.qingkui.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = QingkuiGreen,
    onPrimary = Color.White,
    primaryContainer = QingkuiGreenSoft,
    onPrimaryContainer = QingkuiInk,
    secondary = QingkuiOrange,
    background = QingkuiBackground,
    onBackground = QingkuiInk,
    surface = QingkuiSurface,
    onSurface = QingkuiInk,
    onSurfaceVariant = QingkuiSecondary,
    outline = QingkuiDivider,
    error = QingkuiError,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80D6B8),
    onPrimary = Color(0xFF07372A),
    primaryContainer = Color(0xFF174D3E),
    onPrimaryContainer = Color(0xFFC8F7E5),
    secondary = Color(0xFFFFB86C),
    background = QingkuiDarkBackground,
    onBackground = QingkuiDarkInk,
    surface = QingkuiDarkSurfaceVariant,
    onSurface = QingkuiDarkInk,
    onSurfaceVariant = QingkuiDarkSecondary,
    outline = Color(0xFF3E4B44),
    error = Color(0xFFFFB4AB),
)

@Composable
fun QingkuiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = QingkuiTypography,
        content = content,
    )
}
