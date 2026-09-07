package com.example.memscan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
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
import com.example.memscan.backend.MemoryBackend
import com.example.memscan.backend.RootBackend
import com.example.memscan.nativebridge.NativeMemoryBridge
import com.example.ui.theme.*

@Composable
fun BackendSettingsDialog(
    currentBackend: MemoryBackend,
    onSelectBackendType: (Int) -> Unit = {},
    onSelectBackend: (useNative: Boolean) -> Unit = {},
    onDismiss: () -> Unit
) {
    val isNativeAvailable = remember { NativeMemoryBridge.isAvailable() }
    val isRootAvailable = remember { RootBackend.isSuBinaryPresent() }
    val nativeArch = remember {
        if (isNativeAvailable) {
            try { NativeMemoryBridge.getNativeArch() } catch (e: Exception) { "arm64-v8a" }
        } else "N/A"
    }

    val isMock = !currentBackend.isPrivileged && currentBackend.name.contains("Mock", ignoreCase = true)
    val isNative = currentBackend.name.contains("Native", ignoreCase = true)
    val isRoot = currentBackend.isPrivileged && currentBackend.name.contains("Root", ignoreCase = true)

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ImmersiveBorder))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = ImmersiveOnPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Engine Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveText
                            )
                            Text(
                                text = "v4.2.1-Native ($nativeArch)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = ImmersivePrimary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ImmersiveTextMuted
                        )
                    }
                }

                // Backend Selector Options
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "ACTIVE MEMORY BACKEND",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveTextMuted,
                        letterSpacing = 1.sp
                    )

                    // Option 1: Mock Backend
                    Surface(
                        onClick = {
                            onSelectBackendType(0)
                            onSelectBackend(false)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isMock) ImmersivePrimary.copy(alpha = 0.15f) else ImmersiveSurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isMock) ImmersivePrimary else ImmersiveBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Mock Engine (Simulation)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = ImmersiveText
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = ImmersiveSuccess.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "RECOMMENDED",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ImmersiveSuccess,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "In-memory target simulation linked to MemoryLab. Safe, instant testing without root.",
                                    fontSize = 11.sp,
                                    color = ImmersiveTextMuted
                                )
                            }
                            RadioButton(
                                selected = isMock,
                                onClick = {
                                    onSelectBackendType(0)
                                    onSelectBackend(false)
                                    onDismiss()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = ImmersivePrimary)
                            )
                        }
                    }

                    // Option 2: Native Linux JNI Backend
                    Surface(
                        onClick = {
                            onSelectBackendType(1)
                            onSelectBackend(true)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isNative) ImmersivePrimary.copy(alpha = 0.15f) else ImmersiveSurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isNative) ImmersivePrimary else ImmersiveBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Native Linux JNI Engine",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = ImmersiveText
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isNativeAvailable) ImmersiveSuccess.copy(alpha = 0.2f) else ImmersiveError.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = if (isNativeAvailable) "READY" else "UNLOADED",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isNativeAvailable) ImmersiveSuccess else ImmersiveError,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Direct process_vm_readv / /proc/pid/mem syscalls for debuggable or same-UID apps.",
                                    fontSize = 11.sp,
                                    color = ImmersiveTextMuted
                                )
                            }
                            RadioButton(
                                selected = isNative,
                                onClick = {
                                    onSelectBackendType(1)
                                    onSelectBackend(true)
                                    onDismiss()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = ImmersivePrimary)
                            )
                        }
                    }

                    // Option 3: Root Backend (su binary)
                    Surface(
                        onClick = {
                            onSelectBackendType(2)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isRoot) ImmersivePrimary.copy(alpha = 0.15f) else ImmersiveSurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isRoot) ImmersivePrimary else ImmersiveBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Root Engine (su binary)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = ImmersiveText
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isRootAvailable) ImmersiveSuccess.copy(alpha = 0.2f) else ImmersiveTextMuted.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = if (isRootAvailable) "SU FOUND" else "UNROOTED",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isRootAvailable) ImmersiveSuccess else ImmersiveTextMuted,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Privileged memory inspection via su binary ptrace/procfs for non-debuggable targets.",
                                    fontSize = 11.sp,
                                    color = ImmersiveTextMuted
                                )
                            }
                            RadioButton(
                                selected = isRoot,
                                onClick = {
                                    onSelectBackendType(2)
                                    onDismiss()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = ImmersivePrimary)
                            )
                        }
                    }
                }

                // Info footer
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ImmersiveSurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "SAFETY & COMPLIANCE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ImmersivePrimary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "For educational testing on controlled applications only. Memory bounds and error checking protect device stability.",
                            fontSize = 11.sp,
                            color = ImmersiveTextMuted
                        )
                    }
                }
            }
        }
    }
}
