package si.apartmamatevz.cmcompanion.bridge

import org.json.JSONObject

/**
 * Mirrors GET dashboard/today.php's response shape (cm_bridge_dashboard_today()
 * in cm_bridge_dashboard.php, pro-dev-repo). guest_count is nullable - null
 * means the server couldn't resolve it (e.g. a stale occupancy_merged
 * reference), not zero guests.
 */
data class TodayDashboard(
    val date: String,
    val totals: TodayTotals,
    val units: List<TodayUnit>,
)

data class TodayTotals(
    val arrivals: Int,
    val arrivalGuests: Int,
    val departures: Int,
    val departureGuests: Int,
    val hostingNow: Int,
    val hostingNowGuests: Int,
)

data class TodayUnit(
    val unit: String,
    val currentlyHosting: TodayStay?,
    val arrivals: List<TodayStay>,
    val departures: List<TodayStay>,
)

data class TodayStay(
    val id: String,
    val guestCount: Int?,
    val checkout: String? = null,
)

fun parseTodayDashboard(json: JSONObject): TodayDashboard {
    val totalsJson = json.getJSONObject("totals")
    val totals = TodayTotals(
        arrivals = totalsJson.getInt("arrivals"),
        arrivalGuests = totalsJson.getInt("arrival_guests"),
        departures = totalsJson.getInt("departures"),
        departureGuests = totalsJson.getInt("departure_guests"),
        hostingNow = totalsJson.getInt("hosting_now"),
        hostingNowGuests = totalsJson.getInt("hosting_now_guests"),
    )

    val unitsJson = json.getJSONArray("units")
    val units = (0 until unitsJson.length()).map { i ->
        val u = unitsJson.getJSONObject(i)
        TodayUnit(
            unit = u.getString("unit"),
            currentlyHosting = u.optJSONObject("currently_hosting")?.let(::parseStay),
            arrivals = parseStayList(u.getJSONArray("arrivals")),
            departures = parseStayList(u.getJSONArray("departures")),
        )
    }

    return TodayDashboard(date = json.getString("date"), totals = totals, units = units)
}

private fun parseStayList(array: org.json.JSONArray): List<TodayStay> =
    (0 until array.length()).map { i -> parseStay(array.getJSONObject(i)) }

private fun parseStay(o: JSONObject): TodayStay = TodayStay(
    id = o.getString("id"),
    guestCount = if (o.isNull("guest_count")) null else o.getInt("guest_count"),
    checkout = if (o.has("checkout") && !o.isNull("checkout")) o.getString("checkout") else null,
)
