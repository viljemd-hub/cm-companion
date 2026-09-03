package si.apartmamatevz.cmcompanion.ui.today

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
import si.apartmamatevz.cmcompanion.bridge.TodayDashboard
import si.apartmamatevz.cmcompanion.bridge.mergeAttentionItems
import si.apartmamatevz.cmcompanion.bridge.parseAlertAttentionItems
import si.apartmamatevz.cmcompanion.bridge.parseInquiryAttentionItems
import si.apartmamatevz.cmcompanion.bridge.parseTodayDashboard
import si.apartmamatevz.cmcompanion.data.InstallationConnection
import si.apartmamatevz.cmcompanion.data.SeenInquiriesStore

class TodayViewModel(private val seenStore: SeenInquiriesStore) : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var dashboard by mutableStateOf<TodayDashboard?>(null)
        private set
    var attention by mutableStateOf<List<AttentionItem>>(emptyList())
        private set

    fun load(connection: InstallationConnection) {
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            try {
                val client = BridgeClient(connection)
                val today = withContext(Dispatchers.IO) { client.get("dashboard/today.php") }
                dashboard = parseTodayDashboard(today)

                // Attention is a client-side merge of two independently-scoped
                // reads - if either one fails (e.g. a device paired before a
                // scope existed), that shouldn't take down the core Today
                // data above, so each is fetched defensively on its own.
                val inquiryItems = runCatching {
                    withContext(Dispatchers.IO) { client.get("dashboard/inquiries.php") }
                }.map(::parseInquiryAttentionItems).getOrDefault(emptyList())

                val alertItems = runCatching {
                    withContext(Dispatchers.IO) { client.get("dashboard/alerts.php") }
                }.map(::parseAlertAttentionItems).getOrDefault(emptyList())

                // Local-only "seen" filter (2026-09-03 decision): once a host
                // has opened an inquiry on this device, it drops off Attention
                // here, even though the underlying pending inquiry is still
                // fully present server-side (and in the admin panel).
                val unseenInquiries = inquiryItems.filterNot { seenStore.isSeen(it.id) }

                attention = mergeAttentionItems(unseenInquiries, alertItems)
            } catch (e: Exception) {
                errorMessage = e.message ?: e.javaClass.simpleName
            } finally {
                isLoading = false
            }
        }
    }
}
