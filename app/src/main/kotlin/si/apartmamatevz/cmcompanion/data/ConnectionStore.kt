package si.apartmamatevz.cmcompanion.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists paired [InstallationConnection]s (including device tokens) in an
 * Android Keystore-backed encrypted file. Never plain SharedPreferences -
 * the device token is equivalent to a password for whatever scopes it
 * carries.
 *
 * One store instance = one device's whole connection list, across every
 * paired CM installation - not one store per installation. Keep this class
 * dumb (read/write JSON); anything scope- or protocol-aware belongs in
 * bridge/, not here.
 */
class ConnectionStore(context: Context) {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun list(): List<InstallationConnection> {
        val raw = prefs.getString(KEY_CONNECTIONS, null) ?: return emptyList()
        val array = JSONArray(raw)
        return (0 until array.length()).map { i -> array.getJSONObject(i).toConnection() }
    }

    fun upsert(connection: InstallationConnection) {
        val existing = list().filterNot { it.id == connection.id }
        save(existing + connection)
    }

    fun remove(id: String) {
        save(list().filterNot { it.id == id })
    }

    private fun save(connections: List<InstallationConnection>) {
        val array = JSONArray()
        connections.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_CONNECTIONS, array.toString()).apply()
    }

    companion object {
        private const val FILE_NAME = "cm_companion_connections"
        private const val KEY_CONNECTIONS = "connections"
    }
}

private fun InstallationConnection.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("installationId", installationId)
    put("baseUrl", baseUrl)
    put("deviceToken", deviceToken)
    put("displayName", displayName)
    put("createdAt", createdAt)
    put("lastConnectedAt", lastConnectedAt)
    put("status", status.name)
    put("scopes", JSONArray(scopes))
    put("continueUrl", continueUrl)
}

private fun JSONObject.toConnection(): InstallationConnection {
    val scopesArray = getJSONArray("scopes")
    return InstallationConnection(
        id = getString("id"),
        installationId = getString("installationId"),
        baseUrl = getString("baseUrl"),
        deviceToken = getString("deviceToken"),
        displayName = getString("displayName"),
        createdAt = getLong("createdAt"),
        lastConnectedAt = if (isNull("lastConnectedAt")) null else getLong("lastConnectedAt"),
        status = ConnectionStatus.valueOf(getString("status")),
        scopes = (0 until scopesArray.length()).map { scopesArray.getString(it) },
        // null-if-missing, not getString - reads from before this field
        // existed (or its earlier "continueUrlPath" name) shouldn't crash
        // on a missing key.
        continueUrl = if (has("continueUrl") && !isNull("continueUrl")) {
            getString("continueUrl")
        } else {
            null
        },
    )
}
