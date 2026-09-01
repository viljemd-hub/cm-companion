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
            Button(
                onClick = {
                    onPair(
                        PairingRequest(
                            bridgeBaseUrl = baseUrl.value,
                            installationId = installationId.value,
                            oneTimeCode = code.value,
                        ),
                        displayName.value,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Pair")
            }
        }
    }
}
