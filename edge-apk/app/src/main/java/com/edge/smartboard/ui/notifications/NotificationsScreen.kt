package com.edge.smartboard.ui.notifications

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edge.smartboard.models.*
import com.edge.smartboard.theme.*
import com.edge.smartboard.ui.dashboard.GlassCard
import com.edge.smartboard.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationsScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Notifications",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = viewModel::markAllRead) {
                    Text("Mark all read", color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                "${notifications.count { !it.isRead }} unread",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(notifications) { notif ->
                NotificationCard(notif)
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun NotificationCard(notif: AppNotification) {
    val (color, icon) = when (notif.type) {
        NotificationType.AI_FINISHED    -> EmeraldGreen to Icons.Default.Psychology
        NotificationType.REPORT_READY   -> NeonBlue     to Icons.Default.Description
        NotificationType.UPLOAD_FAILED  -> AccentRed    to Icons.Default.CloudOff
        NotificationType.QUEUE_FULL     -> AccentOrange to Icons.Default.Queue
        NotificationType.LOW_CONFIDENCE -> AccentGold   to Icons.Default.Warning
        NotificationType.NETWORK_LOST   -> AccentRed    to Icons.Default.WifiOff
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (!notif.isRead) color.copy(0.5f) else GlassBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (!notif.isRead) color.copy(0.04f) else Color.Transparent)
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(notif.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(notif.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    SimpleDateFormat("HH:mm • dd MMM", Locale.getDefault()).format(Date(notif.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!notif.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}
