package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.memorylab.MemoryLabScreen
import com.example.memscan.ui.components.BackendSettingsDialog
import com.example.memscan.ui.components.ProcessPickerDialog
import com.example.memscan.ui.screens.CacheExplorerScreen
import com.example.memscan.ui.screens.HexViewerScreen
import com.example.memscan.ui.screens.InspectorScreen
import com.example.memscan.ui.screens.RegionsScreen
import com.example.memscan.ui.screens.ScannerScreen
import com.example.memscan.ui.screens.WatchlistScreen
import com.example.memscan.ui.viewmodel.MemScanViewModel
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    private val viewModel: MemScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MemScanApp(viewModel)
            }
        }
    }
}

enum class NavTab(val title: String, val icon: ImageVector) {
    SCANNER("SCAN", Icons.Default.Search),
    WATCHLIST("FREEZE", Icons.Default.AcUnit),
    INSPECTOR("DEBUG", Icons.Default.BugReport),
    CACHE_FILES("FILES", Icons.Default.FolderSpecial),
    HEX("HEX", Icons.Default.Code),
    TEST_LAB("LAB", Icons.Default.Science),
    REGIONS("MAP", Icons.Default.GridOn)
}

@Composable
fun MemScanApp(viewModel: MemScanViewModel) {
    var currentTab by remember { mutableStateOf(NavTab.SCANNER) }
    var showProcessPicker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val attachedProcess by viewModel.attachedProcess.collectAsState()
    val currentBackend by viewModel.currentBackend.collectAsState()
    val processList by viewModel.processList.collectAsState()
    val isLoadingProcesses by viewModel.isLoadingProcesses.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val accountState by viewModel.accountState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ImmersiveBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    containerColor = ImmersiveSurfaceHigh,
                    contentColor = ImmersiveText,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(data.visuals.message, fontSize = 13.sp)
                }
            }
        },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ImmersiveBackground)
            ) {
                // Header matching Design HTML
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ImmersivePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = ImmersiveOnPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "MemScan Pro",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveText,
                                fontSize = 18.sp
                            )
                            Text(
                                text = if (currentBackend.isPrivileged) "ENGINE V4.2-NATIVE" else "ENGINE V4.2-SIM",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ImmersivePrimary,
                                letterSpacing = 1.2.sp,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Settings Button
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ImmersiveBorder.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = ImmersiveText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = ImmersiveBorder, thickness = 1.dp)

                // Attached Process Status Banner matching Design HTML
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ImmersiveSurface)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.clickable {
                            viewModel.refreshProcesses()
                            showProcessPicker = true
                        }
                    ) {
                        // Glowing status dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .shadow(8.dp, CircleShape, spotColor = if (attachedProcess != null) ImmersiveSuccess else ImmersiveError)
                                .clip(CircleShape)
                                .background(if (attachedProcess != null) ImmersiveSuccess else ImmersiveError)
                        )

                        Column {
                            Text(
                                text = if (attachedProcess != null) "ATTACHED TO" else "STATUS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveTextMuted,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = attachedProcess?.displayLabel ?: "No process attached",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (attachedProcess != null) ImmersivePrimary else ImmersiveTextMuted
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (attachedProcess != null) {
                                viewModel.detachProcess()
                            } else {
                                viewModel.refreshProcesses()
                                showProcessPicker = true
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            contentColor = ImmersiveText
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (attachedProcess != null) "DETACH" else "SELECT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = ImmersiveBorder, thickness = 1.dp)
            }
        },
        bottomBar = {
            // Immersive Navigation Bar matching Design HTML
            Surface(
                color = ImmersiveSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavTab.values().forEach { tab ->
                        val isSelected = currentTab == tab

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .clickable { currentTab = tab }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(52.dp)
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) ImmersiveBorder else androidx.compose.ui.graphics.Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) ImmersivePrimary else ImmersiveTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = tab.title,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) ImmersiveText else ImmersiveTextMuted,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavTab.SCANNER -> ScannerScreen(
                    viewModel = viewModel,
                    onNavigateToHex = { addr ->
                        viewModel.jumpHexAddress(addr)
                        currentTab = NavTab.HEX
                    },
                    onNavigateToInspector = { addr ->
                        viewModel.inspectAddress(addr)
                        currentTab = NavTab.INSPECTOR
                    }
                )
                NavTab.WATCHLIST -> WatchlistScreen(
                    viewModel = viewModel,
                    onNavigateToHex = { addr ->
                        viewModel.jumpHexAddress(addr)
                        currentTab = NavTab.HEX
                    }
                )
                NavTab.INSPECTOR -> InspectorScreen(
                    viewModel = viewModel,
                    onNavigateToHex = { addr ->
                        viewModel.jumpHexAddress(addr)
                        currentTab = NavTab.HEX
                    }
                )
                NavTab.CACHE_FILES -> CacheExplorerScreen(
                    viewModel = viewModel
                )
                NavTab.HEX -> HexViewerScreen(
                    viewModel = viewModel
                )
                NavTab.TEST_LAB -> MemoryLabScreen(
                    onSwitchToScanner = {
                        currentTab = NavTab.SCANNER
                    }
                )
                NavTab.REGIONS -> RegionsScreen(
                    viewModel = viewModel,
                    onNavigateToHex = { addr ->
                        viewModel.jumpHexAddress(addr)
                        currentTab = NavTab.HEX
                    }
                )
            }
        }
    }

    // Process Picker Dialog
    if (showProcessPicker) {
        ProcessPickerDialog(
            processes = processList,
            isLoading = isLoadingProcesses,
            onRefresh = { viewModel.refreshProcesses() },
            onSelectProcess = { pid -> viewModel.attachToProcess(pid) },
            onDismiss = { showProcessPicker = false }
        )
    }

    // Backend / Engine Settings Dialog
    if (showSettings) {
        BackendSettingsDialog(
            currentBackend = currentBackend,
            onSelectBackendType = { typeIndex -> viewModel.switchBackendType(typeIndex) },
            onSelectBackend = { useNative -> viewModel.switchBackend(useNative) },
            onDismiss = { showSettings = false }
        )
    }

    // Account Reset / Switch Event Dialog
    if (accountState.isResetDetected) {
        AlertDialog(
            onDismissRequest = { viewModel.acknowledgeAccountReset() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = ImmersiveWarning)
                    Text("MEMORY RESET DETECTED", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ImmersiveText)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = accountState.resetReason,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ImmersivePrimary
                    )
                    Text(
                        text = "The target app changed state or switched account. Memory addresses might now be invalid or pointing to freed objects.",
                        fontSize = 12.sp,
                        color = ImmersiveTextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.handleAccountReset(autoRediscover = true) },
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersivePrimary, contentColor = ImmersiveOnPrimary)
                ) {
                    Text("AUTO-RECOVER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { viewModel.handleAccountReset(autoRediscover = false) }) {
                        Text("CLEAR LOCKS", fontSize = 11.sp, color = ImmersiveError)
                    }
                    TextButton(onClick = { viewModel.acknowledgeAccountReset() }) {
                        Text("IGNORE", fontSize = 11.sp, color = ImmersiveTextMuted)
                    }
                }
            },
            containerColor = ImmersiveSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
