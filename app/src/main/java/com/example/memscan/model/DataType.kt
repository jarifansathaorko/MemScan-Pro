package com.example.memscan.model

import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class DataType(val displayName: String, val sizeInBytes: Int, val alignment: Int) {
    INT8("Int8", 1, 1),
    INT16("Int16", 2, 2),
    INT32("Int32", 4, 4),
    INT64("Int64", 8, 8),
    FLOAT32("Float32", 4, 4),
    FLOAT64("Float64", 8, 8),
    STRING_UTF8("UTF-8 String", -1, 1),
    STRING_UTF16("UTF-16 String", -1, 2),
    RAW_BYTES("Raw Bytes (Hex)", -1, 1);

    fun parseToBytes(input: String): ByteArray? {
        return try {
            val trimmed = input.trim()
            when (this) {
                INT8 -> {
                    val value = trimmed.toByteOrNull() ?: trimmed.toIntOrNull(16)?.toByte() ?: return null
                    byteArrayOf(value)
                }
                INT16 -> {
                    val value = trimmed.toShortOrNull() ?: trimmed.toIntOrNull(16)?.toShort() ?: return null
                    ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
                }
                INT32 -> {
                    val value = trimmed.toIntOrNull() ?: trimmed.toLongOrNull(16)?.toInt() ?: return null
                    ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
                }
                INT64 -> {
                    val value = trimmed.toLongOrNull() ?: return null
                    ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array()
                }
                FLOAT32 -> {
                    val value = trimmed.toFloatOrNull() ?: return null
                    ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array()
                }
                FLOAT64 -> {
                    val value = trimmed.toDoubleOrNull() ?: return null
                    ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(value).array()
                }
                STRING_UTF8 -> trimmed.toByteArray(Charsets.UTF_8)
                STRING_UTF16 -> trimmed.toByteArray(Charsets.UTF_16LE)
                RAW_BYTES -> {
                    val cleanHex = trimmed.replace(" ", "").replace("0x", "")
                    if (cleanHex.length % 2 != 0) return null
                    cleanHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun formatBytes(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        return try {
            when (this) {
                INT8 -> bytes[0].toString()
                INT16 -> {
                    if (bytes.size < 2) return ""
                    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).short.toString()
                }
                INT32 -> {
                    if (bytes.size < 4) return ""
                    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).int.toString()
                }
                INT64 -> {
                    if (bytes.size < 8) return ""
                    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).long.toString()
                }
                FLOAT32 -> {
                    if (bytes.size < 4) return ""
                    "%.4f".format(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).float)
                }
                FLOAT64 -> {
                    if (bytes.size < 8) return ""
                    "%.4f".format(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).double)
                }
                STRING_UTF8 -> {
                    val nullIdx = bytes.indexOf(0)
                    val slice = if (nullIdx >= 0) bytes.copyOfRange(0, nullIdx) else bytes
                    String(slice, Charsets.UTF_8).filter { it.code in 32..126 }
                }
                STRING_UTF16 -> {
                    String(bytes, Charsets.UTF_16LE).takeWhile { it != '\u0000' }
                }
                RAW_BYTES -> bytes.joinToString(" ") { "%02X".format(it) }
            }
        } catch (e: Exception) {
            bytes.joinToString(" ") { "%02X".format(it) }
        }
    }

    /**
     * Compares two byte arrays according to the data type.
     * Returns: negative if val1 < val2, 0 if val1 == val2, positive if val1 > val2
     */
    fun compare(val1: ByteArray, val2: ByteArray): Int {
        return try {
            when (this) {
                INT8 -> val1[0].compareTo(val2[0])
                INT16 -> {
                    val s1 = ByteBuffer.wrap(val1).order(ByteOrder.LITTLE_ENDIAN).short
                    val s2 = ByteBuffer.wrap(val2).order(ByteOrder.LITTLE_ENDIAN).short
                    s1.compareTo(s2)
                }
                INT32 -> {
                    val i1 = ByteBuffer.wrap(val1).order(ByteOrder.LITTLE_ENDIAN).int
                    val i2 = ByteBuffer.wrap(val2).order(ByteOrder.LITTLE_ENDIAN).int
                    i1.compareTo(i2)
                }
                INT64 -> {
                    val l1 = ByteBuffer.wrap(val1).order(ByteOrder.LITTLE_ENDIAN).long
                    val l2 = ByteBuffer.wrap(val2).order(ByteOrder.LITTLE_ENDIAN).long
                    l1.compareTo(l2)
                }
                FLOAT32 -> {
                    val f1 = ByteBuffer.wrap(val1).order(ByteOrder.LITTLE_ENDIAN).float
                    val f2 = ByteBuffer.wrap(val2).order(ByteOrder.LITTLE_ENDIAN).float
                    f1.compareTo(f2)
                }
                FLOAT64 -> {
                    val d1 = ByteBuffer.wrap(val1).order(ByteOrder.LITTLE_ENDIAN).double
                    val d2 = ByteBuffer.wrap(val2).order(ByteOrder.LITTLE_ENDIAN).double
                    d1.compareTo(d2)
                }
                else -> {
                    // For string and raw bytes, compare content equality
                    if (val1.contentEquals(val2)) 0 else 1
                }
            }
        } catch (e: Exception) {
            0
        }
    }
}
