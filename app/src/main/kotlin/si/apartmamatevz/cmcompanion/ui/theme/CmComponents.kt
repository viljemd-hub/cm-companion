package si.apartmamatevz.cmcompanion.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Shared card shell (2026-09-17 visual pass) - every screen's sections
 * (Today's Attention/Currently hosting/Arrivals/Departures, Alerts rows,
 * Inquiries rows, Connection rows) wrap in this instead of bare Text
 * blocks, so the "acceptably nice for a public Journal post" bar applies
 * consistently, not screen-by-screen.
 */
@Composable
fun CmCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CmColors.Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CmColors.BorderSubtle),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
