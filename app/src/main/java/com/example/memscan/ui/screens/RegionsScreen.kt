package com.example.memscan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
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
import com.example.memscan.model.MemoryRegion
import com.example.memscan.ui.viewmodel.MemScanViewModel
import com.example.ui.theme.*

@Composable
fun RegionsScreen(
    viewModel: MemScanViewModel,
    modifier: Modifier = Modifier,
    onNavigateToHex: (Long) -> Unit
) {
    val regions by viewModel.memoryRegions.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredRegions = remember(regions, selectedFilter) {
        when (selectedFilter) {
            "RW" -> regions.filter { it.isReadable && it.isWritable }
            "CODE (RX)" -> regions.filter { it.isExecutable }
            "HEAP" -> regions.filter { it.pathname.contains("heap", ignoreCase = true) }
            "ANON" -> regions.filter { it.pathname.isEmpty() || it.pathname.contains("anon", ignoreCase = true) }
            "LIBS" -> regions.filter { it.pathname.endsWith(".so") }
            else -> regions
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filter Chips
        val filterOptions = listOf("ALL", "RW", "CODE (RX)", "HEAP", "ANON", "LIBS")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filterOptions) { filter ->
                val isSelected = filter == selectedFilter
                Surface(
                    onClick = { selectedFilter = filter },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) ImmersivePrimary else ImmersiveSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) ImmersivePrimary else ImmersiveBorder
                    )
                ) {
                    Text(
                        text = filter,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) ImmersiveOnPrimary else ImmersiveTextMuted,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Regions Container
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ImmersiveSurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ImmersiveSurface.copy(alpha = 0.7f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredRegions.size} MEMORY REGIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveTextMuted,
                        letterSpacing = 0.5.sp
                    )
                    IconButton(
                        onClick = { viewModel.loadMemoryRegions() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload",
                            tint = ImmersivePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(color = ImmersiveBorder, thickness = 1.dp)

                if (filteredRegions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No memory regions match current filter",
                            color = ImmersiveTextMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredRegions, key = { it.startAddress }) { region ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Address Range & Name
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = region.formattedRange,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ImmersivePrimary
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = when {
                                                region.isExecutable -> ImmersivePrimaryContainer
                                                region.isWritable -> ImmersiveSuccess.copy(alpha = 0.2f)
                                                else -> ImmersiveSurfaceHigh
                                            }
                                        ) {
                                            Text(
                                                text = region.permissions,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when {
                                                    region.isExecutable -> ImmersiveOnPrimaryContainer
                                                    region.isWritable -> ImmersiveSuccess
                                                    else -> ImmersiveTextMuted
                                                },
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = if (region.pathname.isEmpty()) "[anonymous]" else region.pathname.split("/").last(),
                                        fontSize = 12.sp,
                                        color = ImmersiveText
                                    )

                                    Text(
                                        text = "Size: ${region.formattedSize} | Tag: ${region.tag}",
                                        fontSize = 10.sp,
                                        color = ImmersiveTextMuted
                                    )
                                }

                                // Quick Inspect Action
                                Button(
                                    onClick = { onNavigateToHex(region.startAddress) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ImmersiveSurfaceHigh,
                                        contentColor = ImmersiveText
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = null,
                                        tint = ImmersivePrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("HEX", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            HorizontalDivider(color = ImmersiveBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}
