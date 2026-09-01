package si.apartmamatevz.cmcompanion.bridge

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Exchanges a one-time pairing code for a permanent per-device token.
 *
 * SERVER SIDE DOES NOT EXIST YET. CM_Mobile_Companion_Plan_v0.1.md §4/§9
 * names this as open work: a device-pairing endpoint on the CM PRO side,
 * following the CM Connector installation_id + one-time-code pattern, is
 * a prerequisite for this call to succeed against a real installation.
 * This class exists so the client-side contract is fixed now (request/
 * response shape below), not guessed differently later once the server
 * endpoint lands.
 *
 * Expected server endpoint (planned path, not yet built):
 *   POST {bridgeBaseUrl}/pairing/exchange
 *   body: {"installation_id": ..., "code": ..., "device_label": ...}
 *   response: {"ok": true, "device_token": ..., "scopes": [...]}
 */
class PairingExchange {

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun exchange(request: PairingRequest, deviceLabel: String): PairingResult {
        val body = JSONObject().apply {
            put("installation_id", request.installationId)
            put("code", request.oneTimeCode)
            put("device_label", deviceLabel)
        }
        val httpRequest = Request.Builder()
            .url("${request.bridgeBaseUrl.trimEnd('/')}/pairing/exchange")
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        http.newCall(httpRequest).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw BridgeException(response.code, text)
            val json = JSONObject(text)
            val scopesArray = json.getJSONArray("scopes")
            return PairingResult(
                deviceToken = json.getString("device_token"),
                scopes = (0 until scopesArray.length()).map { scopesArray.getString(it) },
            )
        }
    }
}

data class PairingResult(
    val deviceToken: String,
    val scopes: List<String>,
)
