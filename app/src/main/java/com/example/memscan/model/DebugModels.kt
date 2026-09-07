package com.example.memscan.model

data class WriteLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val address: Long,
    val writtenBytes: ByteArray,
    val readBackBytes: ByteArray?,
    val isOverwritten: Boolean,
    val success: Boolean,
    val dataType: DataType
) {
    val addressHex: String
        get() = "0x%08X".format(address)

    val writtenFormatted: String
        get() = dataType.formatBytes(writtenBytes)

    val readBackFormatted: String
        get() = if (readBackBytes != null) dataType.formatBytes(readBackBytes) else "NULL (Unmapped)"
}

data class ObjectField(
    val offset: Int,
    val address: Long,
    val rawBytes: ByteArray,
    val int32Val: Int?,
    val floatVal: Float?,
    val pointerHex: String?,
    val asciiStr: String?
) {
    val addressHex: String
        get() = "0x%08X".format(address)

    val offsetFormatted: String
        get() = if (offset >= 0) "+0x%02X (+%d)".format(offset, offset) else "-0x%02X (-%d)".format(-offset, -offset)
}
