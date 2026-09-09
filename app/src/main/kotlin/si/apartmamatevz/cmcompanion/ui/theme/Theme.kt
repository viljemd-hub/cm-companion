package si.apartmamatevz.cmcompanion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The actual "CM dark navy, official, calm, self-identifying" palette
 * (2026-09-17), pulled directly from the existing guest-facing identity
 * (public/confirm_reservation.php, public/cancel_reservation.php,
 * common/lib/email.php in pro-dev-repo) rather than invented fresh -
 * Companion should look like part of the same product family, not a
 * separately-designed app that happens to share a name.
 */
object CmColors {
    val BackgroundDeep = Color(0xFF050814)
    val Background = Color(0xFF0B1220)
    val Surface = Color(0xFF0F1B2D)
    val Border = Color(0xFF33465F)
    val BorderSubtle = Color(0xFF263A55)

    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFFE5EDF7)
    val TextMuted = Color(0xFFCBD5E1)
    val TextFaint = Color(0xFF9CA3AF)

    val Primary = Color(0xFF2563EB)
    val PrimaryPressed = Color(0xFF1D4ED8)
    val Accent = Color(0xFF38BDF8)

    val Danger = Color(0xFFDC2626)
    val DangerPressed = Color(0xFFB91C1C)
    val DangerContainer = Color(0xFFFECACA)
}

@Composable
fun CmCompanionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = CmColors.Background,
            onBackground = CmColors.TextPrimary,
            surface = CmColors.Surface,
            onSurface = CmColors.TextPrimary,
            surfaceVariant = CmColors.Surface,
            onSurfaceVariant = CmColors.TextMuted,
            outline = CmColors.Border,
            outlineVariant = CmColors.BorderSubtle,
            primary = CmColors.Primary,
            onPrimary = Color.White,
            primaryContainer = CmColors.PrimaryPressed,
            onPrimaryContainer = CmColors.TextPrimary,
            secondary = CmColors.Accent,
            onSecondary = CmColors.BackgroundDeep,
            error = CmColors.Danger,
            onError = Color.White,
            errorContainer = CmColors.DangerContainer,
            onErrorContainer = CmColors.DangerPressed,
        ),
        content = content,
    )
}
