package si.apartmamatevz.cmcompanion.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import si.apartmamatevz.cmcompanion.data.InstallationConnection
import si.apartmamatevz.cmcompanion.ui.dock.DockScreen
import si.apartmamatevz.cmcompanion.ui.dock.DockViewModel
import si.apartmamatevz.cmcompanion.ui.today.TodayScreen
import si.apartmamatevz.cmcompanion.ui.today.TodayViewModel

/**
 * Top-level shell: an installation switcher in the app bar, four tabs
 * underneath (Today / Alerts / Inquiries / Connection), per the v0.1
 * screen scope in CM_Mobile_Companion_Plan_v0.1.md §6.
 *
 * Multi-installation lives in the data model from day one (see
 * data/InstallationConnection.kt) but the UI stays deliberately flat: with
 * one paired installation the switcher is barely noticeable, with several
 * it's a plain dropdown - no account-management screen in v0.1.
 */
enum class CompanionTab { TODAY, ALERTS, INQUIRIES, CONNECTION }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionShell(dockViewModel: DockViewModel) {
    // Named dockViewModel, not viewModel - the latter would shadow the
    // imported androidx.lifecycle.viewmodel.compose.viewModel() Composable
    // called below for per-tab view models (TodayScreen).
    val connections = dockViewModel.connections
    var selected by remember(connections) { mutableStateOf(connections.firstOrNull()) }
    var tab by remember { mutableStateOf(CompanionTab.TODAY) }

    if (selected == null) {
        DockScreen(
            isPairing = dockViewModel.isPairing,
            errorMessage = dockViewModel.errorMessage,
            onPair = { request, name -> dockViewModel.pair(request, name, deviceLabel = "Android") },
        )
        return
    }

    Scaffold(
        topBar = { TopAppBar(title = { InstallationSwitcher(connections, selected) { selected = it } }) },
        bottomBar = {
            NavigationBar {
                CompanionTab.entries.forEach { candidate ->
                    NavigationBarItem(
                        selected = tab == candidate,
                        onClick = { tab = candidate },
                        icon = {},
                        label = { Text(candidate.name) },
                    )
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            when (tab) {
                CompanionTab.TODAY -> TodayScreen(selected!!, viewModel())
                // Alerts/Inquiries/Connection are the next increment (see
                // pro_dev_roadmap memory, 2026-09-03 TODAY design review) -
                // deliberately not bundled into this pass.
                else -> Text("TODO: ${tab.name} screen for ${selected?.displayName}")
            }
        }
    }
}

@Composable
private fun InstallationSwitcher(
    connections: List<InstallationConnection>,
    selected: InstallationConnection?,
    onSelect: (InstallationConnection) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    TextButton(onClick = { expanded = true }) {
        Text(selected?.displayName ?: "CM Companion")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        connections.forEach { connection ->
            DropdownMenuItem(
                text = { Text(connection.displayName) },
                onClick = {
                    onSelect(connection)
                    expanded = false
                },
            )
        }
    }
}
