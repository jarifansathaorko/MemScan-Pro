package com.example.memscan.model

data class HexDumpRow(
    val address: Long,
    val bytes: ByteArray
) {
    val addressHex: String
        get() = "0x%08X".format(address)

    val hexString: String
        get() = bytes.joinToString(" ") { "%02X".format(it) }

    val asciiString: String
        get() = bytes.map { b ->
            val code = b.toInt() and 0xFF
            if (code in 32..126) code.toChar() else '.'
        }.joinToString("")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HexDumpRow) return false
        return address == other.address && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
