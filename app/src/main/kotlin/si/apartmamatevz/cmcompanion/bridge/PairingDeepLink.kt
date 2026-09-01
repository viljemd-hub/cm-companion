package si.apartmamatevz.cmcompanion.bridge

import android.net.Uri

/**
 * Parses the "Add to Companion" deep link a CM admin panel would render as
 * a QR code / tappable link once it generates a pairing code:
 *
 *   cmcompanion://pair?url=<bridge_base_url>&installation_id=<id>&code=<one_time_code>
 *
 * QR scanning UI is not built yet (see CM_Mobile_Companion_Plan_v0.1.md
 * §9, open for a future session) - this parser exists now so the pairing
 * *protocol* is locked in from v0.1, even before a camera screen calls it.
 * Manual entry (PairingScreen) and this deep link both resolve to the same
 * [PairingRequest] and the same exchange call - no separate code path.
 */
data class PairingRequest(
    val bridgeBaseUrl: String,
    val installationId: String,
    val oneTimeCode: String,
)

fun parsePairingDeepLink(uri: Uri): PairingRequest? {
    if (uri.scheme != "cmcompanion" || uri.host != "pair") return null
    val url = uri.getQueryParameter("url") ?: return null
    val installationId = uri.getQueryParameter("installation_id") ?: return null
    val code = uri.getQueryParameter("code") ?: return null
    return PairingRequest(bridgeBaseUrl = url, installationId = installationId, oneTimeCode = code)
}
