package si.apartmamatevz.cmcompanion.bridge

import org.json.JSONObject

/** Mirrors GET dashboard/reservations.php - a real, confirmed booking (never a cleaning block or ICS placeholder). */
data class Reservation(
    val id: String,
    val unit: String,
    val from: String,
    val to: String,
    val guestName: String,
    val guestCount: Int?,
)

fun parseReservations(json: JSONObject): List<Reservation> {
    val array = json.getJSONArray("reservations")
    return (0 until array.length()).map { i ->
        val o = array.getJSONObject(i)
        Reservation(
            id = o.getString("id"),
            unit = o.getString("unit"),
            from = o.getString("from"),
            to = o.getString("to"),
            guestName = o.optString("guest_name", ""),
            guestCount = if (o.isNull("guest_count")) null else o.optInt("guest_count"),
        )
    }
}
