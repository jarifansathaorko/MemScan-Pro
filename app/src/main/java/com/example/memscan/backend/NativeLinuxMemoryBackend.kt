package com.example.memscan.backend

import android.os.Process
import com.example.memscan.model.MemoryRegion
import com.example.memscan.model.ProcessInfo
import com.example.memscan.nativebridge.NativeMemoryBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class NativeLinuxMemoryBackend : MemoryBackend {
    override val name: String = "Native Linux Engine (JNI)"
    override val description: String = "Direct JNI process_vm_readv/writev and /proc/[pid]/mem engine"
    override val isPrivileged: Boolean = true

    private var attachedProcess: ProcessInfo? = null

    override fun isAvailable(): Boolean {
        return NativeMemoryBridge.isAvailable()
    }

    override suspend fun getProcesses(): List<ProcessInfo> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ProcessInfo>()
        val selfPid = Process.myPid()

        try {
            val procDir = File("/proc")
            val pids = procDir.listFiles { file ->
                file.isDirectory && file.name.all { it.isDigit() }
            } ?: emptyArray()

            for (pidFile in pids) {
                val pid = pidFile.name.toIntOrNull() ?: continue
                if (pid == selfPid) continue

                val cmdlineFile = File(pidFile, "cmdline")
                val cmdline = try {
                    cmdlineFile.readText().replace("\u0000", " ").trim()
                } catch (e: Exception) {
                    ""
                }

                if (cmdline.isEmpty()) continue

                val pkgName = cmdline.substringBefore(":")
                val procType = com.example.memscan.model.ProcessType.detect(cmdline, pkgName)

                // Read parent PID and status if accessible
                val (ppid, memMb) = try {
                    val statFile = File(pidFile, "stat")
                    val statLine = statFile.readText()
                    val afterComm = statLine.substringAfterLast(")")
                    val parts = afterComm.trim().split("\\s+".toRegex())
                    val parent = parts.getOrNull(1)?.toIntOrNull()
                    val rssPages = parts.getOrNull(21)?.toLongOrNull() ?: 0L
                    val mb = (rssPages * 4 / 1024).toInt()
                    Pair(parent, mb)
                } catch (e: Exception) {
                    Pair(null, 0)
                }

                val is64Bit = try {
                    File(pidFile, "exe").canonicalPath.contains("64")
                } catch (e: Exception) {
                    true
                }

                list.add(
                    ProcessInfo(
                        pid = pid,
                        name = cmdline.split("/").last().take(30),
                        packageName = pkgName,
                        is64Bit = is64Bit,
                        isDebuggable = false,
                        memoryUsageMb = memMb,
                        isSystem = pid < 1000,
                        processType = procType,
                        parentPid = ppid,
                        isPrimaryTarget = (procType == com.example.memscan.model.ProcessType.MAIN_UI)
                    )
                )
            }
        } catch (e: Exception) {
            // Permission denied reading full /proc
        }

        if (list.isEmpty()) {
            // Provide fallback self/test entries if proc is restricted by SELinux
            list.add(
                ProcessInfo(
                    pid = selfPid,
                    name = "MemScan Pro (Self)",
                    packageName = "com.aistudio.memscan.xkqrvw",
                    is64Bit = true,
                    isDebuggable = true
                )
            )
        }

        list.sortedBy { it.pid }
    }

    override suspend fun attach(pid: Int): Result<ProcessInfo> = withContext(Dispatchers.IO) {
        if (!isProcessAlive(pid)) {
            return@withContext Result.failure(IllegalStateException("Process PID $pid is not active."))
        }

        val proc = getProcesses().find { it.pid == pid } ?: ProcessInfo(
            pid = pid,
            name = "Process $pid",
            packageName = "pid.$pid"
        )
        attachedProcess = proc
        Result.success(proc)
    }

    override fun detach() {
        attachedProcess = null
    }

    override fun getAttachedProcess(): ProcessInfo? = attachedProcess

    override suspend fun getMemoryRegions(pid: Int): List<MemoryRegion> = withContext(Dispatchers.IO) {
        val regions = mutableListOf<MemoryRegion>()
        val mapsFile = File("/proc/$pid/maps")
        if (!mapsFile.exists() || !mapsFile.canRead()) {
            return@withContext regions
        }

        try {
            mapsFile.forEachLine { line ->
                // Example line: 7b420000-7b460000 rw-p 00000000 00:00 0 [heap]
                val tokens = line.split("\\s+".toRegex())
                if (tokens.size >= 5) {
                    val addrs = tokens[0].split("-")
                    val start = addrs[0].toLongOrNull(16) ?: 0L
                    val end = addrs[1].toLongOrNull(16) ?: 0L
                    val perms = tokens[1]
                    val offset = tokens[2].toLongOrNull(16) ?: 0L
                    val dev = tokens[3]
                    val inode = tokens[4].toLongOrNull() ?: 0L
                    val path = if (tokens.size >= 6) tokens[5] else ""

                    regions.add(
                        MemoryRegion(
                            startAddress = start,
                            endAddress = end,
                            permissions = perms,
                            offset = offset,
                            device = dev,
                            inode = inode,
                            pathname = path
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore read errors
        }

        regions
    }

    override suspend fun readMemory(pid: Int, address: Long, size: Int): ByteArray? = withContext(Dispatchers.IO) {
        try {
            NativeMemoryBridge.readProcessMemory(pid, address, size)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun writeMemory(pid: Int, address: Long, data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            NativeMemoryBridge.writeProcessMemory(pid, address, data)
        } catch (e: Exception) {
            false
        }
    }

    override fun isProcessAlive(pid: Int): Boolean {
        return File("/proc/$pid").exists()
    }
}
