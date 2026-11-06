package si.apartmamatevz.cmcompanion.ui.reservations

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import si.apartmamatevz.cmcompanion.bridge.BridgeClient
import si.apartmamatevz.cmcompanion.bridge.Reservation
import si.apartmamatevz.cmcompanion.bridge.parseReservations
import si.apartmamatevz.cmcompanion.data.InstallationConnection

/**
 * Phase 1 of the NFC keycard gateway idea (2026-11-06): just the list, no
 * selection/action yet - "which reservation is this card for" needs a
 * picker to exist before anything can be done with it. No seen-state or
 * expand-in-place here (unlike InquiriesViewModel) - nothing to act on
 * yet, so nothing to track across loads.
 */
class ReservationsViewModel : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var reservations by mutableStateOf<List<Reservation>>(emptyList())
        private set

    fun load(connection: InstallationConnection) {
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    BridgeClient(connection).get("dashboard/reservations.php")
                }
                reservations = parseReservations(json)
            } catch (e: Exception) {
                errorMessage = e.message ?: e.javaClass.simpleName
            } finally {
                isLoading = false
            }
        }
    }
}
