package si.apartmamatevz.cmcompanion.data

import android.content.Context

/**
 * Local-only "already looked at this inquiry" tracking (2026-09-03
 * decision, deliberately not server-side): once a host expands an inquiry
 * to view its details, it drops out of the Today ATTENTION feed on this
 * device. Not synced across devices or with the admin panel - acceptable
 * for v0.1, a real limitation if the host uses Companion on more than one
 * phone. Plain (unencrypted) prefs, since inquiry IDs aren't sensitive.
 *
 * unmarkSeen() exists so the host can deliberately put an inquiry back on
 * the Attention list (2026-09-03: "če bi rabil dodatno spodbudo") - seen
 * is a one-way auto-flag on view, but reversible on purpose.
 */
class SeenInquiriesStore(context: Context) {

    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun isSeen(inquiryId: String): Boolean =
        prefs.getStringSet(KEY_SEEN, emptySet())?.contains(inquiryId) == true

    fun markSeen(inquiryId: String) {
        val current = prefs.getStringSet(KEY_SEEN, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(inquiryId)
        prefs.edit().putStringSet(KEY_SEEN, current).apply()
    }

    fun unmarkSeen(inquiryId: String) {
        val current = prefs.getStringSet(KEY_SEEN, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.remove(inquiryId)
        prefs.edit().putStringSet(KEY_SEEN, current).apply()
    }

    companion object {
        private const val FILE_NAME = "cm_companion_seen_inquiries"
        private const val KEY_SEEN = "seen_ids"
    }
}
