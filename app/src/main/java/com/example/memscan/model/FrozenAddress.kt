package com.example.memscan.model

enum class FreezeStability(val label: String, val badgeColorName: String) {
    STABLE("Stable (Enforced)", "Success"),
    FIGHTING("Active Overwrite (Battling UI)", "Warning"),
    RELOCATED("Auto-Relocated", "Primary"),
    LOST("Address Lost", "Error")
}

data class FrozenAddress(
    val id: String = java.util.UUID.randomUUID().toString(),
    val address: Long,
    val originalAddress: Long = address,
    val description: String,
    val targetValue: ByteArray,
    val currentValue: ByteArray,
    val dataType: DataType,
    val isFrozen: Boolean = true,
    val lastWriteSuccess: Boolean = true,
    val stability: FreezeStability = FreezeStability.STABLE,
    val writeCount: Long = 0L,
    val overwriteCount: Long = 0L,
    val stabilityScore: Float = 1.0f,
    val relocationCount: Int = 0,
    val lastRelocatedAt: Long = 0L
) {
    val addressHex: String
        get() = "0x%08X".format(address)

    val originalAddressHex: String
        get() = "0x%08X".format(originalAddress)

    val targetFormatted: String
        get() = dataType.formatBytes(targetValue)

    val currentFormatted: String
        get() = dataType.formatBytes(currentValue)
}

