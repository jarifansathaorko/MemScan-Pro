package com.example.memscan.backend

import com.example.memorylab.MemoryLabTarget
import com.example.memscan.model.DataType
import com.example.memscan.model.MemoryRegion
import com.example.memscan.model.ProcessInfo
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap

class MockBackend : MemoryBackend {
    override val name: String = "Mock Engine (Simulation)"
    override val description: String = "Safe simulated memory space for testing, scanning, and hex inspection"
    override val isPrivileged: Boolean = false

    private var attachedProcess: ProcessInfo? = null

    // Simulated processes
    private val processes = listOf(
        ProcessInfo(
            pid = MemoryLabTarget.PID,
            name = MemoryLabTarget.PROCESS_NAME,
            packageName = MemoryLabTarget.PACKAGE_NAME,
            is64Bit = true,
            isDebuggable = true,
            memoryUsageMb = 72,
            processType = com.example.memscan.model.ProcessType.MAIN_UI
        ),
        // YouTube Studio Multi-Process Architecture
        ProcessInfo(
            pid = 18231,
            name = "YouTube Studio",
            packageName = "com.google.android.apps.youtube.creator",
            is64Bit = true,
            isDebuggable = false,
            memoryUsageMb = 412,
            processType = com.example.memscan.model.ProcessType.MAIN_UI,
            isPrimaryTarget = true
        ),
        ProcessInfo(
            pid = 18472,
            name = "Studio:sandboxed_proc0",
            packageName = "com.google.android.apps.youtube.creator",
            is64Bit = true,
            isDebuggable = false,
            memoryUsageMb = 103,
            processType = com.example.memscan.model.ProcessType.WEBVIEW_RENDERER,
            parentPid = 18231,
            isPrimaryTarget = false
        ),
        ProcessInfo(
            pid = 18491,
            name = "Studio:analytics_service",
            packageName = "com.google.android.apps.youtube.creator",
            is64Bit = true,
            isDebuggable = false,
            memoryUsageMb = 28,
            processType = com.example.memscan.model.ProcessType.SERVICE,
            parentPid = 18231,
            isPrimaryTarget = false
        ),
        ProcessInfo(
            pid = 12840,
            name = "CyberStrike 3D",
            packageName = "com.unity.targetgame",
            is64Bit = true,
            isDebuggable = true,
            memoryUsageMb = 285,
            processType = com.example.memscan.model.ProcessType.MAIN_UI
        ),
        ProcessInfo(
            pid = 8402,
            name = "Retro Arcade",
            packageName = "com.retro.arcade",
            is64Bit = false,
            isDebuggable = true,
            memoryUsageMb = 94,
            processType = com.example.memscan.model.ProcessType.MAIN_UI
        ),
        ProcessInfo(
            pid = 9115,
            name = "Physics Sandbox",
            packageName = "com.example.benchmark",
            is64Bit = true,
            isDebuggable = false,
            memoryUsageMb = 160,
            processType = com.example.memscan.model.ProcessType.MAIN_UI
        )
    )

    // Memory regions for attached simulated process
    private val simulatedRegions = listOf(
        MemoryRegion(
            startAddress = 0x00400000L,
            endAddress = 0x00460000L,
            permissions = "r-xp",
            pathname = "/data/app/com.example.memorylab/bin/main"
        ),
        MemoryRegion(
            startAddress = 0x7B420000L,
            endAddress = 0x7B460000L,
            permissions = "rw-p",
            pathname = "[heap]"
        ),
        MemoryRegion(
            startAddress = 0x8C100000L,
            endAddress = 0x8C130000L,
            permissions = "rw-p",
            pathname = "/data/app/lib/arm64/libil2cpp.so"
        ),
        MemoryRegion(
            startAddress = 0x7FFE0000L,
            endAddress = 0x7FFE8000L,
            permissions = "rw-p",
            pathname = "[stack]"
        )
    )

    // In-memory simulated address space (Sparse Map of address to byte)
    private val memoryMap = ConcurrentHashMap<Long, Byte>()

    init {
        // Automatically attach to MemoryLab target by default for instant testing
        attachSync(MemoryLabTarget.PID)
    }

    private fun resetAndPopulateMemory() {
        memoryMap.clear()

        // Populate with live MemoryLab variables:
        // Views (Int32) = 12450
        writeUint32(MemoryLabTarget.ADDR_VIEWS, MemoryLabTarget.views.value)
        // Formatted String representations (UTF-8 and UTF-16) for Smart Scan
        writeStringUtf8(0x7B42A120L, "12.5K")
        val utf16Views = "12.5K".toByteArray(Charsets.UTF_16LE)
        writeBytes(0x7B42A130L, utf16Views)
        // Subscribers (Int32) = 427
        writeUint32(MemoryLabTarget.ADDR_SUBSCRIBERS, MemoryLabTarget.subscribers.value)
        // Revenue (Float32) = 31.42
        writeFloat(MemoryLabTarget.ADDR_REVENUE, MemoryLabTarget.revenue.value)
        writeStringUtf8(0x7B42A140L, "$31.42")
        // Watch Time (Double64) = 183.7
        writeDouble(MemoryLabTarget.ADDR_WATCH_TIME, MemoryLabTarget.watchTime.value)

        // Additional varied test primitives
        writeUint32(0x7B42C000L, 100) // Health
        writeUint32(0x7B42C004L, 250) // Max Mana
        writeInt64(0x7B42C030L, 999999999L) // High Score
        writeStringUtf8(0x7B42C050L, "PlayerOne") // Player Name
        writeBytes(0x7B42C070L, byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())) // Magic Header

        // Random noise in heap for realistic scanning
        for (i in 0 until 500) {
            val addr = 0x7B420000L + (i * 32)
            if (!memoryMap.containsKey(addr)) {
                writeUint32(addr, (i * 17) % 5000)
            }
        }
    }

    private fun writeUint32(addr: Long, value: Int) {
        val bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
        writeBytes(addr, bytes)
    }

    private fun writeFloat(addr: Long, value: Float) {
        val bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array()
        writeBytes(addr, bytes)
    }

    private fun writeDouble(addr: Long, value: Double) {
        val bytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(value).array()
        writeBytes(addr, bytes)
    }

    private fun writeInt64(addr: Long, value: Long) {
        val bytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array()
        writeBytes(addr, bytes)
    }

    private fun writeStringUtf8(addr: Long, text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        writeBytes(addr, bytes)
        memoryMap[addr + bytes.size] = 0 // null terminator
    }

    private fun writeBytes(addr: Long, bytes: ByteArray) {
        for (i in bytes.indices) {
            memoryMap[addr + i] = bytes[i]
        }
    }

    override fun isAvailable(): Boolean = true

    override suspend fun getProcesses(): List<ProcessInfo> = processes

    private fun attachSync(pid: Int): Result<ProcessInfo> {
        val proc = processes.find { it.pid == pid }
            ?: return Result.failure(IllegalArgumentException("Process PID $pid not found"))
        attachedProcess = proc
        resetAndPopulateMemory()
        return Result.success(proc)
    }

    override suspend fun attach(pid: Int): Result<ProcessInfo> {
        return attachSync(pid)
    }

    override fun detach() {
        attachedProcess = null
    }

    override fun getAttachedProcess(): ProcessInfo? = attachedProcess

    override suspend fun getMemoryRegions(pid: Int): List<MemoryRegion> {
        return simulatedRegions
    }

    override suspend fun readMemory(pid: Int, address: Long, size: Int): ByteArray? {
        if (size <= 0) return ByteArray(0)

        // Keep MemoryLab target memory addresses fresh if values were updated via UI
        if (pid == MemoryLabTarget.PID || attachedProcess?.pid == MemoryLabTarget.PID) {
            val vBytes = MemoryLabTarget.getViewsBytes()
            for (i in vBytes.indices) memoryMap[MemoryLabTarget.ADDR_VIEWS + i] = vBytes[i]

            val sBytes = MemoryLabTarget.getSubscribersBytes()
            for (i in sBytes.indices) memoryMap[MemoryLabTarget.ADDR_SUBSCRIBERS + i] = sBytes[i]

            val rBytes = MemoryLabTarget.getRevenueBytes()
            for (i in rBytes.indices) memoryMap[MemoryLabTarget.ADDR_REVENUE + i] = rBytes[i]

            val wBytes = MemoryLabTarget.getWatchTimeBytes()
            for (i in wBytes.indices) memoryMap[MemoryLabTarget.ADDR_WATCH_TIME + i] = wBytes[i]
        }

        val result = ByteArray(size)
        for (i in 0 until size) {
            result[i] = memoryMap[address + i] ?: 0
        }
        return result
    }

    override suspend fun writeMemory(pid: Int, address: Long, data: ByteArray): Boolean {
        for (i in data.indices) {
            memoryMap[address + i] = data[i]
        }

        // Propagate memory edits directly to MemoryLab variables
        if (pid == MemoryLabTarget.PID || attachedProcess?.pid == MemoryLabTarget.PID) {
            MemoryLabTarget.updateFromMemoryWrite(address, data)
        }

        return true
    }

    override fun isProcessAlive(pid: Int): Boolean {
        return processes.any { it.pid == pid }
    }
}
