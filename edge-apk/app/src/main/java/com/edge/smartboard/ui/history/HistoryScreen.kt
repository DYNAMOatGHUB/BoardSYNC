package com.edge.smartboard.ui.history

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edge.smartboard.models.*
import com.edge.smartboard.theme.*
import com.edge.smartboard.ui.dashboard.GlassCard
import com.edge.smartboard.ui.dashboard.SectionHeader
import com.edge.smartboard.viewmodel.SessionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onSessionClick: (String) -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle(emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("Date") }
    var filterStatus by remember { mutableStateOf<SessionStatus?>(null) }

    val filtered = sessions.filter { s ->
        (searchQuery.isEmpty() ||
                s.teacherName.contains(searchQuery, ignoreCase = true) ||
                s.subject.contains(searchQuery, ignoreCase = true)) &&
                (filterStatus == null || s.status == filterStatus)
    }.let { list ->
        when (sortBy) {
            "Score"    -> list.sortedByDescending { it.score }
            "Duration" -> list.sortedByDescending { it.durationSeconds }
            else       -> list.sortedByDescending { it.date }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header + search
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Session History",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by teacher, subject...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    { IconButton({ searchQuery = "" }) { Icon(Icons.Default.Clear, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor     = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor   = MaterialTheme.colorScheme.onSurface,
                    cursorColor          = MaterialTheme.colorScheme.primary
                ),
                singleLine = true
            )

            // Sort + Filter chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Date", "Score", "Duration").forEach { sort ->
                    FilterChip(
                        selected = sortBy == sort,
                        onClick  = { sortBy = sort },
                        label    = { Text(sort) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor     = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        // Summary
        if (sessions.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${filtered.size} session(s) found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Session list
        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.VideoLibrary, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No sessions found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Start a new capture session to see it here", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered) { session ->
                    SessionCard(session, onClick = { onSessionClick(session.sessionId) })
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun SessionCard(session: Session, onClick: () -> Unit) {
    val statusColor = when (session.status) {
        SessionStatus.COMPLETED  -> EmeraldGreen
        SessionStatus.UPLOADING  -> NeonBlue
        SessionStatus.PROCESSING -> AccentGold
        SessionStatus.FAILED     -> AccentRed
        SessionStatus.UPLOADED   -> NeonPurple
        else                     -> TextDisabled
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        borderColor = statusColor.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(NeonBlue.copy(0.3f), NeonPurple.copy(0.3f)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    session.teacherName.take(2).uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(session.teacherName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text(
                    "${session.subject} • ${session.classRoom}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(session.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                session.score?.let { score ->
                    Text(
                        "%.0f%%".format(score),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        session.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                val mins = session.durationSeconds / 60
                val secs = session.durationSeconds % 60
                Text(
                    "%02d:%02d".format(mins, secs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
