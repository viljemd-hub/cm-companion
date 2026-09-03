package si.apartmamatevz.cmcompanion.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import si.apartmamatevz.cmcompanion.bridge.AttentionItem
import si.apartmamatevz.cmcompanion.bridge.AttentionKind
import si.apartmamatevz.cmcompanion.bridge.TodayDashboard
import si.apartmamatevz.cmcompanion.bridge.TodayStay
import si.apartmamatevz.cmcompanion.data.InstallationConnection

/**
 * v0.1 "host situational awareness" screen - see docs/architecture.md and
 * the 2026-09-03 design review recorded in pro_dev_roadmap memory:
 * "Currently hosting" ranks high (host thinks "who's here", not
 * "what does my occupancy model say"), guest counts are shown as an
 * aggregate headcount, never a name. Attention sits at the very top -
 * "in 5 seconds, is everything OK and what needs me" is the screen's job,
 * not a full admin dashboard.
 */
@Composable
fun TodayScreen(connection: InstallationConnection, viewModel: TodayViewModel, onInquiryClick: (String) -> Unit) {
    LaunchedEffect(connection.id) { viewModel.load(connection) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        when {
            viewModel.isLoading && viewModel.dashboard == null -> CircularProgressIndicator()
            viewModel.errorMessage != null -> Text("Could not load today: ${viewModel.errorMessage}")
            viewModel.dashboard != null -> TodayContent(viewModel.dashboard!!, viewModel.attention, onInquiryClick)
        }
    }
}

@Composable
private fun TodayContent(dashboard: TodayDashboard, attention: List<AttentionItem>, onInquiryClick: (String) -> Unit) {
    val hostingStays = dashboard.units.mapNotNull { it.currentlyHosting?.let { s -> it.unit to s } }
    val arrivalStays = dashboard.units.flatMap { u -> u.arrivals.map { u.unit to it } }
    val departureStays = dashboard.units.flatMap { u -> u.departures.map { u.unit to it } }

    AttentionSection(attention, onInquiryClick)

    Section(
        title = "Currently hosting",
        summary = "${dashboard.totals.hostingNow} reservation(s) · ${dashboard.totals.hostingNowGuests} guests",
    ) {
        if (hostingStays.isEmpty()) {
            Text("No one in-house today.")
        } else {
            hostingStays.forEach { (unit, stay) -> StayRow(unit, stay, dateLabel = "until") }
        }
    }

    Section(
        title = "Arrivals today",
        summary = "${dashboard.totals.arrivals} arrival(s) · ${dashboard.totals.arrivalGuests} guests",
    ) {
        if (arrivalStays.isEmpty()) {
            Text("No arrivals today.")
        } else {
            arrivalStays.forEach { (unit, stay) -> StayRow(unit, stay) }
        }
    }

    Section(
        title = "Departures today",
        summary = "${dashboard.totals.departures} departure(s) · ${dashboard.totals.departureGuests} guests",
    ) {
        if (departureStays.isEmpty()) {
            Text("No departures today.")
        } else {
            departureStays.forEach { (unit, stay) -> StayRow(unit, stay) }
        }
    }
}

@Composable
private fun AttentionSection(items: List<AttentionItem>, onInquiryClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            if (items.isEmpty()) "All clear" else "${items.size} item(s) need attention",
            fontWeight = FontWeight.Bold,
        )
        items.forEach { item ->
            val marker = if (item.kind == AttentionKind.INQUIRY) "●" else "○"
            val rowModifier = if (item.kind == AttentionKind.INQUIRY) {
                Modifier.clickable { onInquiryClick(item.id) }
            } else {
                Modifier
            }
            Text("$marker ${item.title} — ${item.unit} — ${item.detail}", modifier = rowModifier)
        }
    }
}

@Composable
private fun Section(title: String, summary: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontWeight = FontWeight.Bold)
        Text(summary)
        content()
    }
}

@Composable
private fun StayRow(unit: String, stay: TodayStay, dateLabel: String? = null) {
    val guestText = stay.guestCount?.let { "$it guests" } ?: "guest count unavailable"
    val tail = if (dateLabel != null && stay.checkout != null) " · $dateLabel ${stay.checkout}" else ""
    Text("$unit — $guestText$tail")
}
