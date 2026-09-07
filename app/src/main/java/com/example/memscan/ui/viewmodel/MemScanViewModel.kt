package com.example.memscan.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.memscan.backend.MemoryBackend
import com.example.memscan.backend.MockBackend
import com.example.memscan.backend.NativeLinuxMemoryBackend
import com.example.memscan.backend.RootBackend
import com.example.memscan.engine.AppDataFileManager
import com.example.memscan.engine.AccountState
import com.example.memscan.engine.AccountSwitchDetector
import com.example.memscan.engine.MemoryFreezer
import com.example.memscan.engine.MemoryInspector
import com.example.memscan.engine.MemoryScanner
import com.example.memscan.engine.ProfileManager
import com.example.memscan.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MemScanViewModel(application: Application) : AndroidViewModel(application) {

    // Available backends
    val mockBackend = MockBackend()
    val nativeBackend = NativeLinuxMemoryBackend()
    val rootBackend = RootBackend(nativeBackend)

    private val _currentBackend = MutableStateFlow<MemoryBackend>(mockBackend)
    val currentBackend: StateFlow<MemoryBackend> = _currentBackend.asStateFlow()

    private val scanner = MemoryScanner(_currentBackend.value)
    val freezer = MemoryFreezer(
        backendProvider = { _currentBackend.value },
        getPid = { _attachedProcess.value?.pid }
    )

    val inspector = MemoryInspector(
        backendProvider = { _currentBackend.value },
        getPid = { _attachedProcess.value?.pid }
    )

    val appDataFileManager = AppDataFileManager(application)
    val profileManager = ProfileManager(application)
    val profiles: StateFlow<List<FreezeProfile>> = profileManager.profiles

    val accountDetector = AccountSwitchDetector(_currentBackend.value, appDataFileManager)
    val accountState: StateFlow<AccountState> = accountDetector.accountState

    // Attached process state
    private val _attachedProcess = MutableStateFlow<ProcessInfo?>(mockBackend.getAttachedProcess())
    val attachedProcess: StateFlow<ProcessInfo?> = _attachedProcess.asStateFlow()

    private val _processList = MutableStateFlow<List<ProcessInfo>>(emptyList())
    val processList: StateFlow<List<ProcessInfo>> = _processList.asStateFlow()

    private val _isLoadingProcesses = MutableStateFlow(false)
    val isLoadingProcesses: StateFlow<Boolean> = _isLoadingProcesses.asStateFlow()

    // Scanner state
    private val _isSmartScan = MutableStateFlow(true)
    val isSmartScan: StateFlow<Boolean> = _isSmartScan.asStateFlow()

    private val _selectedRegionFilter = MutableStateFlow(RegionFilter.HEAP_ONLY)
    val selectedRegionFilter: StateFlow<RegionFilter> = _selectedRegionFilter.asStateFlow()

    private val _selectedDataType = MutableStateFlow(DataType.INT32)
    val selectedDataType: StateFlow<DataType> = _selectedDataType.asStateFlow()

    private val _selectedScanMode = MutableStateFlow(ScanMode.EXACT)
    val selectedScanMode: StateFlow<ScanMode> = _selectedScanMode.asStateFlow()

    private val _searchValue = MutableStateFlow("12450")
    val searchValue: StateFlow<String> = _searchValue.asStateFlow()

    private val _scanResults = MutableStateFlow<List<ScanResult>>(emptyList())
    val scanResults: StateFlow<List<ScanResult>> = _scanResults.asStateFlow()

    private val _scanCount = MutableStateFlow(0)
    val scanCount: StateFlow<Int> = _scanCount.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow<MemoryScanner.ScanProgress?>(null)
    val scanProgress: StateFlow<MemoryScanner.ScanProgress?> = _scanProgress.asStateFlow()

    private var scanJob: Job? = null

    // Memory regions state
    private val _memoryRegions = MutableStateFlow<List<MemoryRegion>>(emptyList())
    val memoryRegions: StateFlow<List<MemoryRegion>> = _memoryRegions.asStateFlow()

    // Hex Viewer state
    private val _hexBaseAddress = MutableStateFlow(0x7B42A108L)
    val hexBaseAddress: StateFlow<Long> = _hexBaseAddress.asStateFlow()

    private val _hexDumpRows = MutableStateFlow<List<HexDumpRow>>(emptyList())
    val hexDumpRows: StateFlow<List<HexDumpRow>> = _hexDumpRows.asStateFlow()

    // Object Inspector state
    private val _inspectedAddress = MutableStateFlow<Long?>(null)
    val inspectedAddress: StateFlow<Long?> = _inspectedAddress.asStateFlow()

    private val _inspectedFields = MutableStateFlow<List<ObjectField>>(emptyList())
    val inspectedFields: StateFlow<List<ObjectField>> = _inspectedFields.asStateFlow()

    private val _isInspecting = MutableStateFlow(false)
    val isInspecting: StateFlow<Boolean> = _isInspecting.asStateFlow()

    // App Data File Explorer state
    private val _cachedFiles = MutableStateFlow<List<AppDataFile>>(emptyList())
    val cachedFiles: StateFlow<List<AppDataFile>> = _cachedFiles.asStateFlow()

    private val _currentSubPath = MutableStateFlow("")
    val currentSubPath: StateFlow<String> = _currentSubPath.asStateFlow()

    private val _activeFileContent = MutableStateFlow<Pair<String, String>?>(null) // (path, content)
    val activeFileContent: StateFlow<Pair<String, String>?> = _activeFileContent.asStateFlow()

    private val _fileSearchResults = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val fileSearchResults: StateFlow<List<Pair<String, String>>> = _fileSearchResults.asStateFlow()

    // UI Status / Alert messages
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        // Load initial mock data
        loadMemoryRegions()
        loadHexDump(_hexBaseAddress.value)
    }

    fun switchBackendType(index: Int) {
        val targetBackend = when (index) {
            1 -> if (nativeBackend.isAvailable()) nativeBackend else mockBackend
            2 -> if (rootBackend.isAvailable()) rootBackend else mockBackend
            else -> mockBackend
        }
        applyNewBackend(targetBackend)
    }

    fun switchBackend(useNative: Boolean) {
        val targetBackend = if (useNative && nativeBackend.isAvailable()) nativeBackend else mockBackend
        applyNewBackend(targetBackend)
    }

    private fun applyNewBackend(targetBackend: MemoryBackend) {
        if (targetBackend != _currentBackend.value) {
            freezer.stop()
            _currentBackend.value.detach()
            _currentBackend.value = targetBackend
            _attachedProcess.value = targetBackend.getAttachedProcess()
            _scanResults.value = emptyList()
            _scanCount.value = 0
            refreshProcesses()
            loadMemoryRegions()
            loadHexDump(_hexBaseAddress.value)
            _statusMessage.value = "Switched to ${targetBackend.name}"
        }
    }

    fun refreshProcesses() {
        viewModelScope.launch {
            _isLoadingProcesses.value = true
            try {
                val list = _currentBackend.value.getProcesses()
                _processList.value = list
            } catch (e: Exception) {
                _statusMessage.value = "Failed to list processes: ${e.message}"
            } finally {
                _isLoadingProcesses.value = false
            }
        }
    }

    fun attachToProcess(pid: Int) {
        viewModelScope.launch {
            val result = _currentBackend.value.attach(pid)
            result.onSuccess { proc ->
                _attachedProcess.value = proc
                _statusMessage.value = "Attached to ${proc.packageName} [${proc.pid}]"
                _scanResults.value = emptyList()
                _scanCount.value = 0
                loadMemoryRegions()
                // Update hex base address to first readable region
                _memoryRegions.value.firstOrNull { it.isReadable }?.let {
                    _hexBaseAddress.value = it.startAddress
                    loadHexDump(it.startAddress)
                }
                loadCacheFiles()
                accountDetector.startMonitoring(
                    process = proc,
                    frozenItemsProvider = { freezer.frozenList.value },
                    onResetTriggered = { reason: String ->
                        _statusMessage.value = "Alert: $reason"
                    }
                )
            }.onFailure { err ->
                _statusMessage.value = "Failed to attach: ${err.message}"
            }
        }
    }

    fun detachProcess() {
        accountDetector.stopMonitoring()
        freezer.stop()
        inspector.stopTracing()
        _currentBackend.value.detach()
        _attachedProcess.value = null
        _scanResults.value = emptyList()
        _scanCount.value = 0
        _memoryRegions.value = emptyList()
        _hexDumpRows.value = emptyList()
        _statusMessage.value = "Detached from target process"
    }

    fun acknowledgeAccountReset() {
        accountDetector.acknowledgeReset()
    }

    fun handleAccountReset(autoRediscover: Boolean = false) {
        val proc = _attachedProcess.value
        val reason = accountDetector.accountState.value.resetReason
        accountDetector.acknowledgeReset()

        if (autoRediscover && proc != null) {
            val matchingProfile = profiles.value.firstOrNull { it.targetPackage == proc.packageName }
            if (matchingProfile != null) {
                _statusMessage.value = "Auto-recovering profile '${matchingProfile.name}'..."
                applyProfile(matchingProfile)
                return
            }
        }

        // Graceful reset: clear stale freeze locks & scan results
        freezer.clearAll()
        resetScan()
        _statusMessage.value = "Reset complete: cleared stale memory locks ($reason)"
    }

    fun toggleSmartScan() {
        _isSmartScan.value = !_isSmartScan.value
    }

    fun setRegionFilter(filter: RegionFilter) {
        _selectedRegionFilter.value = filter
    }

    fun setDataType(type: DataType) {
        _selectedDataType.value = type
    }

    fun setScanMode(mode: ScanMode) {
        _selectedScanMode.value = mode
    }

    fun setSearchValue(value: String) {
        _searchValue.value = value
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun startFirstScan() {
        val proc = _attachedProcess.value ?: run {
            _statusMessage.value = "No process attached"
            return
        }

        val type = _selectedDataType.value
        val mode = _selectedScanMode.value
        val input = _searchValue.value
        val filter = _selectedRegionFilter.value

        scanJob?.cancel()
        _isScanning.value = true

        scanJob = viewModelScope.launch {
            try {
                val scannerEngine = MemoryScanner(_currentBackend.value)
                val regions = if (_memoryRegions.value.isEmpty()) {
                    _currentBackend.value.getMemoryRegions(proc.pid)
                } else _memoryRegions.value

                val results = if (_isSmartScan.value && input.isNotBlank()) {
                    scannerEngine.performSmartScan(
                        pid = proc.pid,
                        rawInput = input,
                        regions = regions,
                        filter = filter,
                        onProgress = { prog -> _scanProgress.value = prog }
                    )
                } else {
                    val targetBytes = if (mode == ScanMode.EXACT) {
                        val parsed = type.parseToBytes(input)
                        if (parsed == null) {
                            _statusMessage.value = "Invalid input for data type ${type.displayName}"
                            _isScanning.value = false
                            return@launch
                        }
                        parsed
                    } else null

                    scannerEngine.performFirstScan(
                        pid = proc.pid,
                        dataType = type,
                        mode = mode,
                        targetBytes = targetBytes,
                        regions = regions,
                        filter = filter,
                        onProgress = { prog -> _scanProgress.value = prog }
                    )
                }

                _scanResults.value = results
                _scanCount.value = 1
                _statusMessage.value = "Scan complete: found ${results.size} matches"
            } catch (e: Exception) {
                _statusMessage.value = "Scan error: ${e.message}"
            } finally {
                _isScanning.value = false
                _scanProgress.value = null
            }
        }
    }

    fun startNextScan() {
        val proc = _attachedProcess.value ?: return
        if (_scanCount.value == 0) {
            startFirstScan()
            return
        }

        val type = _selectedDataType.value
        val mode = _selectedScanMode.value
        val input = _searchValue.value

        val targetBytes = if (mode == ScanMode.EXACT) {
            val parsed = type.parseToBytes(input)
            if (parsed == null) {
                _statusMessage.value = "Invalid input for data type ${type.displayName}"
                return
            }
            parsed
        } else null

        scanJob?.cancel()
        _isScanning.value = true

        scanJob = viewModelScope.launch {
            try {
                val scannerEngine = MemoryScanner(_currentBackend.value)
                val filtered = scannerEngine.performNextScan(
                    pid = proc.pid,
                    previousResults = _scanResults.value,
                    dataType = type,
                    mode = mode,
                    targetBytes = targetBytes,
                    onProgress = { prog -> _scanProgress.value = prog }
                )

                _scanResults.value = filtered
                _scanCount.update { it + 1 }
                _statusMessage.value = "Refined down to ${filtered.size} matches"
            } catch (e: Exception) {
                _statusMessage.value = "Scan error: ${e.message}"
            } finally {
                _isScanning.value = false
                _scanProgress.value = null
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _isScanning.value = false
        _scanProgress.value = null
        _statusMessage.value = "Scan cancelled"
    }

    fun resetScan() {
        scanJob?.cancel()
        _isScanning.value = false
        _scanProgress.value = null
        _scanResults.value = emptyList()
        _scanCount.value = 0
    }

    fun writeMemoryValue(address: Long, newValueStr: String, dataType: DataType) {
        val proc = _attachedProcess.value ?: return
        val bytes = dataType.parseToBytes(newValueStr) ?: run {
            _statusMessage.value = "Invalid format for ${dataType.displayName}"
            return
        }

        viewModelScope.launch {
            val success = _currentBackend.value.writeMemory(proc.pid, address, bytes)
            val readBack = _currentBackend.value.readMemory(proc.pid, address, bytes.size)
            inspector.recordWrite(address, bytes, readBack, dataType, success)

            if (success) {
                _statusMessage.value = "Wrote $newValueStr to 0x%08X".format(address)
                _scanResults.update { list ->
                    list.map {
                        if (it.address == address) {
                            it.copy(currentValue = bytes)
                        } else it
                    }
                }
                if (Math.abs(address - _hexBaseAddress.value) < 256) {
                    loadHexDump(_hexBaseAddress.value)
                }
            } else {
                _statusMessage.value = "Failed to write to 0x%08X (Access Denied)".format(address)
            }
        }
    }

    fun addToWatchlist(address: Long, label: String, dataType: DataType, freezeImmediately: Boolean = false) {
        val proc = _attachedProcess.value ?: return
        viewModelScope.launch {
            val bytes = _currentBackend.value.readMemory(proc.pid, address, if (dataType.sizeInBytes > 0) dataType.sizeInBytes else 4)
                ?: ByteArray(if (dataType.sizeInBytes > 0) dataType.sizeInBytes else 4)

            val item = FrozenAddress(
                address = address,
                description = label.ifEmpty { "Addr 0x%08X".format(address) },
                targetValue = bytes,
                currentValue = bytes,
                dataType = dataType,
                isFrozen = freezeImmediately
            )
            freezer.addAddress(item)
            _statusMessage.value = "Added 0x%08X to Watchlist".format(address)
        }
    }

    fun freezeAllCandidates(label: String) {
        if (_scanResults.value.isEmpty()) return
        freezer.freezeAllCandidates(_scanResults.value.take(20), label)
        _statusMessage.value = "Locked ${_scanResults.value.take(20).size} candidates in Freezer"
    }

    fun clearWatchlist() {
        freezer.clearAll()
        _statusMessage.value = "Cleared all watchlist addresses"
    }

    fun loadMemoryRegions() {
        val proc = _attachedProcess.value ?: return
        viewModelScope.launch {
            val regions = _currentBackend.value.getMemoryRegions(proc.pid)
            _memoryRegions.value = regions
        }
    }

    fun loadHexDump(address: Long) {
        val proc = _attachedProcess.value ?: return
        _hexBaseAddress.value = address

        viewModelScope.launch {
            val rowCount = 16
            val bytesPerRow = 16
            val totalBytes = rowCount * bytesPerRow
            val raw = _currentBackend.value.readMemory(proc.pid, address, totalBytes) ?: ByteArray(totalBytes)

            val rows = mutableListOf<HexDumpRow>()
            for (i in 0 until rowCount) {
                val rowAddr = address + (i * bytesPerRow)
                val sliceStart = i * bytesPerRow
                val sliceEnd = minOf(sliceStart + bytesPerRow, raw.size)
                val slice = if (sliceStart < raw.size) raw.copyOfRange(sliceStart, sliceEnd) else ByteArray(bytesPerRow)
                rows.add(HexDumpRow(address = rowAddr, bytes = slice))
            }
            _hexDumpRows.value = rows
        }
    }

    fun jumpHexAddress(address: Long) {
        loadHexDump(address)
    }

    fun editHexByte(address: Long, byteVal: Byte) {
        val proc = _attachedProcess.value ?: return
        viewModelScope.launch {
            val success = _currentBackend.value.writeMemory(proc.pid, address, byteArrayOf(byteVal))
            if (success) {
                loadHexDump(_hexBaseAddress.value)
            }
        }
    }

    // --- Object Inspector & Debugging ---

    fun inspectAddress(address: Long) {
        _inspectedAddress.value = address
        _isInspecting.value = true
        viewModelScope.launch {
            val fields = inspector.inspectObjectLayout(address)
            _inspectedFields.value = fields
            _isInspecting.value = false
        }
    }

    fun startTracing(address: Long, dataType: DataType) {
        inspector.startTracing(address, dataType)
        _statusMessage.value = "Started live value tracer on 0x%08X".format(address)
    }

    fun stopTracing() {
        inspector.stopTracing()
    }

    // --- Cache & File System Fallback (Plan B) ---

    fun loadCacheFiles(subPath: String = "") {
        val pkg = _attachedProcess.value?.packageName ?: "com.google.android.apps.youtube.creator"
        _currentSubPath.value = subPath
        viewModelScope.launch {
            val files = appDataFileManager.listFiles(pkg, subPath)
            _cachedFiles.value = files
        }
    }

    fun openFile(filePath: String) {
        viewModelScope.launch {
            val text = appDataFileManager.readFileText(filePath)
            if (text != null) {
                _activeFileContent.value = Pair(filePath, text)
            } else {
                _statusMessage.value = "Could not read file (Binary or Permission Denied)"
            }
        }
    }

    fun closeActiveFile() {
        _activeFileContent.value = null
    }

    fun saveActiveFile(newContent: String) {
        val current = _activeFileContent.value ?: return
        viewModelScope.launch {
            val ok = appDataFileManager.writeFileText(current.first, newContent)
            if (ok) {
                _activeFileContent.value = Pair(current.first, newContent)
                _statusMessage.value = "Saved changes to ${current.first}"
            } else {
                _statusMessage.value = "Failed to write file (Root required)"
            }
        }
    }

    fun searchCacheFiles(query: String) {
        val pkg = _attachedProcess.value?.packageName ?: "com.google.android.apps.youtube.creator"
        viewModelScope.launch {
            val results = appDataFileManager.searchFiles(pkg, query)
            _fileSearchResults.value = results
            _statusMessage.value = "Found ${results.size} matches in cache files"
        }
    }

    fun forceStopTargetApp() {
        val pkg = _attachedProcess.value?.packageName ?: "com.google.android.apps.youtube.creator"
        viewModelScope.launch {
            val ok = appDataFileManager.forceStopApp(pkg)
            if (ok) {
                _statusMessage.value = "Force stopped $pkg. You can now edit cache/prefs files safely."
            } else {
                _statusMessage.value = "Could not force stop app"
            }
        }
    }

    // --- Freeze Profiles ---

    fun saveFreezeProfile(name: String) {
        val pkg = _attachedProcess.value?.packageName ?: "com.google.android.apps.youtube.creator"
        val currentFrozen = freezer.frozenList.value
        if (currentFrozen.isEmpty()) {
            _statusMessage.value = "Watchlist is empty. Add addresses to freeze first."
            return
        }

        val variables = currentFrozen.map {
            ProfileVariable(
                label = it.description,
                targetValueStr = it.targetFormatted,
                dataType = it.dataType,
                preferredStrategy = ScanStrategy.NUMERIC_EXACT
            )
        }

        val profile = FreezeProfile(
            name = name,
            targetPackage = pkg,
            variables = variables
        )
        profileManager.saveProfile(profile)
        _statusMessage.value = "Saved profile '$name' with ${variables.size} variables"
    }

    fun applyProfile(profile: FreezeProfile) {
        val proc = _attachedProcess.value ?: run {
            _statusMessage.value = "Please attach to target process first"
            return
        }

        viewModelScope.launch {
            _statusMessage.value = "Applying Profile '${profile.name}'..."
            val regions = if (_memoryRegions.value.isEmpty()) {
                _currentBackend.value.getMemoryRegions(proc.pid)
            } else _memoryRegions.value

            val scannerEngine = MemoryScanner(_currentBackend.value)

            for (v in profile.variables) {
                val matches = scannerEngine.performSmartScan(
                    pid = proc.pid,
                    rawInput = v.targetValueStr,
                    regions = regions,
                    filter = RegionFilter.HEAP_ONLY
                )

                val bestMatch = matches.maxByOrNull { it.confidence }
                if (bestMatch != null) {
                    val bytes = v.dataType.parseToBytes(v.targetValueStr) ?: bestMatch.currentValue
                    val item = FrozenAddress(
                        address = bestMatch.address,
                        originalAddress = bestMatch.address,
                        description = v.label,
                        targetValue = bytes,
                        currentValue = bestMatch.currentValue,
                        dataType = v.dataType,
                        isFrozen = true
                    )
                    freezer.addAddress(item)
                }
            }

            _statusMessage.value = "Profile applied: locked matching variables in memory"
        }
    }

    fun deleteProfile(id: String) {
        profileManager.deleteProfile(id)
    }
}

