package si.apartmamatevz.cmcompanion.ui.dock

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import si.apartmamatevz.cmcompanion.R
import si.apartmamatevz.cmcompanion.bridge.PairingRequest
import si.apartmamatevz.cmcompanion.bridge.parsePairingDeepLink
import si.apartmamatevz.cmcompanion.ui.theme.CmColors

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
 * The QR scanner button resolves to the exact same [PairingRequest] and
 * calls [onPair] with it - see bridge/PairingDeepLink.kt - so scanning
 * and typing can never quietly drift apart into two implementations.
 */
@Composable
fun DockScreen(
    isPairing: Boolean,
    errorMessage: String?,
    onPair: (PairingRequest, displayName: String) -> Unit,
    showCancel: Boolean = false,
    onCancel: () -> Unit = {},
) {
    val baseUrl = remember { mutableStateOf("") }
    val installationId = remember { mutableStateOf("") }
    val code = remember { mutableStateOf("") }
    val displayName = remember { mutableStateOf("") }
    var scanError by remember { mutableStateOf<String?>(null) }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents ?: return@rememberLauncherForActivityResult
        val request = runCatching { parsePairingDeepLink(Uri.parse(contents)) }.getOrNull()
        if (request == null) {
            scanError = "That QR code isn't a CM Companion pairing link."
        } else {
            scanError = null
            onPair(request, displayName.value.trim().ifBlank { request.installationId })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.cm_logo),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
            Text(
                "CM Companion",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = CmColors.Accent,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        Text("Add CM installation", style = MaterialTheme.typography.titleMedium, color = CmColors.TextPrimary)
        Text(
            "Generate a pairing code from the CM admin panel, then enter it here or scan its QR code.",
            color = CmColors.TextMuted,
        )

        Button(
            onClick = {
                scanLauncher.launch(
                    ScanOptions()
                        .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        .setPrompt("Scan the pairing QR code from your CM admin panel")
                        .setBeepEnabled(false)
                        .setOrientationLocked(false),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Scan QR code")
        }
        scanError?.let { Text(it, color = CmColors.Danger) }

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
            Text(
                "Expected format: XXXX-XXXX (e.g. A1B2-C3D4) - make sure you copied the short code, not the deep link.",
                color = CmColors.Danger,
            )
        }

        errorMessage?.let { Text(it, color = CmColors.Danger) }

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

        // Only shown when reached via "Add another installation" from an
        // existing Connection screen - the very first pairing (no
        // connections yet) has nothing to cancel back to.
        if (showCancel) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }

        // Anyone landing here without their own CM yet (e.g. shown this
        // screen before ever generating a pairing code) needs a way
        // forward that isn't a dead end - CM Free's own downloads page
        // covers both "get CM Free" and "where do I generate a code".
        val linkContext = LocalContext.current
        Text(
            "Don't have CM yet? Get CM Free and pair from Admin → CM Companion.",
            color = CmColors.TextMuted,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            "apartmamatevz.si/cmfree/download.html",
            color = CmColors.Accent,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://apartmamatevz.si/cmfree/download.html"),
                )
                runCatching { linkContext.startActivity(intent) }
            },
        )
    }
}
