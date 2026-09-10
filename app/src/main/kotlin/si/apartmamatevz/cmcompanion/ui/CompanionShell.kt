package si.apartmamatevz.cmcompanion.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import si.apartmamatevz.cmcompanion.R
import si.apartmamatevz.cmcompanion.bridge.AttentionKind
import si.apartmamatevz.cmcompanion.bridge.AvailabilityQuery
import si.apartmamatevz.cmcompanion.bridge.AvailabilityResult
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
 * Every connection's stored baseUrl is the Bridge API root, not the
 * site root - cm_bridge_pairing.php on the server builds the "Bridge
 * URL" it shows during pairing as `{site}/admin/api/bridge/v1`, and the
 * app stores exactly that string (BridgeClient appends paths like
 * `dashboard/today.php` onto it). A real bug (2026-09-10) appended
 * `/admin/admin_calendar.php` straight onto that Bridge-API baseUrl
 * instead of stripping the suffix first, producing a double
 * "admin/.../admin/..." URL that never resolved. This constant documents
 * the exact suffix to strip - it must always match the server side.
 */
private const val BRIDGE_API_SUFFIX = "/admin/api/bridge/v1"

/**
 * Default when a connection's [InstallationConnection.continueUrl] is
 * unset - derived straight from that connection's own baseUrl rather
 * than a bare hardcoded path, so the edit dialog always starts from the
 * real, actually-reachable URL for that specific installation.
 */
private fun defaultContinueUrl(connection: InstallationConnection): String {
    val siteRoot = connection.baseUrl.trimEnd('/').removeSuffix(BRIDGE_API_SUFFIX)
    return "$siteRoot/admin/admin_calendar.php"
}

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

    // Hoisted above Scaffold (not created inline per-branch) so both the
    // top bar (needs the active connection's tier for the Plus badge) and
    // InquiriesScreen's onDone (forces a Today reload on the SAME
    // instance - fixes a real 2026-09-03 report where returning to Today
    // after viewing/accepting an inquiry left the Attention count stale)
    // share one instance instead of two independently-created ones.
    val todayViewModel: TodayViewModel = viewModel(factory = seenStoreFactory)
    val inquiriesViewModel: InquiriesViewModel = viewModel(factory = seenStoreFactory)
    val alertsViewModel: AlertsViewModel = viewModel()
    var pendingInquiryId by remember { mutableStateOf<String?>(null) }
    var showPlusMenu by remember { mutableStateOf(false) }
    var showAvailabilityQuery by remember { mutableStateOf(false) }

    // "free" until Today's first successful load - matches
    // TodayDashboard.tier's own documented fallback, so the Plus badge
    // simply doesn't render rather than flashing incorrectly on launch.
    val tier = todayViewModel.dashboard?.tier ?: "free"

    if (showAvailabilityQuery) {
        AvailabilityQueryDialog(
            connection = selected,
            onDismiss = { showAvailabilityQuery = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { InstallationSwitcher(connections, selected) { selected = it } },
                actions = {
                    CmLogoBadge(
                        tier = tier,
                        onOpenAdmin = {
                            val connection = selected ?: return@CmLogoBadge
                            val url = connection.continueUrl?.ifBlank { null } ?: defaultContinueUrl(connection)
                            // Custom Tabs, not a plain ACTION_VIEW intent
                            // (2026-09-10 real user report: "no way back"
                            // to Companion after tapping the logo) - stays
                            // in the same task with a visible back arrow
                            // to the calling app, instead of launching a
                            // fully separate browser task.
                            runCatching {
                                androidx.browser.customtabs.CustomTabsIntent.Builder()
                                    .build()
                                    .launchUrl(topBarContext, Uri.parse(url))
                            }.onFailure {
                                Toast.makeText(topBarContext, "No browser available", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenSettings = { editingContinueUrlFor = selected },
                        onOpenPlusMenu = { showPlusMenu = true },
                        plusMenuExpanded = showPlusMenu,
                        onDismissPlusMenu = { showPlusMenu = false },
                        onAvailabilityQuery = {
                            showPlusMenu = false
                            showAvailabilityQuery = true
                        },
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
                    onRename = { connection, newName -> dockViewModel.renameConnection(connection, newName) },
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
 * Plus-tier "quick availability check" (see AvailabilityQuery.kt) - first
 * action under the tier badge menu. A guest asks an ad-hoc question
 * mid-conversation outside the formal inquiry flow; this answers it in
 * one query without creating anything. Calls the existing public
 * availability_multi.php directly, not through Bridge auth - it's the
 * same unauthenticated endpoint a third-party integrator already has.
 */
private val EU_DATE_FORMAT = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
private val ISO_DATE_FORMAT = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
    timeZone = java.util.TimeZone.getTimeZone("UTC")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvailabilityQueryDialog(connection: InstallationConnection?, onDismiss: () -> Unit) {
    // Epoch millis (UTC midnight), not a typed string - a real date
    // picker instead of "type YYYY-MM-DD yourself" per user feedback
    // (2026-09-10): typing raw ISO dates on a phone felt error-prone.
    // Displayed to the host in EU dd.MM.yyyy; converted to ISO only for
    // the actual availability_multi.php query.
    var fromMillis by remember { mutableStateOf<Long?>(null) }
    var toMillis by remember { mutableStateOf<Long?>(null) }
    var pickingFrom by remember { mutableStateOf(false) }
    var pickingTo by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<AvailabilityResult>?>(null) }
    // Snapshotted at query time, not read live from fromMillis/toMillis -
    // the host could change the date fields after seeing results but
    // before tapping "open in admin calendar", which must still point at
    // the range those results actually describe.
    var queriedFrom by remember { mutableStateOf<String?>(null) }
    var queriedTo by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val resultsContext = LocalContext.current

    if (pickingFrom) {
        val state = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = fromMillis)
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { pickingFrom = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    fromMillis = state.selectedDateMillis
                    results = null
                    pickingFrom = false
                }) { Text("OK") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pickingFrom = false }) { Text("Cancel") }
            },
        ) { androidx.compose.material3.DatePicker(state = state) }
    }
    if (pickingTo) {
        // Opens on the already-picked "From" date, not today - a
        // departure is virtually always days/weeks after arrival, so
        // starting the picker near "today" instead of near "From" meant
        // scrolling every single time. Only applies the fallback the
        // first time (toMillis == null); once "To" has its own value,
        // that value wins.
        val state = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = toMillis ?: fromMillis,
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { pickingTo = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    toMillis = state.selectedDateMillis
                    results = null
                    pickingTo = false
                }) { Text("OK") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pickingTo = false }) { Text("Cancel") }
            },
        ) { androidx.compose.material3.DatePicker(state = state) }
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Availability for a period") },
        text = {
            // A plain Modifier.clickable on an OutlinedTextField never
            // fires (2026-09-10 real bug) - the field's own internal
            // pointerInput consumes the tap for focus/cursor purposes
            // before it reaches our modifier. The field-as-a-button
            // pattern needs its own interactionSource, watched for a
            // press release, instead.
            val fromInteractions = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            LaunchedEffect(fromInteractions) {
                fromInteractions.interactions.collect {
                    if (it is androidx.compose.foundation.interaction.PressInteraction.Release) pickingFrom = true
                }
            }
            val toInteractions = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            LaunchedEffect(toInteractions) {
                toInteractions.interactions.collect {
                    if (it is androidx.compose.foundation.interaction.PressInteraction.Release) pickingTo = true
                }
            }

            Column {
                OutlinedTextField(
                    value = fromMillis?.let { EU_DATE_FORMAT.format(java.util.Date(it)) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    interactionSource = fromInteractions,
                    label = { Text("From") },
                    placeholder = { Text("dd.mm.yyyy") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = toMillis?.let { EU_DATE_FORMAT.format(java.util.Date(it)) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    interactionSource = toInteractions,
                    label = { Text("To") },
                    placeholder = { Text("dd.mm.yyyy") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                errorMessage?.let { Text(it, color = CmColors.Danger, modifier = Modifier.padding(top = 8.dp)) }
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 12.dp))
                }
                results?.forEach { r ->
                    Text(
                        "${r.unit}: ${if (r.available) "available" else "not available"}",
                        color = if (r.available) CmColors.Accent else CmColors.TextMuted,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    // Only for available results (2026-09-10, user's own
                    // scoping): a free range is what's worth jumping to
                    // admin over - a blocked one has nothing to act on
                    // here, the host already knows it's taken.
                    if (r.available && connection != null && queriedFrom != null && queriedTo != null) {
                        Text(
                            "Open ${r.unit} in admin calendar →",
                            color = CmColors.Accent,
                            style = MaterialTheme.typography.bodySmall,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                            modifier = Modifier
                                .padding(start = 12.dp, top = 2.dp)
                                .clickable {
                                    val siteRoot = connection.baseUrl.trimEnd('/')
                                        .removeSuffix("/admin/api/bridge/v1")
                                    val url = "$siteRoot/admin/admin_calendar.php" +
                                        "?unit=${Uri.encode(r.unit)}" +
                                        "&focus_from=${queriedFrom}&focus_to=${queriedTo}"
                                    runCatching {
                                        androidx.browser.customtabs.CustomTabsIntent.Builder()
                                            .build()
                                            .launchUrl(resultsContext, Uri.parse(url))
                                    }
                                },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isLoading && fromMillis != null && toMillis != null,
                onClick = {
                    val target = connection ?: return@Button
                    val from = fromMillis ?: return@Button
                    val to = toMillis ?: return@Button
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        try {
                            val fromIso = ISO_DATE_FORMAT.format(java.util.Date(from))
                            val toIso = ISO_DATE_FORMAT.format(java.util.Date(to))
                            val response = withContext(Dispatchers.IO) {
                                AvailabilityQuery().check(target, fromIso, toIso)
                            }
                            results = response.results
                            queriedFrom = fromIso
                            queriedTo = toIso
                        } catch (e: Exception) {
                            errorMessage = e.message ?: e.javaClass.simpleName
                        } finally {
                            isLoading = false
                        }
                    }
                },
            ) { Text("Check") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
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
private fun CmLogoBadge(
    tier: String,
    onOpenAdmin: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlusMenu: () -> Unit,
    plusMenuExpanded: Boolean,
    onDismissPlusMenu: () -> Unit,
    onAvailabilityQuery: () -> Unit,
) {
    // Only Plus/PRO get the tier badge + menu - a Free installation has
    // no Plus-only actions to offer, so nothing renders rather than a
    // disabled/greyed-out badge. PRO is included, not just an exact
    // "plus" match: PRO is a strict superset of Plus features, so a PRO
    // owner should see everything a Plus owner sees here, not be locked
    // out of it by a too-literal tier check.
    val showPlusBadge = tier == "plus" || tier == "pro"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 12.dp),
    ) {
        Box {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(end = 6.dp)
                    // Label + tier badge are one clickable unit that opens
                    // the Plus menu - only meaningful when there IS a
                    // menu, so a Free installation's label stays inert.
                    .let { if (showPlusBadge) it.clickableNoIndication(onClick = onOpenPlusMenu) else it },
            ) {
                Text(
                    "CM-Companion",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
                if (showPlusBadge) {
                    Text(
                        tier.uppercase(),
                        color = CmColors.BackgroundDeep,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CmColors.Accent)
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
            DropdownMenu(expanded = plusMenuExpanded, onDismissRequest = onDismissPlusMenu) {
                DropdownMenuItem(
                    text = { Text("Availability for a period") },
                    onClick = onAvailabilityQuery,
                )
            }
        }
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                // White backing plate, same reasoning as the launcher
                // icon's adaptive background - the logo's black
                // outline/white fill was made for a light background,
                // and read as "peeking through" the dark app bar
                // without one.
                .background(androidx.compose.ui.graphics.Color.White)
                .combinedClickable(onClick = onOpenAdmin, onLongClick = onOpenSettings),
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

/**
 * Plain [androidx.compose.foundation.clickable] draws a ripple that
 * overflows this tiny badge's rounded corners oddly at this size -
 * suppressing the indication (not the click itself) keeps the badge
 * looking like a clean pill instead of a smeared ripple.
 */
@Composable
private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    return this.then(
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        ),
    )
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
