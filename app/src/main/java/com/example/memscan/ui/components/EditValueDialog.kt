package com.example.memscan.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.memscan.model.DataType
import com.example.ui.theme.*

@Composable
fun EditValueDialog(
    address: Long,
    currentValueStr: String,
    initialDataType: DataType,
    onWriteValue: (Long, String, DataType) -> Unit,
    onAddToWatchlist: (Long, String, DataType, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var editValue by remember { mutableStateOf(currentValueStr) }
    var label by remember { mutableStateOf("") }
    var freezeChecked by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ImmersiveBorder))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Edit Memory Value",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveText
                        )
                        Text(
                            text = "0x%08X (%s)".format(address, initialDataType.displayName),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = ImmersivePrimary
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

                // New Value Field
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    label = { Text("New Value", color = ImmersiveTextMuted) },
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

                // Optional Description for Watchlist
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    placeholder = { Text("Watchlist label (e.g. Coins, Health)...", color = ImmersiveTextMuted, fontSize = 12.sp) },
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

                // Freeze toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AcUnit,
                            contentDescription = null,
                            tint = if (freezeChecked) ImmersivePrimary else ImmersiveTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Freeze value (Continuous write)",
                            fontSize = 12.sp,
                            color = ImmersiveText
                        )
                    }
                    Switch(
                        checked = freezeChecked,
                        onCheckedChange = { freezeChecked = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ImmersiveOnPrimary,
                            checkedTrackColor = ImmersivePrimary,
                            uncheckedThumbColor = ImmersiveTextMuted,
                            uncheckedTrackColor = ImmersiveSurfaceHigh
                        )
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onAddToWatchlist(address, label, initialDataType, freezeChecked)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder)
                    ) {
                        Text("Add Watchlist", color = ImmersiveText, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            onWriteValue(address, editValue, initialDataType)
                            if (freezeChecked || label.isNotEmpty()) {
                                onAddToWatchlist(address, label, initialDataType, freezeChecked)
                            }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ImmersivePrimary,
                            contentColor = ImmersiveOnPrimary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Write Value", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
