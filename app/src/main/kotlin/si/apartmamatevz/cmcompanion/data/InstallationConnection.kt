package si.apartmamatevz.cmcompanion.data

/**
 * One paired CM installation, as seen from this device.
 *
 * Deliberately NOT "the user's account" - a Companion install can hold
 * several of these (one owner, several properties/CM installs, or several
 * installs shared by one person). See docs/architecture.md "Device,
 * account, installation" for the reasoning: identity = this device,
 * not a login. [deviceToken] is per (device, installation) - a lost phone
 * is revoked from the CM admin's "Connected devices" list without
 * touching any other device's access.
 */
data class InstallationConnection(
    val id: String,
    val installationId: String,
    val baseUrl: String,
    val deviceToken: String,
    val displayName: String,
    val createdAt: Long,
    val lastConnectedAt: Long?,
    val status: ConnectionStatus,
    val scopes: List<String>,
    // User-editable path (relative to baseUrl) the app-bar logo's short
    // tap opens in the system browser - "continue working on this
    // installation's actual web UI". Null means not customized yet, so
    // callers fall back to admin/admin_calendar.php rather than storing
    // that default in every existing connection.
    val continueUrlPath: String? = null,
)

enum class ConnectionStatus {
    ACTIVE,
    UNREACHABLE,
    REVOKED,
}
