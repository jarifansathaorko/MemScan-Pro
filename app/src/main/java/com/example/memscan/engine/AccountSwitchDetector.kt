package com.example.memscan.engine

import com.example.memscan.backend.MemoryBackend
import com.example.memscan.model.FreezeStability
import com.example.memscan.model.FrozenAddress
import com.example.memscan.model.ProcessInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AccountState(
    val accountIdentifier: String? = null,
    val isResetDetected: Boolean = false,
    val resetReason: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Monitors target application lifecycle and data to detect account switching,
 * profile logout, or complete memory unmapping events.
 */
class AccountSwitchDetector(
    private val backend: MemoryBackend,
    private val fileManager: AppDataFileManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitorJob: Job? = null

    private val _accountState = MutableStateFlow(AccountState())
    val accountState: StateFlow<AccountState> = _accountState.asStateFlow()

    private var lastObservedPid: Int? = null
    private var lastAccountToken: String? = null

    fun startMonitoring(
        process: ProcessInfo,
        frozenItemsProvider: () -> List<FrozenAddress>,
        onResetTriggered: (reason: String) -> Unit
    ) {
        monitorJob?.cancel()
        lastObservedPid = process.pid

        monitorJob = scope.launch {
            while (isActive) {
                delay(2000)

                // 1. Process Liveness Check (Target often restarts on account change)
                val isAlive = backend.isProcessAlive(process.pid)
                if (!isAlive) {
                    _accountState.value = AccountState(
                        isResetDetected = true,
                        resetReason = "Target process (${process.name}) terminated or recreated"
                    )
                    onResetTriggered("Target app process exited or switched account")
                    break
                }

                // 2. Memory Stability Collapse Check
                val frozen = frozenItemsProvider()
                if (frozen.size >= 2) {
                    val lostCount = frozen.count { it.stability == FreezeStability.LOST }
                    if (lostCount.toFloat() / frozen.size >= 0.75f) {
                        _accountState.value = AccountState(
                            isResetDetected = true,
                            resetReason = "Memory layout collapsed ($lostCount/${frozen.size} addresses lost)"
                        )
                        onResetTriggered("Memory reset detected ($lostCount addresses lost)")
                    }
                }

                // 3. SharedPreferences / Storage Identity Check
                try {
                    val searchMatches = fileManager.searchFiles(process.packageName, "account")
                    if (searchMatches.isNotEmpty()) {
                        val path = searchMatches.first().first
                        val content = fileManager.readFileText(path) ?: ""
                        val tokenMatch = Regex("<string name=\"(?:account_name|current_user_id|active_account|active_channel_id)\">(.*?)</string>").find(content)
                        val currentToken = tokenMatch?.groupValues?.getOrNull(1)
                        if (currentToken != null && lastAccountToken != null && currentToken != lastAccountToken) {
                            _accountState.value = AccountState(
                                accountIdentifier = currentToken,
                                isResetDetected = true,
                                resetReason = "Account switch: $lastAccountToken -> $currentToken"
                            )
                            onResetTriggered("Switched account to $currentToken")
                        }
                        if (currentToken != null) {
                            lastAccountToken = currentToken
                        }
                    }
                } catch (ignored: Exception) {}
            }
        }
    }

    fun acknowledgeReset() {
        _accountState.value = AccountState()
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }
}
