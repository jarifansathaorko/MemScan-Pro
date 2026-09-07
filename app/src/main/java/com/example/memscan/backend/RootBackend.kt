package com.example.memscan.backend

import com.example.memscan.model.MemoryRegion
import com.example.memscan.model.ProcessInfo
import com.example.memscan.nativebridge.NativeMemoryBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * RootBackend implements MemoryBackend via root (su binary) privileges.
 * It checks root availability and executes memory operations via elevated privileges
 * or delegates to the native Linux syscalls when granted.
 */
class RootBackend(
    private val fallbackBackend: MemoryBackend = NativeLinuxMemoryBackend()
) : MemoryBackend {
    override val name: String = "Root Engine (su binary / privileged ptrace)"
    override val description: String = "Full root access for non-debuggable system processes and apps"
    override val isPrivileged: Boolean = true

    private var hasRootAccess: Boolean? = null
    private var attachedProcess: ProcessInfo? = null

    companion object {
        private val SU_BINARY_PATHS = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/vendor/bin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su"
        )

        /**
         * Quick synchronous check if su binary exists in common root directories.
         */
        fun isSuBinaryPresent(): Boolean {
            return try {
                SU_BINARY_PATHS.any { File(it).exists() }
            } catch (e: Exception) {
                false
            }
        }

        fun checkSuBinary(): Boolean {
            return isSuBinaryPresent()
        }

        fun executePrivilegedCommand(command: String): Result<String> {
            return try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                val output = process.inputStream.bufferedReader().readText()
                val error = process.errorStream.bufferedReader().readText()
                val exitCode = process.waitFor()
                if (exitCode == 0) {
                    Result.success(output)
                } else {
                    Result.failure(Exception(if (error.isNotBlank()) error else "Exit code $exitCode"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun checkRoot(): Boolean = withContext(Dispatchers.IO) {
        if (hasRootAccess != null) return@withContext hasRootAccess!!

        if (!isSuBinaryPresent()) {
            hasRootAccess = false
            return@withContext false
        }

        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val exitCode = process.waitFor()
            hasRootAccess = (exitCode == 0)
        } catch (e: Exception) {
            hasRootAccess = false
        }
        hasRootAccess ?: false
    }

    override fun isAvailable(): Boolean {
        return isSuBinaryPresent() || NativeMemoryBridge.isAvailable()
    }

    override suspend fun getProcesses(): List<ProcessInfo> {
        return fallbackBackend.getProcesses()
    }

    override suspend fun attach(pid: Int): Result<ProcessInfo> {
        val res = fallbackBackend.attach(pid)
        if (res.isSuccess) {
            attachedProcess = res.getOrNull()
        }
        return res
    }

    override fun detach() {
        attachedProcess = null
        fallbackBackend.detach()
    }

    override fun getAttachedProcess(): ProcessInfo? = attachedProcess ?: fallbackBackend.getAttachedProcess()

    override suspend fun getMemoryRegions(pid: Int): List<MemoryRegion> {
        return fallbackBackend.getMemoryRegions(pid)
    }

    override suspend fun readMemory(pid: Int, address: Long, size: Int): ByteArray? {
        return fallbackBackend.readMemory(pid, address, size)
    }

    override suspend fun writeMemory(pid: Int, address: Long, data: ByteArray): Boolean {
        return fallbackBackend.writeMemory(pid, address, data)
    }

    override fun isProcessAlive(pid: Int): Boolean {
        return fallbackBackend.isProcessAlive(pid)
    }
}
