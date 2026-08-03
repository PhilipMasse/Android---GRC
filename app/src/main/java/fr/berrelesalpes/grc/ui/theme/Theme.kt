package fr.berrelesalpes.grc.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Bleu,
    onPrimary = Blanc,
    secondary = Vert,
    onSecondary = Blanc,
    tertiary = Or,
    onTertiary = GrisTexte,
    background = Blanc,
    onBackground = GrisTexte,
    surface = Blanc,
    onSurface = GrisTexte,
    surfaceVariant = GrisClair,
    error = Rouge,
    onError = Blanc,
)

private val DarkColors = darkColorScheme(
    primary = Bleu,
    onPrimary = Blanc,
    secondary = Vert,
    onSecondary = Blanc,
    tertiary = Or,
    onTertiary = GrisTexte,
    background = BleuFonce,
    onBackground = Blanc,
    surface = BleuFonce,
    onSurface = Blanc,
    error = Rouge,
    onError = Blanc,
)

private val GrcTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
)

@Composable
fun GrcTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GrcTypography,
        content = content
    )
}
