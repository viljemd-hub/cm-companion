package si.apartmamatevz.cmcompanion.ui.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import si.apartmamatevz.cmcompanion.bridge.BridgeClient
import si.apartmamatevz.cmcompanion.bridge.TodayDashboard
import si.apartmamatevz.cmcompanion.bridge.parseTodayDashboard
import si.apartmamatevz.cmcompanion.data.InstallationConnection

class TodayViewModel : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var dashboard by mutableStateOf<TodayDashboard?>(null)
        private set

    fun load(connection: InstallationConnection) {
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    BridgeClient(connection).get("dashboard/today.php")
                }
                dashboard = parseTodayDashboard(json)
            } catch (e: Exception) {
                errorMessage = e.message ?: e.javaClass.simpleName
            } finally {
                isLoading = false
            }
        }
    }
}
