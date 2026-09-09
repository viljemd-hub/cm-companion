package si.apartmamatevz.cmcompanion.ui.alerts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import si.apartmamatevz.cmcompanion.bridge.AttentionItem
import si.apartmamatevz.cmcompanion.bridge.AttentionKind
import si.apartmamatevz.cmcompanion.data.InstallationConnection

/**
 * Cross-installation feed - see AlertsViewModel for why the active
 * connection's own items are excluded (Today already covers those).
 * Tapping a row switches the active installation (and, for an inquiry,
 * jumps to Inquiries pre-expanded) via onItemClick - same pattern as
 * Today's own ATTENTION section.
 */
@Composable
fun AlertsScreen(
    connections: List<InstallationConnection>,
    activeConnectionId: String?,
    viewModel: AlertsViewModel,
    onItemClick: (AttentionItem) -> Unit,
) {
    LaunchedEffect(connections, activeConnectionId) { viewModel.load(connections, activeConnectionId) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            connections.size <= 1 -> Text(
                "Alerts becomes useful once you've paired more than one installation - " +
                    "Today already covers everything for this one.",
            )
            viewModel.isLoading && viewModel.items.isEmpty() -> CircularProgressIndicator()
            viewModel.errorMessage != null -> Text("Could not load alerts: ${viewModel.errorMessage}")
            viewModel.items.isEmpty() -> Text("All clear on your other installations.")
            else -> viewModel.items.forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(item) },
                ) {
                    Text(item.installationLabel ?: "", fontWeight = FontWeight.Bold)
                    val marker = if (item.kind == AttentionKind.INQUIRY) "●" else "○"
                    Text("$marker ${item.title} — ${item.unit} — ${item.detail}")
                }
            }
        }
    }
}
