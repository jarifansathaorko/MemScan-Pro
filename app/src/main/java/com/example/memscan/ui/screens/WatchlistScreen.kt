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
import androidx.compose.ui.window.Dialog
import com.example.memscan.model.FreezeProfile
import com.example.memscan.model.FreezeStability
import com.example.memscan.model.FrozenAddress
import com.example.memscan.ui.components.EditValueDialog
import com.example.memscan.ui.viewmodel.MemScanViewModel
import com.example.ui.theme.*

@Composable
fun WatchlistScreen(
    viewModel: MemScanViewModel,
    modifier: Modifier = Modifier,
    onNavigateToHex: (Long) -> Unit
) {
    val frozenList by viewModel.freezer.frozenList.collectAsState()
    val isDaemonRunning by viewModel.freezer.isDaemonRunning.collectAsState()
    val isAggressiveMode by viewModel.freezer.isAggressiveMode.collectAsState()
    val profiles by viewModel.profiles.collectAsState()

    var selectedForEdit by remember { mutableStateOf<FrozenAddress?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showSaveProfileDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("YT Studio Analytics") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status Card: Daemon Loop, Aggressive Mode & Profile Actions
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = ImmersiveSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isDaemonRunning) ImmersiveSuccess else ImmersiveTextMuted)
                        )
                        Column {
                            Text(
                                text = if (isDaemonRunning) "MEMORY FREEZE DAEMON" else "DAEMON IDLE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isDaemonRunning) ImmersiveSuccess else ImmersiveTextMuted,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "${frozenList.count { it.isFrozen }} frozen / ${frozenList.size} monitored",
                                fontSize = 11.sp,
                                color = ImmersiveTextMuted
                            )
                        }
                    }

                    // Aggressive Mode Toggle (40ms vs 100ms)
                    Surface(
                        onClick = { viewModel.freezer.toggleAggressiveMode() },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAggressiveMode) ImmersiveWarning.copy(alpha = 0.2f) else ImmersiveSurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAggressiveMode) ImmersiveWarning else ImmersiveBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = if (isAggressiveMode) ImmersiveWarning else ImmersivePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isAggressiveMode) "AGGRESSIVE (40ms)" else "NORMAL (100ms)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAggressiveMode) ImmersiveWarning else ImmersivePrimary
                            )
                        }
                    }
                }

                // Action Buttons Row: Profiles & Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showSaveProfileDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ImmersivePrimary, contentColor = ImmersiveOnPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SAVE PROFILE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showProfileDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Bookmarks, contentDescription = null, modifier = Modifier.size(13.dp), tint = ImmersivePrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LOAD (${profiles.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ImmersiveText)
                        }
                    }

                    if (frozenList.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearWatchlist() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("CLEAR ALL", color = ImmersiveError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Watchlist Table Container
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
                        text = "WATCHED ADDRESSES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveTextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "STABILITY / LOCK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveTextMuted,
                        letterSpacing = 0.5.sp
                    )
                }

                HorizontalDivider(color = ImmersiveBorder, thickness = 1.dp)

                if (frozenList.isEmpty()) {
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
                                imageVector = Icons.Default.AcUnit,
                                contentDescription = null,
                                tint = ImmersiveTextMuted.copy(alpha = 0.5f),
                                modifier = Modifier.size(42.dp)
                            )
                            Text(
                                text = "No addresses saved to watchlist yet",
                                color = ImmersiveTextMuted,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Tap Freeze or Freeze All in Scanner to lock values continuously",
                                color = ImmersiveTextMuted.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(frozenList, key = { it.id }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedForEdit = item }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Address, Stability & Description
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.addressHex,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ImmersivePrimary
                                        )

                                        // Stability Badge
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = when (item.stability) {
                                                FreezeStability.STABLE -> ImmersiveSuccess.copy(alpha = 0.2f)
                                                FreezeStability.FIGHTING -> ImmersiveWarning.copy(alpha = 0.2f)
                                                FreezeStability.RELOCATED -> ImmersivePrimary.copy(alpha = 0.2f)
                                                FreezeStability.LOST -> ImmersiveError.copy(alpha = 0.2f)
                                            }
                                        ) {
                                            Text(
                                                text = item.stability.name,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (item.stability) {
                                                    FreezeStability.STABLE -> ImmersiveSuccess
                                                    FreezeStability.FIGHTING -> ImmersiveWarning
                                                    FreezeStability.RELOCATED -> ImmersivePrimary
                                                    FreezeStability.LOST -> ImmersiveError
                                                },
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = ImmersiveSurfaceHigh
                                        ) {
                                            Text(
                                                text = item.dataType.displayName,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ImmersiveTextMuted,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = item.description,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ImmersiveText
                                    )
                                    Text(
                                        text = "Target: ${item.targetFormatted} | Current: ${item.currentFormatted}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = if (item.isFrozen) ImmersiveSuccess else ImmersiveTextMuted
                                    )
                                }

                                // Actions: Freeze Switch & Delete
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Switch(
                                        checked = item.isFrozen,
                                        onCheckedChange = { viewModel.freezer.toggleFreeze(item.id) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = ImmersiveOnPrimary,
                                            checkedTrackColor = ImmersivePrimary,
                                            uncheckedThumbColor = ImmersiveTextMuted,
                                            uncheckedTrackColor = ImmersiveSurfaceHigh
                                        )
                                    )

                                    IconButton(
                                        onClick = { onNavigateToHex(item.address) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Code,
                                            contentDescription = "Inspect in Hex",
                                            tint = ImmersiveTextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.freezer.removeAddress(item.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = ImmersiveError.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
                                        )
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

    // Save Profile Dialog
    if (showSaveProfileDialog) {
        Dialog(onDismissRequest = { showSaveProfileDialog = false }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ImmersiveSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("SAVE FREEZE PROFILE", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ImmersivePrimary)
                    Text("Save currently frozen addresses as a profile for auto-discovery across app relaunches.", fontSize = 12.sp, color = ImmersiveTextMuted)

                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("Profile Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ImmersivePrimary,
                            unfocusedBorderColor = ImmersiveBorder,
                            focusedTextColor = ImmersiveText,
                            unfocusedTextColor = ImmersiveText
                        )
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showSaveProfileDialog = false }) {
                            Text("CANCEL", color = ImmersiveTextMuted)
                        }
                        Button(
                            onClick = {
                                if (newProfileName.isNotBlank()) {
                                    viewModel.saveFreezeProfile(newProfileName.trim())
                                    showSaveProfileDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ImmersivePrimary)
                        ) {
                            Text("SAVE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Load / Manage Profiles Dialog
    if (showProfileDialog) {
        Dialog(onDismissRequest = { showProfileDialog = false }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ImmersiveSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("SAVED PROFILES", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ImmersivePrimary)

                    if (profiles.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                            Text("No saved profiles found.", color = ImmersiveTextMuted, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            items(profiles, key = { it.id }) { p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(p.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ImmersiveText)
                                        Text("${p.variables.size} variables • ${p.targetPackage}", fontSize = 11.sp, color = ImmersiveTextMuted)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Button(
                                            onClick = {
                                                viewModel.applyProfile(p)
                                                showProfileDialog = false
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("APPLY", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }

                                        IconButton(onClick = { viewModel.deleteProfile(p.id) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Delete, null, tint = ImmersiveError, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                HorizontalDivider(color = ImmersiveBorder.copy(alpha = 0.3f))
                            }
                        }
                    }

                    TextButton(onClick = { showProfileDialog = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("CLOSE", color = ImmersivePrimary)
                    }
                }
            }
        }
    }

    // Edit Value Dialog from Watchlist
    selectedForEdit?.let { item ->
        EditValueDialog(
            address = item.address,
            currentValueStr = item.targetFormatted,
            initialDataType = item.dataType,
            onWriteValue = { addr, newVal, dType ->
                val bytes = dType.parseToBytes(newVal)
                if (bytes != null) {
                    viewModel.freezer.updateValue(item.id, bytes)
                    viewModel.writeMemoryValue(addr, newVal, dType)
                }
            },
            onAddToWatchlist = { _, desc, _, freeze ->
                if (desc.isNotEmpty()) viewModel.freezer.updateDescription(item.id, desc)
                if (freeze != item.isFrozen) viewModel.freezer.toggleFreeze(item.id)
            },
            onDismiss = { selectedForEdit = null }
        )
    }
}
