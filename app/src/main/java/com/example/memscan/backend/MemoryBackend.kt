package com.example.memscan.backend

import com.example.memscan.model.MemoryRegion
import com.example.memscan.model.ProcessInfo

/**
 * Abstract interface for memory operations across different execution environments:
 * - MockBackend: In-memory simulation for instant emulator / demo testing without root
 * - NativeLinuxMemoryBackend: Direct JNI + process_vm_readv / /proc/$pid/mem for debuggable or rooted targets
 * - RootedShellBackend: Su / ptrace backend for elevated privilege execution
 */
interface MemoryBackend {
    val name: String
    val description: String
    val isPrivileged: Boolean

    fun isAvailable(): Boolean
    suspend fun getProcesses(): List<ProcessInfo>
    suspend fun attach(pid: Int): Result<ProcessInfo>
    fun detach()
    fun getAttachedProcess(): ProcessInfo?
    suspend fun getMemoryRegions(pid: Int): List<MemoryRegion>
    suspend fun readMemory(pid: Int, address: Long, size: Int): ByteArray?
    suspend fun writeMemory(pid: Int, address: Long, data: ByteArray): Boolean
    fun isProcessAlive(pid: Int): Boolean
}
