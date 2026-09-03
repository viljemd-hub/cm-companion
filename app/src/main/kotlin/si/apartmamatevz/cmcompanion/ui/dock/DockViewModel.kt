package si.apartmamatevz.cmcompanion.ui.dock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import si.apartmamatevz.cmcompanion.bridge.BridgeException
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
                errorMessage = describeError(e)
            } finally {
                isPairing = false
            }
        }
    }

    /**
     * BridgeException.message is just "Bridge request failed: HTTP 400" -
     * the actually useful part (invalid_code / code_expired / missing_code
     * / ...) is the server's JSON error field, carried in .body but never
     * surfaced before. A real debugging session (2026-09-02/03) burned a
     * lot of time because the app only ever showed the HTTP status, never
     * the reason - this reads the JSON body when present so the error text
     * on screen means something without needing the server access log.
     */
    private fun describeError(e: Exception): String {
        if (e is BridgeException) {
            val reason = runCatching { JSONObject(e.body).optString("error") }.getOrNull()
            if (!reason.isNullOrBlank()) return "$reason (HTTP ${e.httpStatus})"
        }
        return e.message ?: e.javaClass.simpleName
    }

    fun forget(connectionId: String) {
        // TODO once a real "Connected devices" revoke endpoint exists server-side
        // (see bridge/PairingExchange.kt): call it here before removing locally,
        // so a lost/stolen phone can't keep using a token CM still thinks is valid.
        store.remove(connectionId)
        connections = store.list()
    }
}
