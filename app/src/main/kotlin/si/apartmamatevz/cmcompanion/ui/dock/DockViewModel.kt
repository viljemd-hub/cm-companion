package si.apartmamatevz.cmcompanion.ui.dock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import si.apartmamatevz.cmcompanion.bridge.PairingExchange
import si.apartmamatevz.cmcompanion.bridge.PairingRequest
import si.apartmamatevz.cmcompanion.data.ConnectionStatus
import si.apartmamatevz.cmcompanion.data.ConnectionStore
import si.apartmamatevz.cmcompanion.data.InstallationConnection
import java.util.UUID

/**
 * Backs the "Add CM installation" (docking) screen. One screen handles
 * both manual entry and a resolved QR/deep-link [PairingRequest] - see
 * bridge/PairingDeepLink.kt for why there is only one code path.
 */
class DockViewModel(private val store: ConnectionStore) : ViewModel() {

    var isPairing by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun connections(): List<InstallationConnection> = store.list()

    fun pair(request: PairingRequest, displayName: String, deviceLabel: String) {
        isPairing = true
        errorMessage = null
        try {
            val result = PairingExchange().exchange(request, deviceLabel)
            store.upsert(
                InstallationConnection(
                    id = UUID.randomUUID().toString(),
                    installationId = request.installationId,
                    baseUrl = request.bridgeBaseUrl,
                    deviceToken = result.deviceToken,
                    displayName = displayName,
                    createdAt = System.currentTimeMillis(),
                    lastConnectedAt = System.currentTimeMillis(),
                    status = ConnectionStatus.ACTIVE,
                    scopes = result.scopes,
                ),
            )
        } catch (e: Exception) {
            errorMessage = e.message
        } finally {
            isPairing = false
        }
    }

    fun forget(connectionId: String) {
        // TODO once a real "Connected devices" revoke endpoint exists server-side
        // (see bridge/PairingExchange.kt): call it here before removing locally,
        // so a lost/stolen phone can't keep using a token CM still thinks is valid.
        store.remove(connectionId)
    }
}
