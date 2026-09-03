package si.apartmamatevz.cmcompanion.ui.dock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import si.apartmamatevz.cmcompanion.BuildConfig
import si.apartmamatevz.cmcompanion.bridge.PairingRequest
import si.apartmamatevz.cmcompanion.data.isOnWifi

/**
 * Server generates codes as strtoupper(bin2hex(random_bytes(4))) split
 * 4-4 with a dash - hex chars only, always uppercase. The exchange
 * endpoint compares codes with a strict !== (case-sensitive), so a
 * lowercase paste would silently never match - hence forcing uppercase
 * here rather than just validating and rejecting it.
 */
private val PAIRING_CODE_PATTERN = Regex("^[0-9A-F]{4}-[0-9A-F]{4}$")

/**
 * Manual-entry docking screen: base URL + installation id + one-time code.
 * A future QR scanner screen resolves to the same [PairingRequest] and
 * calls [onPair] with it - see bridge/PairingDeepLink.kt.
 */
@Composable
fun DockScreen(
    isPairing: Boolean,
    errorMessage: String?,
    onPair: (PairingRequest, displayName: String) -> Unit,
    onTestConnection: () -> Unit = {},
) {
    val baseUrl = remember { mutableStateOf("") }
    val installationId = remember { mutableStateOf("") }
    val code = remember { mutableStateOf("") }
    val displayName = remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Add CM installation")
        Text("Generate a pairing code from the CM admin panel, then enter it here.")

        OutlinedTextField(
            value = displayName.value,
            onValueChange = { displayName.value = it },
            label = { Text("Name (e.g. Apartma Matevž)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = baseUrl.value,
            onValueChange = { baseUrl.value = it },
            label = { Text("CM Bridge URL") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = installationId.value,
            onValueChange = { installationId.value = it },
            label = { Text("Installation ID") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = code.value,
            onValueChange = { code.value = it.uppercase() },
            label = { Text("Pairing code") },
            placeholder = { Text("XXXX-XXXX") },
            isError = code.value.isNotBlank() && !PAIRING_CODE_PATTERN.matches(code.value),
            modifier = Modifier.fillMaxWidth(),
        )
        if (code.value.isNotBlank() && !PAIRING_CODE_PATTERN.matches(code.value)) {
            Text("Expected format: XXXX-XXXX (e.g. A1B2-C3D4) - make sure you copied the short code, not the deep link.")
        }

        errorMessage?.let { Text(it) }

        if (isPairing) {
            CircularProgressIndicator()
        } else {
            // Disabled instead of letting an empty field reach the server -
            // a real debugging session found that submitting with a blank
            // Installation ID or Pairing code produces the exact same
            // generic 400 either way, which is hard to diagnose from the
            // app alone. Trimmed too, since copy/paste from the admin page
            // can carry a stray leading/trailing space.
            val canSubmit = baseUrl.value.isNotBlank() && installationId.value.isNotBlank() &&
                PAIRING_CODE_PATTERN.matches(code.value)
            Button(
                enabled = canSubmit,
                onClick = {
                    onPair(
                        PairingRequest(
                            bridgeBaseUrl = baseUrl.value.trim(),
                            installationId = installationId.value.trim(),
                            oneTimeCode = code.value.trim(),
                        ),
                        displayName.value.trim(),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Pair")
            }
        }

        // Debug-build-only shortcut (2026-09-03), backed by
        // app/dev.secrets.properties - see DockViewModel.useTestConnection()
        // and build.gradle.kts. Absent (button doesn't render) unless that
        // local, git-ignored file was present at build time - a fresh
        // clone of this public repo never shows it. Also gated to WiFi
        // ("če je v lokalnem omrežju, naj bo vidno sicer ne") - a shared
        // family debug APK's embedded token shouldn't be one tap away from
        // anywhere on mobile data.
        val context = LocalContext.current
        if (BuildConfig.DEV_DEVICE_TOKEN.isNotBlank() && isOnWifi(context)) {
            OutlinedButton(onClick = onTestConnection, modifier = Modifier.fillMaxWidth()) {
                Text("Test connection (${BuildConfig.DEV_DEVICE_LABEL.ifBlank { "debug" }})")
            }
        }
    }
}
