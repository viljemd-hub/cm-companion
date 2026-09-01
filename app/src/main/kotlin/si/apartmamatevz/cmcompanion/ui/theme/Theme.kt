package si.apartmamatevz.cmcompanion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Placeholder dark theme only - matches the CM dark-navy guest-facing
 * identity used elsewhere in the ecosystem (see pro_dev_roadmap memory,
 * "CM dark navy, official, calm, self-identifying"). Real palette/typography
 * pass is future work, not v0.1 scope.
 */
@Composable
fun CmCompanionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(),
        content = content,
    )
}
