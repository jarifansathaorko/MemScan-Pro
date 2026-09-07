package com.example.memscan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
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
import com.example.memscan.model.HexDumpRow
import com.example.memscan.ui.viewmodel.MemScanViewModel
import com.example.ui.theme.*

@Composable
fun HexViewerScreen(
    viewModel: MemScanViewModel,
    modifier: Modifier = Modifier
) {
    val baseAddress by viewModel.hexBaseAddress.collectAsState()
    val dumpRows by viewModel.hexDumpRows.collectAsState()

    var jumpAddressInput by remember(baseAddress) { mutableStateOf("0x%08X".format(baseAddress)) }
    var selectedByteForEdit by remember { mutableStateOf<Pair<Long, Byte>?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Address Navigation Bar
        Surface(
            shape = RoundedCornerShape(16.dp),
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
                // Prev 256 bytes
                IconButton(
                    onClick = { viewModel.jumpHexAddress((baseAddress - 256).coerceAtLeast(0L)) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Previous 256B",
                        tint = ImmersivePrimary
                    )
                }

                // Address Input
                OutlinedTextField(
                    value = jumpAddressInput,
                    onValueChange = { jumpAddressInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ImmersiveSurfaceDark,
                        unfocusedContainerColor = ImmersiveSurfaceDark,
                        focusedBorderColor = ImmersivePrimary,
                        unfocusedBorderColor = ImmersiveBorder,
                        focusedTextColor = ImmersiveText,
                        unfocusedTextColor = ImmersiveText
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )

                // GO Button
                Button(
                    onClick = {
                        val clean = jumpAddressInput.trim().removePrefix("0x").removePrefix("0X")
                        val parsed = clean.toLongOrNull(16)
                        if (parsed != null) {
                            viewModel.jumpHexAddress(parsed)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImmersivePrimary,
                        contentColor = ImmersiveOnPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("GO", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Next 256 bytes
                IconButton(
                    onClick = { viewModel.jumpHexAddress(baseAddress + 256) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Next 256B",
                        tint = ImmersivePrimary
                    )
                }
            }
        }

        // Hex Dump Viewer
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ImmersiveSurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Table Subheader
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ImmersiveSurface.copy(alpha = 0.7f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OFFSET 00 01 02 03 04 05 06 07  08 09 0A 0B 0C 0D 0E 0F",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersivePrimary,
                        letterSpacing = 0.5.sp
                    )
                    IconButton(
                        onClick = { viewModel.loadHexDump(baseAddress) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = ImmersiveTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                HorizontalDivider(color = ImmersiveBorder, thickness = 1.dp)

                val horizontalScroll = rememberScrollState()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 6.dp)
                ) {
                    items(dumpRows, key = { it.address }) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(horizontalScroll)
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Address Column
                            Text(
                                text = row.addressHex,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ImmersivePrimary,
                                modifier = Modifier.width(96.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // 16 Hex bytes
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.width(360.dp)
                            ) {
                                for (i in 0 until 16) {
                                    val byteVal = if (i < row.bytes.size) row.bytes[i] else 0.toByte()
                                    val byteAddr = row.address + i
                                    val isZero = byteVal == 0.toByte()

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .clickable {
                                                selectedByteForEdit = Pair(byteAddr, byteVal)
                                            }
                                            .background(if (!isZero) ImmersiveSurfaceHigh else androidx.compose.ui.graphics.Color.Transparent)
                                            .padding(horizontal = 3.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "%02X".format(byteVal),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            fontWeight = if (!isZero) FontWeight.Bold else FontWeight.Normal,
                                            color = if (!isZero) ImmersiveText else ImmersiveTextMuted.copy(alpha = 0.5f)
                                        )
                                    }

                                    if (i == 7) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // ASCII representation
                            Text(
                                text = "|${row.asciiString}|",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ImmersiveSuccess.copy(alpha = 0.85f)
                            )
                        }
                        HorizontalDivider(color = ImmersiveBorder.copy(alpha = 0.2f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    // Single Byte Quick Edit Dialog
    selectedByteForEdit?.let { (addr, currentByte) ->
        var hexInput by remember { mutableStateOf("%02X".format(currentByte)) }

        Dialog(onDismissRequest = { selectedByteForEdit = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ImmersiveBorder))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Edit Byte at 0x%08X".format(addr),
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveText,
                        fontSize = 15.sp
                    )

                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { hexInput = it.take(2).uppercase() },
                        label = { Text("Hex Byte (00 - FF)", color = ImmersiveTextMuted) },
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ImmersiveSurfaceDark,
                            unfocusedContainerColor = ImmersiveSurfaceDark,
                            focusedBorderColor = ImmersivePrimary,
                            unfocusedBorderColor = ImmersiveBorder,
                            focusedTextColor = ImmersiveText,
                            unfocusedTextColor = ImmersiveText
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { selectedByteForEdit = null }) {
                            Text("CANCEL", color = ImmersiveTextMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val parsed = hexInput.toIntOrNull(16)?.toByte()
                                if (parsed != null) {
                                    viewModel.editHexByte(addr, parsed)
                                }
                                selectedByteForEdit = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ImmersivePrimary,
                                contentColor = ImmersiveOnPrimary
                            )
                        ) {
                            Text("WRITE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
