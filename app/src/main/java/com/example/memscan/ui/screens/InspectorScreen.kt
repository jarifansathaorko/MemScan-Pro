package com.example.memscan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memscan.model.DataType
import com.example.memscan.ui.components.EditValueDialog
import com.example.memscan.ui.viewmodel.MemScanViewModel
import com.example.ui.theme.*

private enum class InspectorSubTab(val title: String) {
    STRUCT("OBJECT STRUCT"),
    TRACER("LIVE TRACER"),
    WRITE_LOGS("WRITE TRACKER")
}

@Composable
fun InspectorScreen(
    viewModel: MemScanViewModel,
    modifier: Modifier = Modifier,
    onNavigateToHex: (Long) -> Unit
) {
    var activeSubTab by remember { mutableStateOf(InspectorSubTab.STRUCT) }
    val inspectedAddress by viewModel.inspectedAddress.collectAsState()
    val inspectedFields by viewModel.inspectedFields.collectAsState()
    val isInspecting by viewModel.isInspecting.collectAsState()

    val writeLogs by viewModel.inspector.writeLogs.collectAsState()
    val traceAddress by viewModel.inspector.traceAddress.collectAsState()
    val traceHistory by viewModel.inspector.traceHistory.collectAsState()

    var addressInput by remember { mutableStateOf(inspectedAddress?.let { "0x%08X".format(it) } ?: "0x7B42A108") }
    var selectedFieldForEdit by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sub-Tab Selector
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = ImmersiveSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InspectorSubTab.values().forEach { tab ->
                    val isSel = activeSubTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) ImmersivePrimary else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable { activeSubTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) ImmersiveOnPrimary else ImmersiveTextMuted,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        when (activeSubTab) {
            InspectorSubTab.STRUCT -> {
                // Address Target Bar
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ImmersiveSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = addressInput,
                            onValueChange = { addressInput = it },
                            label = { Text("Base Object Address", fontSize = 11.sp) },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ImmersivePrimary,
                                unfocusedBorderColor = ImmersiveBorder,
                                focusedTextColor = ImmersiveText,
                                unfocusedTextColor = ImmersiveText
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                val clean = addressInput.trim().removePrefix("0x").removePrefix("0X")
                                val addr = clean.toLongOrNull(16)
                                if (addr != null) {
                                    viewModel.inspectAddress(addr)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ImmersivePrimary,
                                contentColor = ImmersiveOnPrimary
                            ),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("DISSECT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Object Dissection Table
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ImmersiveSurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (isInspecting) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ImmersivePrimary)
                        }
                    } else if (inspectedFields.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AccountTree, null, tint = ImmersiveTextMuted, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Enter an address to dissect memory class struct", color = ImmersiveTextMuted, fontSize = 12.sp)
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ImmersiveSurface)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("OFFSET / ADDR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ImmersiveTextMuted)
                                    Text("DECODED PRIMITIVES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ImmersiveTextMuted)
                                }
                                HorizontalDivider(color = ImmersiveBorder)
                            }

                            items(inspectedFields) { field ->
                                val isCenter = field.offset == 0
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isCenter) ImmersivePrimary.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent)
                                        .clickable { selectedFieldForEdit = field.address }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = field.offsetFormatted,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCenter) ImmersivePrimary else ImmersiveTextMuted
                                            )
                                            Text(
                                                text = field.addressHex,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = ImmersiveText
                                            )
                                        }
                                        val hexStr = field.rawBytes.joinToString(" ") { "%02X".format(it) }
                                        Text(hexStr, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ImmersiveTextMuted)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        field.int32Val?.let {
                                            Text("Int32: %,d".format(it), fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ImmersiveText)
                                        }
                                        field.floatVal?.let {
                                            Text("Float: %.2f".format(it), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = ImmersiveTextMuted)
                                        }
                                        field.pointerHex?.let {
                                            Text("Ptr: $it", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ImmersiveSecondary)
                                        }
                                        field.asciiStr?.let {
                                            Text("\"$it\"", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ImmersiveSuccess)
                                        }
                                    }
                                }
                                HorizontalDivider(color = ImmersiveBorder.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }

            InspectorSubTab.TRACER -> {
                // Value Tracer Controls
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ImmersiveSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("LIVE VALUE TRACER", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ImmersivePrimary)
                            Text(
                                text = if (traceAddress != null) "Tracing 0x%08X (120ms ticks)".format(traceAddress) else "Tracer idle. Select an address.",
                                fontSize = 11.sp,
                                color = ImmersiveTextMuted
                            )
                        }

                        if (traceAddress != null) {
                            Button(
                                onClick = { viewModel.stopTracing() },
                                colors = ButtonDefaults.buttonColors(containerColor = ImmersiveError),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("STOP", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        } else {
                            Button(
                                onClick = {
                                    val clean = addressInput.trim().removePrefix("0x")
                                    clean.toLongOrNull(16)?.let { viewModel.startTracing(it, DataType.INT32) }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ImmersivePrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("TRACE INPUT", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Trace Timeline History
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ImmersiveSurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (traceHistory.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No telemetry recorded yet. Start tracer above.", color = ImmersiveTextMuted, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            items(traceHistory) { point ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "+%d ms".format(System.currentTimeMillis() - point.timestamp),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = ImmersiveTextMuted
                                    )
                                    Text(
                                        text = point.valueFormatted,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ImmersiveText
                                    )
                                }
                                HorizontalDivider(color = ImmersiveBorder.copy(alpha = 0.2f))
                            }
                        }
                    }
                }
            }

            InspectorSubTab.WRITE_LOGS -> {
                // Write Tracker Log
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ImmersiveSurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (writeLogs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No memory writes executed in this session.", color = ImmersiveTextMuted, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(writeLogs, key = { it.id }) { log ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(log.addressHex, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ImmersivePrimary)
                                            if (log.isOverwritten) {
                                                Surface(color = ImmersiveWarning.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                                    Text("OVERWRITTEN BY APP", color = ImmersiveWarning, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                }
                                            } else {
                                                Surface(color = ImmersiveSuccess.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                                    Text("HOLDING", color = ImmersiveSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                        Text(
                                            text = "Wrote: ${log.writtenFormatted} | Readback: ${log.readBackFormatted}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = if (log.isOverwritten) ImmersiveWarning else ImmersiveText
                                        )
                                    }

                                    IconButton(onClick = { onNavigateToHex(log.address) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Code, "Hex", tint = ImmersiveTextMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                                HorizontalDivider(color = ImmersiveBorder.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Edit Field Dialog
    selectedFieldForEdit?.let { addr ->
        EditValueDialog(
            address = addr,
            currentValueStr = "",
            initialDataType = DataType.INT32,
            onWriteValue = { a, valStr, dt ->
                viewModel.writeMemoryValue(a, valStr, dt)
                viewModel.inspectAddress(addr)
            },
            onAddToWatchlist = { a, desc, dt, frz ->
                viewModel.addToWatchlist(a, desc, dt, frz)
            },
            onDismiss = { selectedFieldForEdit = null }
        )
    }
}
