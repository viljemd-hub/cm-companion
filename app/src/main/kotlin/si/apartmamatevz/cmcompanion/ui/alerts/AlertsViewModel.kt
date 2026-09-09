package si.apartmamatevz.cmcompanion.ui.alerts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import si.apartmamatevz.cmcompanion.bridge.AttentionItem
import si.apartmamatevz.cmcompanion.bridge.BridgeClient
import si.apartmamatevz.cmcompanion.bridge.mergeAttentionItems
import si.apartmamatevz.cmcompanion.bridge.parseAlertAttentionItems
import si.apartmamatevz.cmcompanion.bridge.parseInquiryAttentionItems
import si.apartmamatevz.cmcompanion.bridge.taggedWith
import si.apartmamatevz.cmcompanion.data.InstallationConnection

/**
 * Cross-installation attention feed (2026-09-17 decision): Today already
 * shows full detail for the ACTIVE connection, so Alerts only earns its
 * place once there's more than one paired installation - it answers "did
 * something change on one of my OTHER properties while I'm looking at
 * this one", not a second copy of Today. Deliberately excludes the
 * active connection's own items.
 */
class AlertsViewModel : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var items by mutableStateOf<List<AttentionItem>>(emptyList())
        private set

    fun load(connections: List<InstallationConnection>, activeConnectionId: String?) {
        val others = connections.filterNot { it.id == activeConnectionId }
        if (others.isEmpty()) {
            items = emptyList()
            errorMessage = null
            return
        }

        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            // Sequential, not parallel - simplest correct thing under the
            // 10-connection soft cap (DockViewModel.MAX_CONNECTIONS); worth
            // revisiting with concurrent fetches if that cap ever moves.
            val all = mutableListOf<AttentionItem>()
            for (connection in others) {
                val client = BridgeClient(connection)
                val inquiryItems = runCatching {
                    withContext(Dispatchers.IO) { client.get("dashboard/inquiries.php") }
                }.map(::parseInquiryAttentionItems).getOrDefault(emptyList())

                val alertItems = runCatching {
                    withContext(Dispatchers.IO) { client.get("dashboard/alerts.php") }
                }.map(::parseAlertAttentionItems).getOrDefault(emptyList())

                all += mergeAttentionItems(inquiryItems, alertItems).taggedWith(connection.displayName, connection.id)
            }
            items = all
            isLoading = false
        }
    }
}
