package si.apartmamatevz.cmcompanion.ui.inquiries

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import si.apartmamatevz.cmcompanion.bridge.Inquiry
import si.apartmamatevz.cmcompanion.data.InstallationConnection

/**
 * Expandable list, not a separate detail screen (2026-09-03 decision) -
 * tapping a row expands it in place with the extra guest-count/country
 * breakdown and the single Accept action. "Back" is just collapsing the
 * row / returning to Today - see InquiriesViewModel.toggleExpanded() for
 * why expanding alone already marks it seen.
 */
@Composable
fun InquiriesScreen(
    connection: InstallationConnection,
    viewModel: InquiriesViewModel,
    initialExpandedId: String? = null,
    onDone: () -> Unit,
) {
    LaunchedEffect(connection.id) { viewModel.load(connection) }

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
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when {
            viewModel.isLoading && viewModel.inquiries.isEmpty() -> CircularProgressIndicator()
            viewModel.errorMessage != null -> Text("Could not load inquiries: ${viewModel.errorMessage}")
            viewModel.inquiries.isEmpty() -> Text("No pending inquiries.")
            else -> viewModel.inquiries.forEach { inquiry ->
                InquiryRow(
                    inquiry = inquiry,
                    expanded = viewModel.expandedId == inquiry.id,
                    accepting = viewModel.acceptingId == inquiry.id,
                    onToggle = { viewModel.toggleExpanded(inquiry.id) },
                    onAccept = { viewModel.accept(connection, inquiry.id, onDone) },
                    onBack = onDone,
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
    onToggle: () -> Unit,
    onAccept: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "${inquiry.unit} — ${inquiry.from} → ${inquiry.to} (${inquiry.nights} nights)",
            fontWeight = FontWeight.Bold,
        )

        if (expanded) {
            val guestParts = buildList {
                if (inquiry.adults > 0) add("${inquiry.adults} adults")
                if (inquiry.kids06 > 0) add("${inquiry.kids06} kids (0-6)")
                if (inquiry.kids712 > 0) add("${inquiry.kids712} kids (7-12)")
            }
            Text(if (guestParts.isEmpty()) "Guest count unavailable" else guestParts.joinToString(" · "))
            Text("Guest country: ${inquiry.guestPhoneCountry ?: "unknown"}")

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
                }
            }
        }
    }
}
