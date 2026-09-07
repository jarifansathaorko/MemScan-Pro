package com.example.memscan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.memscan.model.AppDataFile
import com.example.memscan.ui.viewmodel.MemScanViewModel
import com.example.ui.theme.*

@Composable
fun CacheExplorerScreen(
    viewModel: MemScanViewModel,
    modifier: Modifier = Modifier
) {
    val attachedProcess by viewModel.attachedProcess.collectAsState()
    val cachedFiles by viewModel.cachedFiles.collectAsState()
    val currentSubPath by viewModel.currentSubPath.collectAsState()
    val activeFileContent by viewModel.activeFileContent.collectAsState()
    val searchResults by viewModel.fileSearchResults.collectAsState()

    var searchQuery by remember { mutableStateOf("12450") }
    var isSearching by remember { mutableStateOf(false) }
    var showEditorDialog by remember { mutableStateOf(false) }
    var editorText by remember { mutableStateOf("") }

    val targetPackage = attachedProcess?.packageName ?: "com.google.android.apps.youtube.creator"

    LaunchedEffect(activeFileContent) {
        if (activeFileContent != null) {
            editorText = activeFileContent!!.second
            showEditorDialog = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Target & Force Stop Control Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ImmersiveSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PLAN B: LOCAL CACHE & STORAGE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersivePrimary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "/data/data/$targetPackage",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ImmersiveText
                    )
                }

                Button(
                    onClick = { viewModel.forceStopTargetApp() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersiveError),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("FORCE STOP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Grep / Search in Files Bar
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search files for '12450', 'views'...", fontSize = 13.sp, color = ImmersiveTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedTextColor = ImmersiveText,
                        unfocusedTextColor = ImmersiveText
                    ),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        if (searchQuery.isNotBlank()) {
                            viewModel.searchCacheFiles(searchQuery)
                            isSearching = true
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersivePrimary, contentColor = ImmersiveOnPrimary),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("GREP", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                if (isSearching) {
                    IconButton(
                        onClick = {
                            isSearching = false
                            viewModel.loadCacheFiles(currentSubPath)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Clear Search", tint = ImmersiveTextMuted)
                    }
                }
            }
        }

        // Navigation Breadcrumb Bar
        if (!isSearching) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (currentSubPath.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            val parent = currentSubPath.substringBeforeLast('/', "")
                            viewModel.loadCacheFiles(parent)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ImmersivePrimary)
                    }
                }

                Text(
                    text = if (currentSubPath.isEmpty()) "Root: /data/data/$targetPackage" else "/$currentSubPath",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = ImmersiveTextMuted
                )
            }
        }

        // Files List Container
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = ImmersiveSurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (isSearching) {
                if (searchResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No matching strings found in storage files", color = ImmersiveTextMuted, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = "MATCHES IN FILES (${searchResults.size})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ImmersivePrimary,
                                modifier = Modifier.padding(14.dp)
                            )
                            HorizontalDivider(color = ImmersiveBorder)
                        }
                        items(searchResults) { (path, matchLine) ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.openFile(path) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = path.substringAfterLast('/'),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ImmersivePrimary
                                )
                                Text(
                                    text = matchLine,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = ImmersiveText
                                )
                                Text(
                                    text = path,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = ImmersiveTextMuted
                                )
                            }
                            HorizontalDivider(color = ImmersiveBorder.copy(alpha = 0.2f))
                        }
                    }
                }
            } else {
                if (cachedFiles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.FolderOpen, null, tint = ImmersiveTextMuted, modifier = Modifier.size(44.dp))
                            Text("No files listed in this directory", color = ImmersiveTextMuted, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(cachedFiles) { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (file.isDirectory) {
                                            val next = if (currentSubPath.isEmpty()) file.name else "$currentSubPath/${file.name}"
                                            viewModel.loadCacheFiles(next)
                                        } else {
                                            viewModel.openFile(file.path)
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                        contentDescription = null,
                                        tint = if (file.isDirectory) ImmersivePrimary else ImmersiveSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column {
                                        Text(
                                            text = file.name,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ImmersiveText
                                        )
                                        Text(
                                            text = "${file.permissions} • ${file.formattedSize}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = ImmersiveTextMuted
                                        )
                                    }
                                }

                                if (!file.isDirectory) {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = "Edit File",
                                        tint = ImmersivePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = ImmersiveBorder.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }

    // In-place File Text Editor Dialog
    if (showEditorDialog && activeFileContent != null) {
        Dialog(onDismissRequest = {
            showEditorDialog = false
            viewModel.closeActiveFile()
        }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ImmersiveSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("EDIT FILE CONTENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ImmersivePrimary)
                            Text(
                                text = activeFileContent!!.first.substringAfterLast('/'),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveText
                            )
                        }
                        IconButton(onClick = {
                            showEditorDialog = false
                            viewModel.closeActiveFile()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = ImmersiveTextMuted)
                        }
                    }

                    HorizontalDivider(color = ImmersiveBorder)

                    OutlinedTextField(
                        value = editorText,
                        onValueChange = { editorText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showEditorDialog = false
                                viewModel.closeActiveFile()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("CANCEL")
                        }

                        Button(
                            onClick = {
                                viewModel.saveActiveFile(editorText)
                                showEditorDialog = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ImmersivePrimary, contentColor = ImmersiveOnPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SAVE (ROOT)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
