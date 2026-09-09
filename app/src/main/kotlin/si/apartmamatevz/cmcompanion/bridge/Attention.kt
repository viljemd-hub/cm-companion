package si.apartmamatevz.cmcompanion.bridge

import org.json.JSONObject

/**
 * "What needs my attention today" - client-side merge of two existing,
 * separately-scoped reads (dashboard.alerts, dashboard.inquiries). No new
 * Bridge scope for this: per the 2026-09-03 design review, Companion
 * should present one unified feed to the host even though the data comes
 * from different CM subsystems underneath - see pro_dev_roadmap memory,
 * "Companion ni CM Admin na telefonu ampak host situational awareness".
 *
 * Pending inquiries rank above alerts: a guest is waiting on a human
 * decision, which is a harder deadline than an advisory price/automation
 * alert. Within each kind, newest first.
 */
data class AttentionItem(
    val kind: AttentionKind,
    val id: String,
    val title: String,
    val detail: String,
    val unit: String,
    val createdAt: String,
    // Set only by the cross-installation Alerts screen (2026-09-17) - null
    // in Today's own single-installation use, where it's implicit. Every
    // item already carries its own connectionId there too (see
    // ui/alerts/AlertsViewModel.kt) - this is the same identification seam
    // a future cross-owner CM Community query would need, kept compatible
    // on purpose per the user's own framing, not because Community exists
    // yet (it doesn't).
    val installationLabel: String? = null,
    val connectionId: String? = null,
)

enum class AttentionKind { INQUIRY, ALERT }

fun parseInquiryAttentionItems(json: JSONObject): List<AttentionItem> {
    val inquiries = json.getJSONArray("inquiries")
    return (0 until inquiries.length()).map { i ->
        val inq = inquiries.getJSONObject(i)
        val from = inq.getString("from")
        val to = inq.getString("to")
        val nights = inq.getInt("nights")
        AttentionItem(
            kind = AttentionKind.INQUIRY,
            id = inq.getString("id"),
            title = "New inquiry",
            detail = "$from → $to ($nights nights)",
            unit = inq.getString("unit"),
            createdAt = inq.getString("created"),
        )
    }
}

fun parseAlertAttentionItems(json: JSONObject): List<AttentionItem> {
    val alerts = json.getJSONArray("alerts")
    return (0 until alerts.length()).map { i ->
        val alert = alerts.getJSONObject(i)
        AttentionItem(
            kind = AttentionKind.ALERT,
            id = alert.optString("id"),
            title = alert.optString("title").ifBlank { alert.optString("type") },
            detail = alert.optString("message"),
            unit = alert.optString("unit"),
            createdAt = alert.optString("created_at"),
        )
    }
}

/** Inquiries first (a guest is waiting), then alerts, newest first within each. */
fun mergeAttentionItems(inquiries: List<AttentionItem>, alerts: List<AttentionItem>): List<AttentionItem> {
    val sortedInquiries = inquiries.sortedByDescending { it.createdAt }
    val sortedAlerts = alerts.sortedByDescending { it.createdAt }
    return sortedInquiries + sortedAlerts
}

/** Stamps a batch of items with which installation they came from - see AlertsViewModel. */
fun List<AttentionItem>.taggedWith(installationLabel: String, connectionId: String): List<AttentionItem> =
    map { it.copy(installationLabel = installationLabel, connectionId = connectionId) }
