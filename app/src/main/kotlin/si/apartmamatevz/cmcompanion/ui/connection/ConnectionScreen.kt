package si.apartmamatevz.cmcompanion.ui.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import si.apartmamatevz.cmcompanion.data.InstallationConnection

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
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(connection.displayName, fontWeight = FontWeight.Bold)
        Text("Installation ID: ${connection.installationId}")
        Text("Bridge URL: ${connection.baseUrl}")
        Text("Status: ${connection.status}")
        Text("Scopes: ${connection.scopes.joinToString(", ")}")
        OutlinedButton(onClick = onForget, modifier = Modifier.fillMaxWidth()) {
            Text("Forget this device")
        }
    }
}
