package si.apartmamatevz.cmcompanion.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import si.apartmamatevz.cmcompanion.BuildConfig
import si.apartmamatevz.cmcompanion.bridge.AttentionKind
import si.apartmamatevz.cmcompanion.data.InstallationConnection
import si.apartmamatevz.cmcompanion.data.SeenInquiriesStore
import si.apartmamatevz.cmcompanion.ui.alerts.AlertsScreen
import si.apartmamatevz.cmcompanion.ui.alerts.AlertsViewModel
import si.apartmamatevz.cmcompanion.ui.connection.ConnectionScreen
import si.apartmamatevz.cmcompanion.ui.dock.DockScreen
import si.apartmamatevz.cmcompanion.ui.dock.DockViewModel
import si.apartmamatevz.cmcompanion.ui.inquiries.InquiriesScreen
import si.apartmamatevz.cmcompanion.ui.inquiries.InquiriesViewModel
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
enum class CompanionTab(val label: String) {
    TODAY("Today"),
    ALERTS("Alerts"),
    INQUIRIES("Inquiries"),
    CONNECTION("Connection"),
}

private fun CompanionTab.icon() = when (this) {
    CompanionTab.TODAY -> Icons.Filled.Home
    CompanionTab.ALERTS -> Icons.Filled.Notifications
    CompanionTab.INQUIRIES -> Icons.Filled.Mail
    CompanionTab.CONNECTION -> Icons.Filled.Link
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionShell(dockViewModel: DockViewModel) {
    // Named dockViewModel, not viewModel - the latter would shadow the
    // imported androidx.lifecycle.viewmodel.compose.viewModel() Composable
    // called below for per-tab view models (TodayScreen).
    val connections = dockViewModel.connections
    var selected by remember(connections) { mutableStateOf(connections.firstOrNull()) }
    var tab by remember { mutableStateOf(CompanionTab.TODAY) }
    var showAddInstallation by remember { mutableStateOf(false) }

    // Closes "Add another installation" mode automatically once a new
    // pairing actually lands in the store - the alternative (tracking
    // pair()'s async success explicitly) is more plumbing for the same
    // result, since any change to connections while adding means it worked.
    LaunchedEffect(connections) { showAddInstallation = false }

    if (selected == null || showAddInstallation) {
        DockScreen(
            isPairing = dockViewModel.isPairing,
            errorMessage = dockViewModel.errorMessage,
            onPair = { request, name -> dockViewModel.pair(request, name, deviceLabel = "Android") },
            onTestConnection = {
                dockViewModel.useTestConnection(
                    bridgeUrl = BuildConfig.DEV_BRIDGE_URL,
                    installationId = BuildConfig.DEV_INSTALLATION_ID,
                    deviceToken = BuildConfig.DEV_DEVICE_TOKEN,
                    deviceLabel = BuildConfig.DEV_DEVICE_LABEL,
                )
            },
            showCancel = selected != null,
            onCancel = { showAddInstallation = false },
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
                        icon = { Icon(candidate.icon(), contentDescription = candidate.label) },
                        label = { Text(candidate.label) },
                    )
                }
            }
        },
    ) { padding ->
        val context = LocalContext.current
        val seenStoreFactory = remember {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    val seenStore = SeenInquiriesStore(context.applicationContext)
                    return when (modelClass) {
                        TodayViewModel::class.java -> TodayViewModel(seenStore) as T
                        InquiriesViewModel::class.java -> InquiriesViewModel(seenStore) as T
                        else -> throw IllegalArgumentException("Unknown ViewModel: $modelClass")
                    }
                }
            }
        }

        // Hoisted (not created inline per-branch) so InquiriesScreen's
        // onDone below can force a Today reload on the SAME instance -
        // fixes a real 2026-09-03 report where returning to Today after
        // viewing/accepting an inquiry left the Attention count stale
        // until an unrelated full reload happened to occur.
        val todayViewModel: TodayViewModel = viewModel(factory = seenStoreFactory)
        val inquiriesViewModel: InquiriesViewModel = viewModel(factory = seenStoreFactory)
        val alertsViewModel: AlertsViewModel = viewModel()
        var pendingInquiryId by remember { mutableStateOf<String?>(null) }

        Column(modifier = Modifier.padding(padding)) {
            when (tab) {
                CompanionTab.TODAY -> TodayScreen(
                    connection = selected!!,
                    viewModel = todayViewModel,
                    onInquiryClick = { inquiryId ->
                        pendingInquiryId = inquiryId
                        tab = CompanionTab.INQUIRIES
                    },
                )
                CompanionTab.INQUIRIES -> InquiriesScreen(
                    connection = selected!!,
                    viewModel = inquiriesViewModel,
                    initialExpandedId = pendingInquiryId,
                    onDone = {
                        pendingInquiryId = null
                        tab = CompanionTab.TODAY
                        todayViewModel.load(selected!!)
                    },
                )
                CompanionTab.CONNECTION -> ConnectionScreen(
                    connections = connections,
                    onAddAnother = { showAddInstallation = true },
                    onForget = { connection -> dockViewModel.forget(connection) },
                )
                CompanionTab.ALERTS -> AlertsScreen(
                    connections = connections,
                    activeConnectionId = selected?.id,
                    viewModel = alertsViewModel,
                    onItemClick = { item ->
                        val target = connections.firstOrNull { it.id == item.connectionId }
                        if (target != null) {
                            selected = target
                            if (item.kind == AttentionKind.INQUIRY) {
                                pendingInquiryId = item.id
                                tab = CompanionTab.INQUIRIES
                            } else {
                                tab = CompanionTab.TODAY
                            }
                        }
                    },
                )
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
