package com.example.memscan.engine

import com.example.memscan.backend.MemoryBackend
import com.example.memscan.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Arrays
import kotlin.coroutines.coroutineContext

class MemoryScanner(private val backend: MemoryBackend) {

    data class ScanProgress(
        val scannedBytes: Long,
        val totalBytes: Long,
        val resultsFound: Int,
        val currentRegion: String,
        val currentStrategy: String = ""
    ) {
        val fraction: Float
            get() = if (totalBytes > 0) (scannedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
    }

    suspend fun performFirstScan(
        pid: Int,
        dataType: DataType,
        mode: ScanMode,
        targetBytes: ByteArray?,
        regions: List<MemoryRegion>,
        filter: RegionFilter = RegionFilter.ALL_WRITABLE,
        onProgress: (ScanProgress) -> Unit = {}
    ): List<ScanResult> = withContext(Dispatchers.Default) {
        val results = mutableListOf<ScanResult>()
        val filteredRegions = regions
            .filter { it.matchesFilter(filter) }
            .sortedByDescending { it.category.priority }

        val totalBytes = filteredRegions.sumOf { it.sizeBytes }
        var scannedBytes = 0L

        val step = if (dataType.sizeInBytes > 0) dataType.alignment else 1
        val itemSize = if (dataType.sizeInBytes > 0) dataType.sizeInBytes else (targetBytes?.size ?: 1)
        val chunkSize = 64 * 1024 // 64KB read chunks

        for (region in filteredRegions) {
            coroutineContext.ensureActive()

            var currentAddress = region.startAddress
            while (currentAddress < region.endAddress) {
                coroutineContext.ensureActive()

                val bytesToRead = minOf(chunkSize.toLong(), region.endAddress - currentAddress).toInt()
                val chunk = backend.readMemory(pid, currentAddress, bytesToRead)

                if (chunk != null && chunk.isNotEmpty()) {
                    val maxOffset = chunk.size - itemSize
                    var offset = 0
                    while (offset <= maxOffset) {
                        val slice = chunk.copyOfRange(offset, offset + itemSize)
                        val address = currentAddress + offset

                        val matches = when (mode) {
                            ScanMode.EXACT -> {
                                targetBytes != null && Arrays.equals(slice, targetBytes)
                            }
                            ScanMode.UNKNOWN_INITIAL -> {
                                true
                            }
                            else -> false
                        }

                        if (matches) {
                            val confidence = when (region.category) {
                                RegionCategory.HEAP -> 92
                                RegionCategory.NATIVE_MALLOC -> 88
                                RegionCategory.APP_DATA -> 80
                                RegionCategory.ANONYMOUS -> 70
                                else -> 50
                            }

                            results.add(
                                ScanResult(
                                    address = address,
                                    previousValue = slice,
                                    currentValue = slice,
                                    dataType = dataType,
                                    strategy = ScanStrategy.STANDARD,
                                    confidence = confidence,
                                    regionTag = region.tag
                                )
                            )
                            if (results.size >= 50000) break
                        }

                        offset += step
                    }
                }

                currentAddress += bytesToRead
                scannedBytes += bytesToRead

                onProgress(
                    ScanProgress(
                        scannedBytes = scannedBytes,
                        totalBytes = totalBytes,
                        resultsFound = results.size,
                        currentRegion = "${region.tag} 0x%X".format(region.startAddress)
                    )
                )

                if (results.size >= 50000) break
            }

            if (results.size >= 50000) break
        }

        assignClusters(results)
    }

    /**
     * Smart Multi-Strategy Scan:
     * Analyzes the user's input (e.g. "12450", "12.5K", "$31.42") and concurrently searches:
     * 1. Numeric Integer (Int32 & Int64)
     * 2. Numeric Float (Float32 exact & scaled)
     * 3. ASCII string ("12450" & "12,450")
     * 4. UTF-16 Java String (UTF-16LE characters in heap)
     * 5. Compact formatted representation ("12.5K")
     * Automatically applies cluster detection to group analytics variables in the same object struct.
     */
    suspend fun performSmartScan(
        pid: Int,
        rawInput: String,
        regions: List<MemoryRegion>,
        filter: RegionFilter = RegionFilter.HEAP_ONLY,
        onProgress: (ScanProgress) -> Unit = {}
    ): List<ScanResult> = withContext(Dispatchers.Default) {
        val cleanInput = rawInput.trim().replace("$", "").replace(",", "").replace(" ", "")
        val candidateTargets = mutableListOf<SmartPattern>()

        // 1. Primitive Int32
        val intVal = cleanInput.toIntOrNull()
        if (intVal != null) {
            val bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(intVal).array()
            candidateTargets.add(SmartPattern(bytes, DataType.INT32, ScanStrategy.NUMERIC_EXACT, 90))
        }

        // 2. Primitive Int64
        val longVal = cleanInput.toLongOrNull()
        if (longVal != null && longVal > 0) {
            val bytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(longVal).array()
            candidateTargets.add(SmartPattern(bytes, DataType.INT64, ScanStrategy.NUMERIC_EXACT, 88))
        }

        // 3. Floating Point
        val floatVal = cleanInput.toFloatOrNull()
        if (floatVal != null) {
            val bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(floatVal).array()
            candidateTargets.add(SmartPattern(bytes, DataType.FLOAT32, ScanStrategy.NUMERIC_FLOAT, 85))

            // Scaled float representation (e.g. 12450 displayed as 12.45)
            if (floatVal >= 1000f) {
                val scaled = floatVal / 1000f
                val scaledBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(scaled).array()
                candidateTargets.add(SmartPattern(scaledBytes, DataType.FLOAT32, ScanStrategy.NUMERIC_FLOAT, 82))
            }
        }

        // 4. ASCII String
        if (cleanInput.length >= 3) {
            val asciiBytes = cleanInput.toByteArray(Charsets.US_ASCII)
            candidateTargets.add(SmartPattern(asciiBytes, DataType.STRING_UTF8, ScanStrategy.STRING_ASCII, 84))

            // Formatted with comma (e.g. "12,450")
            if (intVal != null) {
                val commaStr = "%,d".format(intVal)
                if (commaStr != cleanInput) {
                    val commaBytes = commaStr.toByteArray(Charsets.US_ASCII)
                    candidateTargets.add(SmartPattern(commaBytes, DataType.STRING_UTF8, ScanStrategy.STRING_ASCII, 82))
                }
            }
        }

        // 5. UTF-16 Java String (very common in Android ART string heaps!)
        if (cleanInput.length >= 3) {
            val utf16Bytes = cleanInput.toByteArray(Charsets.UTF_16LE)
            candidateTargets.add(SmartPattern(utf16Bytes, DataType.STRING_UTF16, ScanStrategy.STRING_UTF16, 86))
        }

        // 6. Compact Formats (e.g., 12.5K)
        if (intVal != null && intVal >= 1000) {
            val kStr = "%.1fK".format(intVal / 1000.0)
            val kBytes = kStr.toByteArray(Charsets.US_ASCII)
            candidateTargets.add(SmartPattern(kBytes, DataType.STRING_UTF8, ScanStrategy.COMPACT_FORMAT, 80))
            val kUtf16 = kStr.toByteArray(Charsets.UTF_16LE)
            candidateTargets.add(SmartPattern(kUtf16, DataType.STRING_UTF16, ScanStrategy.COMPACT_FORMAT, 82))
        }

        if (candidateTargets.isEmpty()) {
            return@withContext emptyList()
        }

        val prioritizedRegions = regions
            .filter { it.matchesFilter(filter) }
            .sortedByDescending { it.category.priority }

        val totalBytes = prioritizedRegions.sumOf { it.sizeBytes }
        var scannedBytes = 0L
        val results = mutableListOf<ScanResult>()
        val chunkSize = 64 * 1024

        for (region in prioritizedRegions) {
            coroutineContext.ensureActive()
            var currentAddress = region.startAddress

            while (currentAddress < region.endAddress) {
                coroutineContext.ensureActive()

                val bytesToRead = minOf(chunkSize.toLong(), region.endAddress - currentAddress).toInt()
                val chunk = backend.readMemory(pid, currentAddress, bytesToRead)

                if (chunk != null && chunk.isNotEmpty()) {
                    for (pattern in candidateTargets) {
                        val pSize = pattern.bytes.size
                        val maxOffset = chunk.size - pSize
                        val step = if (pattern.dataType.sizeInBytes > 0) pattern.dataType.alignment else 1
                        var offset = 0

                        while (offset <= maxOffset) {
                            var match = true
                            for (b in 0 until pSize) {
                                if (chunk[offset + b] != pattern.bytes[b]) {
                                    match = false
                                    break
                                }
                            }

                            if (match) {
                                val address = currentAddress + offset
                                val slice = chunk.copyOfRange(offset, offset + pSize)
                                val baseConfidence = pattern.baseConfidence + (if (region.category == RegionCategory.HEAP) 5 else 0)

                                results.add(
                                    ScanResult(
                                        address = address,
                                        previousValue = slice,
                                        currentValue = slice,
                                        dataType = pattern.dataType,
                                        strategy = pattern.strategy,
                                        confidence = minOf(98, baseConfidence),
                                        regionTag = region.tag
                                    )
                                )
                                if (results.size >= 50000) break
                            }

                            offset += step
                        }
                    }
                }

                currentAddress += bytesToRead
                scannedBytes += bytesToRead

                onProgress(
                    ScanProgress(
                        scannedBytes = scannedBytes,
                        totalBytes = totalBytes,
                        resultsFound = results.size,
                        currentRegion = "${region.tag} 0x%X".format(region.startAddress),
                        currentStrategy = "Multi-Strategy (${candidateTargets.size} representations)"
                    )
                )

                if (results.size >= 50000) break
            }

            if (results.size >= 50000) break
        }

        assignClusters(results)
    }

    suspend fun performNextScan(
        pid: Int,
        previousResults: List<ScanResult>,
        dataType: DataType,
        mode: ScanMode,
        targetBytes: ByteArray?,
        onProgress: (ScanProgress) -> Unit = {}
    ): List<ScanResult> = withContext(Dispatchers.Default) {
        val nextResults = mutableListOf<ScanResult>()
        val itemSize = if (dataType.sizeInBytes > 0) dataType.sizeInBytes else (targetBytes?.size ?: 1)
        val total = previousResults.size

        for ((index, item) in previousResults.withIndex()) {
            if (index % 100 == 0) {
                coroutineContext.ensureActive()
                onProgress(
                    ScanProgress(
                        scannedBytes = index.toLong(),
                        totalBytes = total.toLong(),
                        resultsFound = nextResults.size,
                        currentRegion = item.addressHex
                    )
                )
            }

            val currentBytes = backend.readMemory(pid, item.address, itemSize) ?: continue

            val matches = when (mode) {
                ScanMode.EXACT -> {
                    targetBytes != null && Arrays.equals(currentBytes, targetBytes)
                }
                ScanMode.CHANGED -> {
                    !Arrays.equals(currentBytes, item.previousValue)
                }
                ScanMode.UNCHANGED -> {
                    Arrays.equals(currentBytes, item.previousValue)
                }
                ScanMode.INCREASED -> {
                    dataType.compare(currentBytes, item.previousValue) > 0
                }
                ScanMode.DECREASED -> {
                    dataType.compare(currentBytes, item.previousValue) < 0
                }
                ScanMode.UNKNOWN_INITIAL -> true
            }

            if (matches) {
                nextResults.add(
                    item.copy(
                        previousValue = item.currentValue,
                        currentValue = currentBytes,
                        confidence = minOf(99, item.confidence + 5) // confidence increases on each successive refinement!
                    )
                )
            }
        }

        assignClusters(nextResults)
    }

    private fun assignClusters(results: List<ScanResult>): List<ScanResult> {
        if (results.size < 2) return results

        val sorted = results.sortedBy { it.address }
        val finalResults = mutableListOf<ScanResult>()

        for (i in sorted.indices) {
            val current = sorted[i]
            val prev = if (i > 0) sorted[i - 1] else null
            val next = if (i < sorted.size - 1) sorted[i + 1] else null

            val distPrev = if (prev != null) Math.abs(current.address - prev.address) else Long.MAX_VALUE
            val distNext = if (next != null) Math.abs(current.address - next.address) else Long.MAX_VALUE

            // If another target value is found within 4096 bytes (struct / class proximity)
            if (distPrev <= 4096 || distNext <= 4096) {
                val clusterBase = current.address and 0xFFFFFF00L
                finalResults.add(
                    current.copy(
                        strategy = if (current.strategy == ScanStrategy.STANDARD) ScanStrategy.CLUSTERED else current.strategy,
                        confidence = minOf(99, current.confidence + 8),
                        clusterGroup = "Struct Cluster @ 0x%08X".format(clusterBase)
                    )
                )
            } else {
                finalResults.add(current)
            }
        }

        return finalResults
    }

    private data class SmartPattern(
        val bytes: ByteArray,
        val dataType: DataType,
        val strategy: ScanStrategy,
        val baseConfidence: Int
    )
}

