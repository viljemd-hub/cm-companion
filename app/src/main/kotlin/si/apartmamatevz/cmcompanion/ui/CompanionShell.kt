package si.apartmamatevz.cmcompanion.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import si.apartmamatevz.cmcompanion.BuildConfig
import si.apartmamatevz.cmcompanion.R
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
import si.apartmamatevz.cmcompanion.ui.theme.CmColors
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
/**
 * Default when a connection's [InstallationConnection.continueUrl] is
 * unset - derived straight from that connection's own baseUrl rather
 * than a bare hardcoded path, so the edit dialog always starts from the
 * real, actually-reachable URL for that specific installation.
 */
private fun defaultContinueUrl(connection: InstallationConnection): String =
    "${connection.baseUrl.trimEnd('/')}/admin/admin_calendar.php"

enum class CompanionTab(val label: String) {
    TODAY("Today"),
    ALERTS("Alerts"),
    INQUIRIES("Inquiries"),
    CONNECTION("Connection"),
}

// Home/Email/Notifications/Settings are in material-icons-core; Link/Mail
// (originally tried here) are extended-only and failed the build - stick
// to the small core icon set rather than pulling in ~2000 extra icons.
private fun CompanionTab.icon() = when (this) {
    CompanionTab.TODAY -> Icons.Filled.Home
    CompanionTab.ALERTS -> Icons.Filled.Notifications
    CompanionTab.INQUIRIES -> Icons.Filled.Email
    CompanionTab.CONNECTION -> Icons.Filled.Settings
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

    val topBarContext = LocalContext.current
    var editingContinueUrlFor by remember { mutableStateOf<InstallationConnection?>(null) }

    editingContinueUrlFor?.let { connection ->
        EditContinueUrlDialog(
            connection = connection,
            onDismiss = { editingContinueUrlFor = null },
            onSave = { url ->
                dockViewModel.setContinueUrl(connection, url)
                editingContinueUrlFor = null
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { InstallationSwitcher(connections, selected) { selected = it } },
                actions = {
                    CmLogoBadge(
                        onOpenAdmin = {
                            val connection = selected ?: return@CmLogoBadge
                            val url = connection.continueUrl?.ifBlank { null } ?: defaultContinueUrl(connection)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            runCatching { topBarContext.startActivity(intent) }
                                .onFailure {
                                    Toast.makeText(topBarContext, "No browser available", Toast.LENGTH_SHORT).show()
                                }
                        },
                        onOpenSettings = { editingContinueUrlFor = selected },
                    )
                },
            )
        },
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

/**
 * Long-pressing the logo edits WHICH page short-tap opens for this one
 * connection - not a generic settings screen, since the whole point is a
 * fast one-tap jump to whatever page this owner actually works from
 * (calendar, a specific unit, manage_reservations.php, ...), which
 * differs per installation and per owner's habits.
 */
@Composable
private fun EditContinueUrlDialog(
    connection: InstallationConnection,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var url by remember(connection.id) {
        mutableStateOf(connection.continueUrl ?: defaultContinueUrl(connection))
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Main installation link") },
        text = {
            Column {
                Text(
                    "Page opened when you tap the logo - defaults to this installation's own admin calendar.",
                    color = CmColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                androidx.compose.material3.OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onSave(url) }) { Text("Save") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * The CM house logo in the app bar - previously this space was just
 * empty, making the bar feel unfinished. Short tap jumps straight to the
 * page set in [EditContinueUrlDialog] for the active connection (fastest
 * path to "continue working on that installation's actual web UI");
 * long press opens that same dialog to change it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CmLogoBadge(onOpenAdmin: () -> Unit, onOpenSettings: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(end = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(onClick = onOpenAdmin, onLongClick = onOpenSettings),
    ) {
        Text(
            "CM-Companion",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            // Tight to the icon, not spread across the bar - this label
            // exists to explain the icon, not to act as a second title.
            modifier = Modifier.padding(end = 6.dp),
        )
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                // White backing plate, same reasoning as the launcher
                // icon's adaptive background - the logo's black
                // outline/white fill was made for a light background,
                // and read as "peeking through" the dark app bar
                // without one.
                .background(androidx.compose.ui.graphics.Color.White),
        ) {
            Icon(
                painter = painterResource(R.drawable.cm_logo),
                contentDescription = "CM Companion",
                tint = androidx.compose.ui.graphics.Color.Unspecified,
                modifier = Modifier
                    .padding(6.dp)
                    .size(28.dp),
            )
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
    androidx.compose.material3.OutlinedButton(
        onClick = { expanded = true },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CmColors.Border),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        // Small raised shadow on top of the border - the plain flat
        // TextButton this replaced looked like inert text, not something
        // tappable; the combination reads as a real, pressable 3D button.
        modifier = Modifier.shadow(elevation = 3.dp, shape = RoundedCornerShape(12.dp)),
    ) {
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
