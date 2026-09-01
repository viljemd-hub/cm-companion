package si.apartmamatevz.cmcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import si.apartmamatevz.cmcompanion.bridge.parsePairingDeepLink
import si.apartmamatevz.cmcompanion.data.ConnectionStore
import si.apartmamatevz.cmcompanion.ui.CompanionShell
import si.apartmamatevz.cmcompanion.ui.dock.DockViewModel
import si.apartmamatevz.cmcompanion.ui.theme.CmCompanionTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DockViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return DockViewModel(ConnectionStore(applicationContext)) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // cmcompanion://pair?url=...&installation_id=...&code=... - see
        // bridge/PairingDeepLink.kt. If the activity was launched from a
        // scanned/tapped pairing link, feed it straight into the same
        // pairing flow the manual-entry screen uses.
        intent?.data?.let { uri ->
            parsePairingDeepLink(uri)?.let { request ->
                viewModel.pair(request, displayName = request.installationId, deviceLabel = "Android")
            }
        }

        setContent {
            CmCompanionTheme {
                CompanionShell(viewModel)
            }
        }
    }
}
