package si.apartmamatevz.cmcompanion.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Used only to gate the debug-build "Test connection" shortcut to WiFi
 * (2026-09-03: "če je v lokalnem omrežju, naj bo vidno sicer ne") - a
 * shared/family debug APK's embedded dev token shouldn't be one tap away
 * from anywhere on mobile data. No location permission needed (unlike
 * reading the actual WiFi SSID) - this only asks "is the active network
 * WiFi at all", not which network.
 */
fun isOnWifi(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}
