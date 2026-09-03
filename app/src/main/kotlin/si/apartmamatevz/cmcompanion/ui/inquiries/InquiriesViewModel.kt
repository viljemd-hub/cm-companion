package si.apartmamatevz.cmcompanion.ui.inquiries

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import si.apartmamatevz.cmcompanion.bridge.BridgeClient
import si.apartmamatevz.cmcompanion.bridge.Inquiry
import si.apartmamatevz.cmcompanion.bridge.parseInquiries
import si.apartmamatevz.cmcompanion.data.InstallationConnection
import si.apartmamatevz.cmcompanion.data.SeenInquiriesStore

class InquiriesViewModel(private val seenStore: SeenInquiriesStore) : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var inquiries by mutableStateOf<List<Inquiry>>(emptyList())
        private set
    var expandedId by mutableStateOf<String?>(null)
        private set
    var acceptingId by mutableStateOf<String?>(null)
        private set

    fun load(connection: InstallationConnection) {
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    BridgeClient(connection).get("dashboard/inquiries.php")
                }
                inquiries = parseInquiries(json)
            } catch (e: Exception) {
                errorMessage = e.message ?: e.javaClass.simpleName
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Expanding IS the "seen" trigger (2026-09-03 decision) - the host
     * looked at it, that's enough to drop it off Today's Attention feed on
     * this device, independent of whether they go on to accept it.
     */
    fun toggleExpanded(inquiryId: String) {
        expandedId = if (expandedId == inquiryId) null else inquiryId
        seenStore.markSeen(inquiryId)
    }

    /**
     * Deliberately Accept-only, no Reject button in the app (2026-09-03
     * decision) - the user has been burned before by an accidental hasty
     * reject; rejection stays an admin-panel-only action, rejection can
     * always wait. Calls the existing action.inquiry_respond Bridge scope,
     * which proxies to the real accept_inquiry.php (ICS/occupancy safety
     * gates, auto-reject-conflicting-pending, accept-link email - all
     * inherited, not reimplemented here).
     */
    fun accept(connection: InstallationConnection, inquiryId: String, onDone: () -> Unit) {
        acceptingId = inquiryId
        errorMessage = null
        viewModelScope.launch {
            try {
                val body = JSONObject().apply {
                    put("id", inquiryId)
                    put("decision", "accept")
                }
                val result = withContext(Dispatchers.IO) {
                    BridgeClient(connection).post("action/inquiry_respond.php", body)
                }
                if (result.optBoolean("ok", false)) {
                    inquiries = inquiries.filterNot { it.id == inquiryId }
                    seenStore.markSeen(inquiryId)
                    onDone()
                } else {
                    errorMessage = result.optString("error", "accept_failed")
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: e.javaClass.simpleName
            } finally {
                acceptingId = null
            }
        }
    }
}
