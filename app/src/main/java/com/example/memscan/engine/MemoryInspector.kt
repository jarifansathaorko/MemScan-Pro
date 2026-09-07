package com.example.memscan.engine

import com.example.memscan.backend.MemoryBackend
import com.example.memscan.model.DataType
import com.example.memscan.model.ObjectField
import com.example.memscan.model.WriteLogEntry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Arrays

class MemoryInspector(
    private val backendProvider: () -> MemoryBackend,
    private val getPid: () -> Int?
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Write Tracker log
    private val _writeLogs = MutableStateFlow<List<WriteLogEntry>>(emptyList())
    val writeLogs: StateFlow<List<WriteLogEntry>> = _writeLogs.asStateFlow()

    // Value Tracer state
    data class TracePoint(val timestamp: Long, val valueFormatted: String, val rawBytes: ByteArray)

    private val _traceAddress = MutableStateFlow<Long?>(null)
    val traceAddress: StateFlow<Long?> = _traceAddress.asStateFlow()

    private val _traceHistory = MutableStateFlow<List<TracePoint>>(emptyList())
    val traceHistory: StateFlow<List<TracePoint>> = _traceHistory.asStateFlow()

    private var traceJob: Job? = null

    fun recordWrite(
        address: Long,
        written: ByteArray,
        readBack: ByteArray?,
        dataType: DataType,
        success: Boolean
    ) {
        val isOverwritten = readBack != null && !Arrays.equals(written, readBack)
        val entry = WriteLogEntry(
            address = address,
            writtenBytes = written,
            readBackBytes = readBack,
            isOverwritten = isOverwritten,
            success = success,
            dataType = dataType
        )
        _writeLogs.update { (listOf(entry) + it).take(60) }
    }

    fun clearWriteLogs() {
        _writeLogs.value = emptyList()
    }

    fun startTracing(address: Long, dataType: DataType) {
        _traceAddress.value = address
        _traceHistory.value = emptyList()
        traceJob?.cancel()

        traceJob = scope.launch {
            val itemSize = if (dataType.sizeInBytes > 0) dataType.sizeInBytes else 4
            while (isActive) {
                val pid = getPid()
                val backend = backendProvider()

                if (pid != null) {
                    val bytes = backend.readMemory(pid, address, itemSize)
                    if (bytes != null) {
                        val point = TracePoint(
                            timestamp = System.currentTimeMillis(),
                            valueFormatted = dataType.formatBytes(bytes),
                            rawBytes = bytes
                        )
                        _traceHistory.update { (listOf(point) + it).take(30) }
                    }
                }
                delay(120)
            }
        }
    }

    fun stopTracing() {
        traceJob?.cancel()
        traceJob = null
        _traceAddress.value = null
    }

    suspend fun inspectObjectLayout(
        address: Long,
        radiusBytes: Int = 48
    ): List<ObjectField> = withContext(Dispatchers.IO) {
        val pid = getPid() ?: return@withContext emptyList()
        val backend = backendProvider()

        val startAddr = maxOf(0L, address - radiusBytes)
        val totalBytes = radiusBytes * 2
        val raw = backend.readMemory(pid, startAddr, totalBytes) ?: return@withContext emptyList()

        val fields = mutableListOf<ObjectField>()

        var offset = 0
        while (offset <= raw.size - 4) {
            val fieldAddr = startAddr + offset
            val relativeOffset = (fieldAddr - address).toInt()
            val slice = raw.copyOfRange(offset, offset + 4)

            val bb = ByteBuffer.wrap(slice).order(ByteOrder.LITTLE_ENDIAN)
            val intVal = bb.getInt(0)
            val floatVal = bb.getFloat(0)

            val ptrHex = if (intVal > 0x00400000 && intVal < 0x7FFFFFFF) "0x%08X".format(intVal) else null

            // Printable ASCII check
            val isAscii = slice.all { it in 32..126 }
            val ascii = if (isAscii) String(slice, Charsets.US_ASCII) else null

            fields.add(
                ObjectField(
                    offset = relativeOffset,
                    address = fieldAddr,
                    rawBytes = slice,
                    int32Val = intVal,
                    floatVal = if (!floatVal.isNaN() && Math.abs(floatVal) < 1e9) floatVal else null,
                    pointerHex = ptrHex,
                    asciiStr = ascii
                )
            )

            offset += 4
        }

        fields
    }
}
