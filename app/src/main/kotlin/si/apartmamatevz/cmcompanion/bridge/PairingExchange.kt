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
 * Server side lives in cm_bridge_pairing.php / admin/api/bridge/v1/pairing/
 * exchange.php on the CM PRO side (pro-dev-repo), following the CM
 * Connector installation_id + one-time-code pattern.
 *
 * Server endpoint:
 *   POST {bridgeBaseUrl}/pairing/exchange.php
 *   body: {"installation_id": ..., "code": ..., "device_label": ...}
 *   response: {"ok": true, "device_token": ..., "scopes": [...]}
 *
 * Note the literal .php - there is no URL-rewrite/routing layer on the
 * server, every Bridge endpoint is a real file Apache serves directly.
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
            .url("${request.bridgeBaseUrl.trimEnd('/')}/pairing/exchange.php")
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
