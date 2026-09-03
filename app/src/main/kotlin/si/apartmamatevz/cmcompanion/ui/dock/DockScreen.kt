package si.apartmamatevz.cmcompanion.ui.dock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import si.apartmamatevz.cmcompanion.bridge.PairingRequest

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
            onValueChange = { code.value = it },
            label = { Text("Pairing code") },
            modifier = Modifier.fillMaxWidth(),
        )

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
            val canSubmit = baseUrl.value.isNotBlank() && installationId.value.isNotBlank() && code.value.isNotBlank()
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
    }
}
