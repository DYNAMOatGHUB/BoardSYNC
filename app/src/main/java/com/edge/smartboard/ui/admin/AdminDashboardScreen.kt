package com.edge.smartboard.ui.admin

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import com.edge.smartboard.theme.*
import com.edge.smartboard.ui.dashboard.GlassCard
import com.edge.smartboard.ui.dashboard.SectionHeader
import com.edge.smartboard.ui.dashboard.WeeklyBarChart
import com.edge.smartboard.viewmodel.DashboardViewModel

@Composable
fun AdminDashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val admin   by viewModel.adminDashboard.collectAsStateWithLifecycle()
    val metrics by viewModel.systemMetrics.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AdminPanelSettings, null, tint = NeonPurple, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Admin Dashboard", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("System Overview", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        // KPI row
        SectionHeader("Key Metrics")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item { AdminKpiCard("Total Sessions",    "${admin.totalSessions}",          Icons.Default.VideoLibrary,  NeonBlue) }
            item { AdminKpiCard("Today's Sessions",  "${admin.todaySessions}",           Icons.Default.Today,         EmeraldGreen) }
            item { AdminKpiCard("Avg Score",         "%.1f%%".format(admin.avgTeachingScore), Icons.Default.Stars,  NeonPurple) }
            item { AdminKpiCard("Upload Success",    "%.1f%%".format(admin.uploadSuccessRate), Icons.Default.CloudDone, AccentGold) }
            item { AdminKpiCard("Storage Used",      "%.1f GB".format(admin.storageUsedGb), Icons.Default.Storage,  AccentOrange) }
        }

        // Resource utilization
        SectionHeader("Resource Utilization")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminResourceBar("GPU Utilization", admin.gpuUtilization, NeonPurple)
                AdminResourceBar("CPU Utilization", admin.cpuUtilization, NeonBlue)
                AdminResourceBar("Storage Used",
                    (admin.storageUsedGb / 1000f * 100f).coerceAtMost(100f), AccentGold)
            }
        }

        // Weekly chart
        SectionHeader("Weekly Teaching Scores")
        WeeklyBarChart(admin.weeklyData)

        // Monthly trend
        SectionHeader("Monthly Trend")
        MonthlyTrendCard(admin.monthlyData)

        // Department comparison
        SectionHeader("Department Performance")
        DepartmentComparisonCard(admin.departmentData)

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun AdminKpiCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    GlassCard(modifier = Modifier.width(160.dp), borderColor = color.copy(0.3f)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
fun AdminResourceBar(label: String, value: Float, color: Color) {
    val animVal by animateFloatAsState(value / 100f, tween(800), label = "res")
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text("%.1f%%".format(value), style = MaterialTheme.typography.labelLarge, color = color)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(CardDarker)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animVal)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .background(Brush.horizontalGradient(listOf(color.copy(0.5f), color)))
            )
        }
    }
}

@Composable
fun MonthlyTrendCard(data: List<Float>) {
    val months = listOf("Apr", "May", "Jun", "Jul")
    val max = data.maxOrNull() ?: 100f

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEachIndexed { i, value ->
                val animH by animateFloatAsState(
                    (value / max),
                    tween(800, delayMillis = i * 100),
                    label = "m"
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%.0f".format(value), style = MaterialTheme.typography.labelSmall, color = NeonPurple)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height((100 * animH).dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(Brush.verticalGradient(listOf(NeonPurple, NeonBlue)))
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(months.getOrElse(i) { "" }, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun DepartmentComparisonCard(data: Map<String, Float>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            data.entries.forEachIndexed { i, (dept, score) ->
                val colors = listOf(NeonBlue, EmeraldGreen, NeonPurple, AccentGold)
                val color  = colors[i % colors.size]
                AdminResourceBar("$dept Department", score, color)
            }
        }
    }
}
