package com.example.memscan.engine

import com.example.memscan.backend.MemoryBackend
import com.example.memscan.model.FreezeStability
import com.example.memscan.model.FrozenAddress
import com.example.memscan.model.ScanResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Arrays

class MemoryFreezer(
    private val backendProvider: () -> MemoryBackend,
    private val getPid: () -> Int?
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var freezeJob: Job? = null

    private val _frozenList = MutableStateFlow<List<FrozenAddress>>(emptyList())
    val frozenList: StateFlow<List<FrozenAddress>> = _frozenList.asStateFlow()

    private val _isDaemonRunning = MutableStateFlow(false)
    val isDaemonRunning: StateFlow<Boolean> = _isDaemonRunning.asStateFlow()

    private val _isAggressiveMode = MutableStateFlow(false)
    val isAggressiveMode: StateFlow<Boolean> = _isAggressiveMode.asStateFlow()

    fun setAggressiveMode(aggressive: Boolean) {
        _isAggressiveMode.value = aggressive
    }

    fun toggleAggressiveMode() {
        _isAggressiveMode.value = !_isAggressiveMode.value
    }

    fun addAddress(address: FrozenAddress) {
        _frozenList.update { list ->
            val existing = list.indexOfFirst { it.address == address.address }
            if (existing >= 0) {
                list.toMutableList().apply { set(existing, address) }
            } else {
                list + address
            }
        }
        ensureDaemonRunning()
    }

    fun freezeAllCandidates(results: List<ScanResult>, label: String) {
        val newItems = results.mapIndexed { idx, res ->
            FrozenAddress(
                address = res.address,
                originalAddress = res.address,
                description = "$label #$idx",
                targetValue = res.currentValue,
                currentValue = res.currentValue,
                dataType = res.dataType,
                isFrozen = true
            )
        }
        _frozenList.update { current ->
            val existingAddrs = current.map { it.address }.toSet()
            val filteredNew = newItems.filter { it.address !in existingAddrs }
            current + filteredNew
        }
        ensureDaemonRunning()
    }

    fun clearAll() {
        _frozenList.value = emptyList()
        stop()
    }

    fun removeAddress(id: String) {
        _frozenList.update { it.filterNot { item -> item.id == id } }
    }

    fun toggleFreeze(id: String) {
        _frozenList.update { list ->
            list.map {
                if (it.id == id) it.copy(isFrozen = !it.isFrozen) else it
            }
        }
        ensureDaemonRunning()
    }

    fun updateValue(id: String, newValue: ByteArray) {
        _frozenList.update { list ->
            list.map {
                if (it.id == id) it.copy(targetValue = newValue) else it
            }
        }
    }

    fun updateDescription(id: String, newDesc: String) {
        _frozenList.update { list ->
            list.map {
                if (it.id == id) it.copy(description = newDesc) else it
            }
        }
    }

    private fun ensureDaemonRunning() {
        if (freezeJob?.isActive == true) return
        val hasActiveFreezes = _frozenList.value.any { it.isFrozen }
        if (!hasActiveFreezes) return

        _isDaemonRunning.value = true
        freezeJob = coroutineScope.launch {
            while (isActive) {
                val pid = getPid()
                val currentBackend = backendProvider()
                val intervalMs = if (_isAggressiveMode.value) 40L else 100L

                if (pid != null && currentBackend.isProcessAlive(pid)) {
                    val activeItems = _frozenList.value.filter { it.isFrozen }
                    for (item in activeItems) {
                        // 1. Write the target value
                        val writeOk = currentBackend.writeMemory(pid, item.address, item.targetValue)
                        
                        // 2. Read back immediately to verify enforcement
                        val readBack = currentBackend.readMemory(pid, item.address, item.targetValue.size)

                        if (readBack != null) {
                            val isMatch = Arrays.equals(readBack, item.targetValue)
                            val newOverwrites = if (!isMatch) item.overwriteCount + 1 else item.overwriteCount
                            val stability = when {
                                !isMatch -> FreezeStability.FIGHTING
                                item.relocationCount > 0 -> FreezeStability.RELOCATED
                                else -> FreezeStability.STABLE
                            }

                            // If target app is fighting and overwrote it back, do an immediate aggressive rewrite
                            if (!isMatch) {
                                currentBackend.writeMemory(pid, item.address, item.targetValue)
                            }

                            _frozenList.update { list ->
                                list.map { curr ->
                                    if (curr.id == item.id) {
                                        curr.copy(
                                            lastWriteSuccess = writeOk,
                                            currentValue = readBack,
                                            writeCount = curr.writeCount + 1,
                                            overwriteCount = newOverwrites,
                                            stability = stability
                                        )
                                    } else curr
                                }
                            }
                        } else {
                            // Memory read failed: address was unmapped or object moved by GC
                            // Attempt Emergency Auto-Relocation in surrounding +/- 32KB
                            val relocatedAddr = attemptRelocation(currentBackend, pid, item)
                            if (relocatedAddr != null) {
                                _frozenList.update { list ->
                                    list.map { curr ->
                                        if (curr.id == item.id) {
                                            curr.copy(
                                                address = relocatedAddr,
                                                stability = FreezeStability.RELOCATED,
                                                relocationCount = curr.relocationCount + 1,
                                                lastRelocatedAt = System.currentTimeMillis()
                                            )
                                        } else curr
                                    }
                                }
                            } else {
                                _frozenList.update { list ->
                                    list.map { curr ->
                                        if (curr.id == item.id) {
                                            curr.copy(stability = FreezeStability.LOST)
                                        } else curr
                                    }
                                }
                            }
                        }
                    }
                }

                delay(intervalMs)
            }
            _isDaemonRunning.value = false
        }
    }

    private suspend fun attemptRelocation(backend: MemoryBackend, pid: Int, item: FrozenAddress): Long? {
        val searchRadius = 32 * 1024L // 32KB search around previous address
        val start = maxOf(0L, item.address - searchRadius)
        val end = item.address + searchRadius
        val chunkSize = (end - start).toInt()

        val chunk = backend.readMemory(pid, start, chunkSize) ?: return null
        val target = item.targetValue
        val tSize = target.size

        for (offset in 0..(chunk.size - tSize)) {
            var match = true
            for (i in 0 until tSize) {
                if (chunk[offset + i] != target[i]) {
                    match = false
                    break
                }
            }
            if (match) {
                return start + offset
            }
        }
        return null
    }

    fun stop() {
        freezeJob?.cancel()
        freezeJob = null
        _isDaemonRunning.value = false
    }
}

