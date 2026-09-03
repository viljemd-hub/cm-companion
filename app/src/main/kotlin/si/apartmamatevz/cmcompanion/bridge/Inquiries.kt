package si.apartmamatevz.cmcompanion.bridge

import org.json.JSONObject

/** Mirrors GET dashboard/inquiries.php - never the guest object, only counts + derived country. */
data class Inquiry(
    val id: String,
    val unit: String,
    val from: String,
    val to: String,
    val nights: Int,
    val created: String,
    val adults: Int,
    val kids06: Int,
    val kids712: Int,
    val guestPhoneCountry: String?,
)

fun parseInquiries(json: JSONObject): List<Inquiry> {
    val array = json.getJSONArray("inquiries")
    return (0 until array.length()).map { i ->
        val o = array.getJSONObject(i)
        Inquiry(
            id = o.getString("id"),
            unit = o.getString("unit"),
            from = o.getString("from"),
            to = o.getString("to"),
            nights = o.getInt("nights"),
            created = o.getString("created"),
            adults = o.optInt("adults", 0),
            kids06 = o.optInt("kids06", 0),
            kids712 = o.optInt("kids712", 0),
            guestPhoneCountry = if (o.isNull("guest_phone_country")) null else o.optString("guest_phone_country").ifBlank { null },
        )
    }
}
