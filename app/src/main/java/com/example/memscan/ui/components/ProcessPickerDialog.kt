package com.example.memscan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.memscan.model.ProcessInfo
import com.example.ui.theme.*

@Composable
fun ProcessPickerDialog(
    processes: List<ProcessInfo>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onSelectProcess: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(processes, searchQuery) {
        if (searchQuery.isBlank()) processes
        else processes.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true) ||
            it.pid.toString().contains(searchQuery)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ImmersiveBorder))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ImmersivePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = ImmersiveOnPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Select Process",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveText
                            )
                            Text(
                                text = "${processes.size} processes available",
                                style = MaterialTheme.typography.bodySmall,
                                color = ImmersiveTextMuted
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = onRefresh) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = ImmersivePrimary
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = ImmersiveTextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, package or PID...", color = ImmersiveTextMuted, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ImmersiveSurfaceDark,
                        unfocusedContainerColor = ImmersiveSurfaceDark,
                        focusedBorderColor = ImmersivePrimary,
                        unfocusedBorderColor = ImmersiveBorder,
                        focusedTextColor = ImmersiveText,
                        unfocusedTextColor = ImmersiveText
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Filter / Tab toggles
                var showOnlyApps by remember { mutableStateOf(true) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = showOnlyApps,
                        onClick = { showOnlyApps = !showOnlyApps },
                        label = { Text(if (showOnlyApps) "Filtered: User Apps" else "Showing: All (incl System)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ImmersivePrimary.copy(alpha = 0.2f),
                            selectedLabelColor = ImmersivePrimary,
                            labelColor = ImmersiveTextMuted
                        )
                    )

                    Text(
                        text = "${filtered.size} PIDs",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ImmersiveTextMuted
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ImmersivePrimary)
                    }
                } else if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No matching processes found",
                            color = ImmersiveTextMuted,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    val displayList = remember(filtered, showOnlyApps) {
                        if (showOnlyApps) filtered.filter { !it.isSystem } else filtered
                    }

                    // Group by package name
                    val grouped = remember(displayList) {
                        displayList.groupBy { it.packageName }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ImmersiveSurfaceDark),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        grouped.forEach { (pkg, procs) ->
                            val isMultiProcess = procs.size > 1

                            item(key = "header_$pkg") {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ImmersiveSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = pkg,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ImmersivePrimary
                                            )
                                            if (isMultiProcess) {
                                                Text(
                                                    text = "Multi-Process Architecture (${procs.size} PIDs)",
                                                    fontSize = 10.sp,
                                                    color = ImmersiveTextMuted
                                                )
                                            }
                                        }

                                        if (isMultiProcess) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = ImmersivePrimary.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "${procs.sumOf { it.memoryUsageMb }} MB TOTAL",
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ImmersivePrimary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            items(procs, key = { it.pid }) { proc ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = if (isMultiProcess) 12.dp else 0.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (proc.isPrimaryTarget) ImmersivePrimary.copy(alpha = 0.08f) else ImmersiveSurfaceHigh.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (proc.isPrimaryTarget) ImmersivePrimary.copy(alpha = 0.4f) else ImmersiveBorder.copy(alpha = 0.3f)
                                    ),
                                    onClick = {
                                        onSelectProcess(proc.pid)
                                        onDismiss()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (isMultiProcess) {
                                                    Text(
                                                        text = if (proc.isPrimaryTarget) "●" else "├─",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 11.sp,
                                                        color = if (proc.isPrimaryTarget) ImmersiveSuccess else ImmersiveTextMuted
                                                    )
                                                }
                                                Text(
                                                    text = proc.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = ImmersiveText
                                                )

                                                if (proc.isPrimaryTarget) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = ImmersiveSuccess.copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            text = "RECOMMENDED",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = ImmersiveSuccess,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = ImmersiveBorder
                                                ) {
                                                    Text(
                                                        text = proc.roleBadge,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ImmersiveTextMuted,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                                if (proc.memoryUsageMb > 0) {
                                                    Text(
                                                        text = "${proc.memoryUsageMb} MB",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 10.sp,
                                                        color = ImmersiveTextMuted
                                                    )
                                                }
                                                Text(
                                                    text = proc.architecture,
                                                    fontSize = 10.sp,
                                                    color = ImmersiveTextMuted
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (proc.isPrimaryTarget) ImmersivePrimary.copy(alpha = 0.2f) else ImmersiveSurfaceHigh
                                        ) {
                                            Text(
                                                text = "PID ${proc.pid}",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (proc.isPrimaryTarget) ImmersivePrimary else ImmersiveText,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
