package si.apartmamatevz.cmcompanion.ui.inquiries

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import si.apartmamatevz.cmcompanion.bridge.Inquiry
import si.apartmamatevz.cmcompanion.data.InstallationConnection
import si.apartmamatevz.cmcompanion.ui.theme.CmCard
import si.apartmamatevz.cmcompanion.ui.theme.CmColors
import si.apartmamatevz.cmcompanion.ui.today.AUTO_REFRESH_INTERVAL_MS

/**
 * Expandable list, not a separate detail screen (2026-09-03 decision) -
 * tapping a row expands it in place with the extra guest-count/country
 * breakdown and the single Accept action. "Back" is just collapsing the
 * row / returning to Today - see InquiriesViewModel.toggleExpanded() for
 * why expanding alone already marks it seen.
 *
 * Auto-refreshes every AUTO_REFRESH_INTERVAL_MS while visible, same as
 * Today/Alerts (2026-09-17) - load() only replaces the inquiry list and
 * seen-state, never expandedId/acceptingId, so a background refresh
 * can't collapse a row the host is mid-way reading or accepting.
 */
@Composable
fun InquiriesScreen(
    connection: InstallationConnection,
    viewModel: InquiriesViewModel,
    initialExpandedId: String? = null,
    onDone: () -> Unit,
) {
    LaunchedEffect(connection.id) {
        while (true) {
            viewModel.load(connection)
            delay(AUTO_REFRESH_INTERVAL_MS)
        }
    }

    // Arriving here from an Attention tap should land already expanded on
    // the inquiry the host tapped - fixes a real 2026-09-03 report ("klik
    // peljal na inquiries" but didn't expand it). Guarded so a later
    // recomposition (e.g. after Accept removes a row) doesn't re-expand.
    LaunchedEffect(viewModel.inquiries, initialExpandedId) {
        if (initialExpandedId != null && viewModel.expandedId == null &&
            viewModel.inquiries.any { it.id == initialExpandedId }
        ) {
            viewModel.toggleExpanded(initialExpandedId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            viewModel.isLoading && viewModel.inquiries.isEmpty() -> CircularProgressIndicator()
            viewModel.errorMessage != null -> Text("Could not load inquiries: ${viewModel.errorMessage}")
            viewModel.inquiries.isEmpty() -> Text("No pending inquiries.", color = CmColors.TextFaint)
            else -> viewModel.inquiries.forEach { inquiry ->
                InquiryRow(
                    inquiry = inquiry,
                    expanded = viewModel.expandedId == inquiry.id,
                    accepting = viewModel.acceptingId == inquiry.id,
                    seen = viewModel.isSeen(inquiry.id),
                    onToggle = { viewModel.toggleExpanded(inquiry.id) },
                    onAccept = { viewModel.accept(connection, inquiry.id, onDone) },
                    onBack = onDone,
                    onUnmarkSeen = { viewModel.unmarkSeen(inquiry.id) },
                )
            }
        }
    }
}

@Composable
private fun InquiryRow(
    inquiry: Inquiry,
    expanded: Boolean,
    accepting: Boolean,
    seen: Boolean,
    onToggle: () -> Unit,
    onAccept: () -> Unit,
    onBack: () -> Unit,
    onUnmarkSeen: () -> Unit,
) {
    CmCard(modifier = Modifier.clickable(onClick = onToggle)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Visible flag state (2026-09-03: "kaj pa če bi bile zastavice
        // vidne?") - a seen inquiry shows a marker even collapsed, so the
        // host can spot at a glance what still needs a fresh look.
        val marker = if (seen) "✓ " else "🚩 "
        Text(
            "$marker${inquiry.unit} — ${inquiry.from} → ${inquiry.to} (${inquiry.nights} nights)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (seen) CmColors.TextMuted else CmColors.TextPrimary,
        )

        if (expanded) {
            val guestParts = buildList {
                if (inquiry.adults > 0) add("${inquiry.adults} adults")
                if (inquiry.kids06 > 0) add("${inquiry.kids06} kids (0-6)")
                if (inquiry.kids712 > 0) add("${inquiry.kids712} kids (7-12)")
            }
            Text(if (guestParts.isEmpty()) "Guest count unavailable" else guestParts.joinToString(" · "), color = CmColors.TextSecondary)
            Text("Guest country: ${inquiry.guestPhoneCountry ?: "unknown"}", color = CmColors.TextSecondary)

            if (accepting) {
                CircularProgressIndicator()
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = onAccept, modifier = Modifier.fillMaxWidth()) {
                        Text("Accept")
                    }
                    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                        Text("Back")
                    }
                    // Only useful once seen (that's what put it in this
                    // "handled" state to begin with) - manual escape hatch
                    // back onto Today's Attention list, not automatic.
                    if (seen) {
                        OutlinedButton(onClick = onUnmarkSeen, modifier = Modifier.fillMaxWidth()) {
                            Text("Remind me again (show in Attention)")
                        }
                    }
                }
            }
        }
        }
    }
}
