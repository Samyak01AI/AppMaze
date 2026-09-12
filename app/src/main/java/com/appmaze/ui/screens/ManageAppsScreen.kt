package com.appmaze.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appmaze.launcher.LauncherAppItem
import com.appmaze.launcher.LauncherUiState
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAppsScreen(
    uiState: LauncherUiState,
    onNavigateBack: () -> Unit,
    onToggleDistracting: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Distracting Apps") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Distracting apps first
            val distracting = uiState.apps.filter { it.isDistracting }
            val nonDistracting = uiState.apps.filter { !it.isDistracting && !it.isEssential }
            val essential = uiState.apps.filter { it.isEssential }

            if (distracting.isNotEmpty()) {
                item {
                    Text(
                        "Distracting",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(distracting, key = { it.packageName }) { app ->
                    AppListItem(app = app, onToggle = { onToggleDistracting(app.packageName) })
                }
            }

            if (nonDistracting.isNotEmpty()) {
                item {
                    Text(
                        "Other Apps",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(nonDistracting, key = { it.packageName }) { app ->
                    AppListItem(app = app, onToggle = { onToggleDistracting(app.packageName) })
                }
            }

            if (essential.isNotEmpty()) {
                item {
                    Text(
                        "Essential (Protected)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(essential, key = { it.packageName }) { app ->
                    AppListItem(app = app, onToggle = null, isProtected = true)
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun AppListItem(
    app: LauncherAppItem,
    onToggle: (() -> Unit)?,
    isProtected: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (app.isDistracting)
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
            if (app.icon != null) {
                Image(
                    bitmap = app.icon,
                    contentDescription = "Icon for ${app.appName}",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(app.appName, style = MaterialTheme.typography.bodyLarge)
                if (app.dailyUsageMinutes > 0) {
                    val hours = app.dailyUsageMinutes / 60
                    val mins = app.dailyUsageMinutes % 60
                    val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                    Text(
                        "Today: $timeStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (app.isDistracting) {
                DistractionBadge(level = app.distractionLevel)
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (isProtected) {
                Icon(
                    Icons.Outlined.Shield,
                    contentDescription = "Protected app",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            } else if (onToggle != null) {
                Checkbox(
                    checked = app.isDistracting,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.error,
                        uncheckedColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }
    }
}

// ─── Demo Screen ─────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoScreen(
    uiState: LauncherUiState,
    onNavigateBack: () -> Unit,
    onAddUsage: (String, Long) -> Unit,
    onToggleDistracting: (String) -> Unit,
    onScrambleNow: () -> Unit,
    onResetLayout: () -> Unit,
    formatScreenTime: (Long) -> String,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Science, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Demo Mode")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Demo Controls",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Simulate usage and test scrambling without waiting for real screen time data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onScrambleNow,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Shuffle, contentDescription = null, Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scramble Now")
                    }
                    OutlinedButton(
                        onClick = onResetLayout,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = null, Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Layout")
                    }
                }
            }

            val demoApps = uiState.apps.take(10)
            items(demoApps, key = { it.packageName }) { app ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (app.icon != null) {
                                Image(
                                    bitmap = app.icon,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.appName, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Current usage: ${formatScreenTime(app.dailyUsageMinutes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (app.isDistracting) {
                                DistractionBadge(level = app.distractionLevel)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onAddUsage(app.packageName, 30) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("+30 min", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { onToggleDistracting(app.packageName) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (app.isDistracting) MaterialTheme.colorScheme.error
                                                   else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    if (app.isDistracting) "Unmark" else "Mark Distracting",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
