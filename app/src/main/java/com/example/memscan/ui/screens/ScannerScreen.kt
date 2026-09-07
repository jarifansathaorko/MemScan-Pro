package com.example.memscan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memscan.model.*
import com.example.memscan.ui.components.EditValueDialog
import com.example.memscan.ui.viewmodel.MemScanViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: MemScanViewModel,
    modifier: Modifier = Modifier,
    onNavigateToHex: (Long) -> Unit,
    onNavigateToInspector: (Long) -> Unit = {}
) {
    val isSmartScan by viewModel.isSmartScan.collectAsState()
    val selectedRegionFilter by viewModel.selectedRegionFilter.collectAsState()
    val selectedDataType by viewModel.selectedDataType.collectAsState()
    val selectedScanMode by viewModel.selectedScanMode.collectAsState()
    val searchValue by viewModel.searchValue.collectAsState()
    val scanResults by viewModel.scanResults.collectAsState()
    val scanCount by viewModel.scanCount.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()

    var showDataTypeMenu by remember { mutableStateOf(false) }
    var showScanModeMenu by remember { mutableStateOf(false) }
    var showRegionFilterMenu by remember { mutableStateOf(false) }

    // Dialog state
    var selectedResultForEdit by remember { mutableStateOf<ScanResult?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Smart Multi-Strategy Scan Banner & Switch
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isSmartScan) ImmersivePrimary.copy(alpha = 0.12f) else ImmersiveSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSmartScan) ImmersivePrimary.copy(alpha = 0.6f) else ImmersiveBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = if (isSmartScan) ImmersivePrimary else ImmersiveTextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = if (isSmartScan) "SMART MULTI-STRATEGY SCAN" else "STANDARD SCAN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isSmartScan) ImmersivePrimary else ImmersiveText
                        )
                        Text(
                            text = if (isSmartScan) "Int32, Float, ASCII, UTF-16, 12.5K & Clusters" else "Single primitive type search",
                            fontSize = 10.sp,
                            color = ImmersiveTextMuted
                        )
                    }
                }

                Switch(
                    checked = isSmartScan,
                    onCheckedChange = { viewModel.toggleSmartScan() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ImmersiveOnPrimary,
                        checkedTrackColor = ImmersivePrimary
                    )
                )
            }
        }

        // Region Filter & Data Type Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Region Filter Card
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    onClick = { showRegionFilterMenu = true },
                    shape = RoundedCornerShape(14.dp),
                    color = ImmersiveSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "REGION FILTER",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveTextMuted,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedRegionFilter.displayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ImmersivePrimary
                            )
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = ImmersiveTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = showRegionFilterMenu,
                    onDismissRequest = { showRegionFilterMenu = false },
                    modifier = Modifier.background(ImmersiveSurface)
                ) {
                    RegionFilter.values().forEach { filter ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = filter.displayName,
                                        color = if (filter == selectedRegionFilter) ImmersivePrimary else ImmersiveText,
                                        fontWeight = if (filter == selectedRegionFilter) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = filter.description,
                                        color = ImmersiveTextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            },
                            onClick = {
                                viewModel.setRegionFilter(filter)
                                showRegionFilterMenu = false
                            }
                        )
                    }
                }
            }

            // Data Type or Mode
            if (!isSmartScan) {
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        onClick = { showDataTypeMenu = true },
                        shape = RoundedCornerShape(14.dp),
                        color = ImmersiveSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "DATA TYPE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveTextMuted,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedDataType.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ImmersiveText
                                )
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = ImmersiveTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = showDataTypeMenu,
                        onDismissRequest = { showDataTypeMenu = false },
                        modifier = Modifier.background(ImmersiveSurface)
                    ) {
                        DataType.values().forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = type.displayName,
                                        color = if (type == selectedDataType) ImmersivePrimary else ImmersiveText,
                                        fontWeight = if (type == selectedDataType) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    viewModel.setDataType(type)
                                    showDataTypeMenu = false
                                }
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ImmersiveSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "ACCELERATION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveTextMuted,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Auto-Parallel (~1.1s)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ImmersiveSuccess
                        )
                    }
                }
            }
        }

        // Search Input & Action Bar
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ImmersiveSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchValue,
                    onValueChange = { viewModel.setSearchValue(it) },
                    placeholder = {
                        Text(
                            text = if (isSmartScan) "Enter analytics (12450, 12.5K, $31.42)..." else "Search value...",
                            color = ImmersiveTextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedTextColor = ImmersiveText,
                        unfocusedTextColor = ImmersiveText
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                if (scanCount > 0) {
                    IconButton(
                        onClick = { viewModel.resetScan() },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Scan",
                            tint = ImmersiveTextMuted
                        )
                    }
                }

                Button(
                    onClick = {
                        if (scanCount == 0) viewModel.startFirstScan() else viewModel.startNextScan()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImmersivePrimary,
                        contentColor = ImmersiveOnPrimary
                    ),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        text = if (scanCount == 0) "FIRST SCAN" else "NEXT SCAN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Scan Progress Indicator
        AnimatedVisibility(visible = isScanning) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ImmersiveSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${scanProgress?.currentStrategy ?: "Scanning"} ${scanProgress?.currentRegion ?: ""}...",
                            fontSize = 11.sp,
                            color = ImmersivePrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        TextButton(
                            onClick = { viewModel.cancelScan() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("CANCEL", color = ImmersiveError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { scanProgress?.fraction ?: 0f },
                        color = ImmersivePrimary,
                        trackColor = ImmersiveSurfaceDark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }

        // Results Container
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ImmersiveSurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Table Header Bar with Freeze All Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ImmersiveSurface.copy(alpha = 0.7f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FOUND: %,d CANDIDATES".format(scanResults.size),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveTextMuted,
                        letterSpacing = 0.5.sp
                    )

                    if (scanResults.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.freezeAllCandidates("Auto-Lock $searchValue") },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ImmersivePrimary.copy(alpha = 0.2f),
                                contentColor = ImmersivePrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.AcUnit, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("FREEZE ALL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = ImmersiveBorder, thickness = 1.dp)

                if (scanResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = ImmersiveTextMuted.copy(alpha = 0.5f),
                                modifier = Modifier.size(42.dp)
                            )
                            Text(
                                text = if (scanCount == 0) "Enter analytics value above & tap First Scan" else "No matches found. Try relaxing region filter.",
                                color = ImmersiveTextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(scanResults.take(1000), key = { it.address }) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedResultForEdit = item }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Address + Badges
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = item.addressHex,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ImmersivePrimary
                                        )

                                        // Strategy Badge
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = ImmersiveSurfaceHigh
                                        ) {
                                            Text(
                                                text = item.strategy.badge,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ImmersivePrimary,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }

                                        // Confidence Badge
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (item.confidence >= 85) ImmersiveSuccess.copy(alpha = 0.2f) else ImmersiveWarning.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "${item.confidence}%",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (item.confidence >= 85) ImmersiveSuccess else ImmersiveWarning,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }

                                        // Region tag
                                        if (item.regionTag.isNotEmpty()) {
                                            Text(
                                                text = item.regionTag,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                color = ImmersiveTextMuted
                                            )
                                        }
                                    }

                                    // Formatted Value
                                    Text(
                                        text = item.currentFormatted,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ImmersiveText
                                    )
                                }

                                if (item.clusterGroup != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "📍 ${item.clusterGroup}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ImmersiveSecondary
                                    )
                                }

                                // Quick Actions Bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            viewModel.addToWatchlist(
                                                address = item.address,
                                                label = "Lock ${item.currentFormatted}",
                                                dataType = item.dataType,
                                                freezeImmediately = true
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.AcUnit, null, modifier = Modifier.size(13.dp), tint = ImmersivePrimary)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("FREEZE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ImmersivePrimary)
                                    }

                                    TextButton(
                                        onClick = {
                                            viewModel.inspectAddress(item.address)
                                            onNavigateToInspector(item.address)
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.AccountTree, null, modifier = Modifier.size(13.dp), tint = ImmersiveSecondary)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("STRUCT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ImmersiveSecondary)
                                    }

                                    TextButton(
                                        onClick = { onNavigateToHex(item.address) },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Code, null, modifier = Modifier.size(13.dp), tint = ImmersiveTextMuted)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("HEX", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ImmersiveTextMuted)
                                    }
                                }
                            }
                            HorizontalDivider(color = ImmersiveBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }

    // Edit Value Dialog
    selectedResultForEdit?.let { res ->
        EditValueDialog(
            address = res.address,
            currentValueStr = res.currentFormatted,
            initialDataType = res.dataType,
            onWriteValue = { addr, newVal, dType ->
                viewModel.writeMemoryValue(addr, newVal, dType)
            },
            onAddToWatchlist = { addr, desc, dType, freeze ->
                viewModel.addToWatchlist(addr, desc, dType, freeze)
            },
            onDismiss = { selectedResultForEdit = null }
        )
    }
}
