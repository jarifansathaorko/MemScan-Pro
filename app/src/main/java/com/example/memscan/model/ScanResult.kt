package com.example.memscan.model

enum class ScanStrategy(val label: String, val badge: String) {
    STANDARD("Exact Match", "EXACT"),
    NUMERIC_EXACT("Primitive Integer", "INT"),
    NUMERIC_FLOAT("Floating Point", "FLOAT"),
    STRING_ASCII("ASCII String", "ASCII"),
    STRING_UTF16("UTF-16 Java String", "UTF-16"),
    COMPACT_FORMAT("Compact Formatted", "COMPACT"),
    CLUSTERED("Clustered Struct", "CLUSTER")
}

data class ScanResult(
    val address: Long,
    val previousValue: ByteArray,
    val currentValue: ByteArray,
    val dataType: DataType,
    val strategy: ScanStrategy = ScanStrategy.STANDARD,
    val confidence: Int = 85,
    val regionTag: String = "HEAP",
    val clusterGroup: String? = null
) {
    val addressHex: String
        get() = "0x%08X".format(address)

    val currentFormatted: String
        get() = dataType.formatBytes(currentValue)

    val previousFormatted: String
        get() = dataType.formatBytes(previousValue)

    val confidenceBadge: String
        get() = when {
            confidence >= 90 -> "HIGH ($confidence%)"
            confidence >= 70 -> "MED ($confidence%)"
            else -> "LOW ($confidence%)"
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScanResult) return false
        return address == other.address && dataType == other.dataType
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + dataType.hashCode()
        return result
    }
}

