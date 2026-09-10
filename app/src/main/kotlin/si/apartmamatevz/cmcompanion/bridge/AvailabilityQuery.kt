package si.apartmamatevz.cmcompanion.bridge

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import si.apartmamatevz.cmcompanion.data.InstallationConnection
import java.util.concurrent.TimeUnit

/**
 * Plus-tier "quick availability check" (named in pro_dev_roadmap memory,
 * 2026-09-03/17: a guest asks an ad-hoc question outside the formal
 * inquiry flow, host has no instant way to answer). Deliberately calls
 * the EXISTING public/api/availability_multi.php - already documented in
 * the Master Dogovor §13 as the machine-readable availability surface for
 * third parties/AI, unauthenticated by design (read-only, no PII). No new
 * Bridge scope needed: this is not Bridge-protocol traffic at all, it's
 * the same public endpoint any outside integrator already has.
 *
 * Uses connection.baseUrl's own site root (strips the Bridge API suffix,
 * same derivation as CompanionShell's defaultContinueUrl()) rather than
 * a second stored URL - one source of truth for "where is this
 * installation" per connection.
 */
private const val BRIDGE_API_SUFFIX = "/admin/api/bridge/v1"

data class AvailabilityResult(
    val unit: String,
    val available: Boolean,
    val reason: String,
)

data class AvailabilityQueryResponse(
    val nights: Int,
    val results: List<AvailabilityResult>,
)

class AvailabilityQuery {
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun check(connection: InstallationConnection, from: String, to: String): AvailabilityQueryResponse {
        val siteRoot = connection.baseUrl.trimEnd('/').removeSuffix(BRIDGE_API_SUFFIX)
        val url = "$siteRoot/public/api/availability_multi.php?from=$from&to=$to&units=all"
        val request = Request.Builder().url(url).get().build()

        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val json = JSONObject(body)
            if (!json.optBoolean("ok", false)) {
                throw BridgeException(response.code, body)
            }

            val resultsJson = json.getJSONObject("results")
            val results = resultsJson.keys().asSequence().map { unit ->
                val r = resultsJson.getJSONObject(unit)
                AvailabilityResult(
                    unit = unit,
                    available = r.optBoolean("available", false),
                    reason = r.optString("reason", ""),
                )
            }.sortedBy { it.unit }.toList()

            return AvailabilityQueryResponse(nights = json.optInt("nights", 0), results = results)
        }
    }
}
