package si.apartmamatevz.cmcompanion.bridge

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import si.apartmamatevz.cmcompanion.data.InstallationConnection
import java.util.concurrent.TimeUnit

/**
 * Speaks CM Bridge Protocol v1 and nothing else.
 *
 * Hard rule (see CM_Mobile_Companion_Plan_v0.1.md §2): Companion may only
 * call the same public/Bridge endpoints a third-party integrator could
 * call. No shortcuts into CM internals, no reading JSON data files, no
 * app-only endpoint with looser rules than a real external module gets.
 * If a screen needs data the Bridge doesn't expose yet, the fix is a new
 * documented Bridge scope server-side - never a special case here.
 *
 * One client instance is bound to exactly one [InstallationConnection] -
 * callers juggling several paired installations construct one client per
 * connection, they never share state between installations.
 */
class BridgeClient(private val connection: InstallationConnection) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * GET against a Bridge v1 endpoint, e.g. "dashboard/today.php". Include
     * the literal .php - there is no URL-rewrite layer on the server, see
     * PairingExchange.kt for the same gotcha that broke pairing once.
     */
    fun get(path: String): JSONObject {
        val request = Request.Builder()
            .url("${connection.baseUrl.trimEnd('/')}/$path")
            .header("X-Bridge-Key", connection.deviceToken)
            .get()
            .build()
        return execute(request)
    }

    /** POST a JSON body against a Bridge v1 endpoint, e.g. "action/inquiry_respond.php". */
    fun post(path: String, body: JSONObject): JSONObject {
        val request = Request.Builder()
            .url("${connection.baseUrl.trimEnd('/')}/$path")
            .header("X-Bridge-Key", connection.deviceToken)
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        return execute(request)
    }

    private fun execute(request: Request): JSONObject {
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw BridgeException(response.code, text)
            }
            return JSONObject(text)
        }
    }
}

/**
 * 2026-09-11: the server's own error bodies are useful ("range_not_free",
 * "status_not_soft_hold", etc.) but used to be thrown away entirely - the
 * message was always the generic "Bridge request failed: HTTP $code",
 * with the real reason sitting unread in [body]. Every screen's catch
 * block just does `e.message ?: ...`, so that generic string is what
 * users actually saw for every non-2xx response, real accept/reject
 * conflicts included. Parse the JSON body's "error" field into the
 * message here once, instead of asking every call site to remember to.
 */
class BridgeException(val httpStatus: Int, val body: String) :
    Exception(buildMessage(httpStatus, body)) {
    companion object {
        private fun buildMessage(httpStatus: Int, body: String): String {
            val serverError = try {
                JSONObject(body).optString("error").takeIf { it.isNotBlank() }
            } catch (e: Exception) {
                null
            }
            return if (serverError != null) {
                "$serverError (HTTP $httpStatus)"
            } else {
                "Bridge request failed: HTTP $httpStatus"
            }
        }
    }
}
