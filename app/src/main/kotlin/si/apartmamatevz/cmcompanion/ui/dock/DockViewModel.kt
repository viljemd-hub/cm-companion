package si.apartmamatevz.cmcompanion.ui.dock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    var connections by mutableStateOf(store.list())
        private set

    fun pair(request: PairingRequest, displayName: String, deviceLabel: String) {
        isPairing = true
        errorMessage = null
        viewModelScope.launch {
            try {
                // BridgeClient/PairingExchange do blocking OkHttp calls -
                // Android forbids network I/O on the main thread
                // (NetworkOnMainThreadException), so this must run on IO.
                val result = withContext(Dispatchers.IO) {
                    PairingExchange().exchange(request, deviceLabel)
                }
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
                connections = store.list()
            } catch (e: Exception) {
                errorMessage = e.message ?: e.javaClass.simpleName
            } finally {
                isPairing = false
            }
        }
    }

    fun forget(connectionId: String) {
        // TODO once a real "Connected devices" revoke endpoint exists server-side
        // (see bridge/PairingExchange.kt): call it here before removing locally,
        // so a lost/stolen phone can't keep using a token CM still thinks is valid.
        store.remove(connectionId)
        connections = store.list()
    }
}
