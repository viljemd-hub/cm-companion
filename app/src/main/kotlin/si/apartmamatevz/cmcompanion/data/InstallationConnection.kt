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
    // Full URL the app-bar logo's short tap opens in the system browser -
    // "continue working on this installation's actual web UI". Null means
    // not customized yet; callers derive a default straight from baseUrl
    // (see CompanionShell.defaultContinueUrl()) rather than storing that
    // default in every existing connection. Deliberately a full URL, not
    // a path fragment relative to baseUrl - editing the actual real URL
    // is clearer than a bare relative string the user has to trust
    // concatenates correctly.
    val continueUrl: String? = null,
)

enum class ConnectionStatus {
    ACTIVE,
    UNREACHABLE,
    REVOKED,
}
