package si.apartmamatevz.cmcompanion.ui.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import si.apartmamatevz.cmcompanion.data.InstallationConnection
import si.apartmamatevz.cmcompanion.ui.theme.CmCard
import si.apartmamatevz.cmcompanion.ui.theme.CmColors

/**
 * Multi-installation management (2026-09-17): one owner's own paired
 * installations, listed with enough detail to tell them apart, plus
 * "Add another" and a per-connection "Forget this device". Each row shows
 * its own installation_id deliberately prominently - this is the seam a
 * future cross-installation Alerts feed and the Plus-tier "quick
 * availability check" both hang off of, and per the user's own framing,
 * the same identification shape a future CM Community query (across OTHER
 * owners' installations, not just these) would need to slot into without
 * a rewrite. Not implying Community exists yet - it doesn't - only that
 * this screen doesn't paint itself into a single-installation corner.
 */
@Composable
fun ConnectionScreen(
    connections: List<InstallationConnection>,
    onAddAnother: () -> Unit,
    onForget: (InstallationConnection) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        connections.forEach { connection ->
            ConnectionRow(connection, onForget = { onForget(connection) })
        }

        Button(onClick = onAddAnother, modifier = Modifier.fillMaxWidth()) {
            Text("Add another installation")
        }
    }
}

@Composable
private fun ConnectionRow(connection: InstallationConnection, onForget: () -> Unit) {
    CmCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                connection.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CmColors.TextPrimary,
            )
            Text("Installation ID: ${connection.installationId}", color = CmColors.TextMuted)
            Text("Bridge URL: ${connection.baseUrl}", color = CmColors.TextMuted)
            Text("Status: ${connection.status}", color = CmColors.Accent)
            Text("Scopes: ${connection.scopes.joinToString(", ")}", color = CmColors.TextFaint)
            OutlinedButton(onClick = onForget, modifier = Modifier.fillMaxWidth()) {
                Text("Forget this device", color = CmColors.Danger)
            }
        }
    }
}
