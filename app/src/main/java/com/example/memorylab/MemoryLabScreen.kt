package com.example.memorylab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*

@Composable
fun MemoryLabScreen(
    modifier: Modifier = Modifier,
    viewModel: MemoryLabViewModel = viewModel(),
    onSwitchToScanner: () -> Unit = {}
) {
    val views by viewModel.views.collectAsState()
    val subscribers by viewModel.subscribers.collectAsState()
    val revenue by viewModel.revenue.collectAsState()
    val watchTime by viewModel.watchTime.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Target Header Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ImmersiveSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                                .background(ImmersivePrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                tint = ImmersiveOnPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "MemoryLab Test Target",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveText
                            )
                            Text(
                                text = "com.example.memorylab • PID 10542 • Debuggable",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = ImmersivePrimary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ImmersiveSuccess.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "ACTIVE TARGET",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveSuccess,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "This controlled target stores genuine in-memory variables. Test your scanner by searching for these values, incrementing them, running 'Next Scan', and editing them from MemScan Pro!",
                    fontSize = 12.sp,
                    color = ImmersiveTextMuted,
                    lineHeight = 16.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onSwitchToScanner,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ImmersivePrimary,
                            contentColor = ImmersiveOnPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Scanner", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.reset() },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = ImmersiveText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset All", color = ImmersiveText, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Section Title
        Text(
            text = "CONTROLLED MEMORY VARIABLES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ImmersiveTextMuted,
            letterSpacing = 1.sp
        )

        // 1. Views Card (Int32)
        MemoryVariableCard(
            title = "Views",
            dataTypeLabel = "Int32 (4 Bytes)",
            addressHex = "0x%08X".format(MemoryLabTarget.ADDR_VIEWS),
            currentValue = "%,d".format(views),
            rawNumeric = views.toString(),
            icon = Icons.Default.Visibility,
            onIncrement = { viewModel.incrementViews(1) },
            incrementLabel = "+1 View",
            onSecondaryIncrement = { viewModel.incrementViews(100) },
            secondaryLabel = "+100"
        )

        // 2. Subscribers Card (Int32)
        MemoryVariableCard(
            title = "Subscribers",
            dataTypeLabel = "Int32 (4 Bytes)",
            addressHex = "0x%08X".format(MemoryLabTarget.ADDR_SUBSCRIBERS),
            currentValue = "%,d".format(subscribers),
            rawNumeric = subscribers.toString(),
            icon = Icons.Default.People,
            onIncrement = { viewModel.incrementSubscribers(1) },
            incrementLabel = "+1 Sub",
            onSecondaryIncrement = { viewModel.incrementSubscribers(10) },
            secondaryLabel = "+10"
        )

        // 3. Revenue Card (Float)
        MemoryVariableCard(
            title = "Revenue",
            dataTypeLabel = "Float32 (IEEE 754)",
            addressHex = "0x%08X".format(MemoryLabTarget.ADDR_REVENUE),
            currentValue = "$%.2f".format(revenue),
            rawNumeric = String.format(java.util.Locale.US, "%.2f", revenue),
            icon = Icons.Default.MonetizationOn,
            onIncrement = { viewModel.incrementRevenue(1.0f) },
            incrementLabel = "+$1.00",
            onSecondaryIncrement = { viewModel.incrementRevenue(10.0f) },
            secondaryLabel = "+$10.00"
        )

        // 4. Watch Time Card (Double)
        MemoryVariableCard(
            title = "Watch Time",
            dataTypeLabel = "Float64 / Double",
            addressHex = "0x%08X".format(MemoryLabTarget.ADDR_WATCH_TIME),
            currentValue = "%.1f hrs".format(watchTime),
            rawNumeric = String.format(java.util.Locale.US, "%.1f", watchTime),
            icon = Icons.Default.Schedule,
            onIncrement = { viewModel.incrementWatchTime(1.0) },
            incrementLabel = "+1.0 hr",
            onSecondaryIncrement = { viewModel.incrementWatchTime(5.0) },
            secondaryLabel = "+5.0 hrs"
        )
    }
}

@Composable
fun MemoryVariableCard(
    title: String,
    dataTypeLabel: String,
    addressHex: String,
    currentValue: String,
    rawNumeric: String,
    icon: ImageVector,
    onIncrement: () -> Unit,
    incrementLabel: String,
    onSecondaryIncrement: () -> Unit,
    secondaryLabel: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ImmersiveSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(ImmersiveSurfaceDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = ImmersivePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = ImmersiveText
                        )
                        Text(
                            text = dataTypeLabel,
                            fontSize = 11.sp,
                            color = ImmersiveTextMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ImmersiveSurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = addressHex,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersivePrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // Live Display Value
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ImmersiveSurfaceDark, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LIVE VALUE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveTextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = currentValue,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveText
                    )
                }

                Text(
                    text = "Raw: $rawNumeric",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = ImmersiveTextMuted
                )
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onIncrement,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImmersivePrimaryContainer,
                        contentColor = ImmersiveOnPrimaryContainer
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(incrementLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onSecondaryIncrement,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(secondaryLabel, color = ImmersiveText, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }
    }
}
