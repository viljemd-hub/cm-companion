package si.apartmamatevz.cmcompanion.ui.reservations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import si.apartmamatevz.cmcompanion.bridge.Reservation
import si.apartmamatevz.cmcompanion.data.InstallationConnection
import si.apartmamatevz.cmcompanion.ui.theme.CmCard
import si.apartmamatevz.cmcompanion.ui.theme.CmColors
import si.apartmamatevz.cmcompanion.ui.today.AUTO_REFRESH_INTERVAL_MS

/**
 * Phase 1 of the NFC keycard gateway idea (2026-11-06) - just a picker,
 * soonest-arrival-first (matches the server's own sort order). Tapping a
 * row does nothing yet; phase 2 opens a reservation-actions screen here
 * (NFC scan / manual tag entry / door+EV toggle), once the Bridge action
 * endpoint exists server-side.
 */
@Composable
fun ReservationsScreen(
    connection: InstallationConnection,
    viewModel: ReservationsViewModel,
) {
    LaunchedEffect(connection.id) {
        while (true) {
            viewModel.load(connection)
            delay(AUTO_REFRESH_INTERVAL_MS)
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
            viewModel.isLoading && viewModel.reservations.isEmpty() -> CircularProgressIndicator()
            viewModel.errorMessage != null -> Text("Could not load reservations: ${viewModel.errorMessage}")
            viewModel.reservations.isEmpty() -> Text("No upcoming reservations.", color = CmColors.TextFaint)
            else -> viewModel.reservations.forEach { reservation ->
                ReservationRow(reservation)
            }
        }
    }
}

@Composable
private fun ReservationRow(reservation: Reservation) {
    CmCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${reservation.unit} — ${reservation.from} → ${reservation.to}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CmColors.TextPrimary,
            )
            val guestLine = buildString {
                if (reservation.guestName.isNotBlank()) append(reservation.guestName)
                if (reservation.guestCount != null) {
                    if (isNotEmpty()) append(" · ")
                    append("${reservation.guestCount} guests")
                }
            }
            if (guestLine.isNotBlank()) {
                Text(guestLine, color = CmColors.TextSecondary)
            }
        }
    }
}
