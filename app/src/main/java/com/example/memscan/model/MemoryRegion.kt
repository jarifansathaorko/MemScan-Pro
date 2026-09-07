package com.example.memscan.model

enum class RegionCategory(val label: String, val priority: Int) {
    HEAP("ART Heap (UI / Objects)", 5),
    NATIVE_MALLOC("Native Malloc (libc)", 4),
    APP_DATA("App Data / BSS", 3),
    ANONYMOUS("Anonymous RW", 3),
    STACK("Thread Stack", 2),
    CODE_RO("Code / Read-Only", 1)
}

enum class RegionFilter(val label: String, val description: String, val estimatedTimeSec: Float) {
    HEAP_ONLY("Heap Only (Recommended)", "Java/Kotlin ViewModels, Compose state & UI objects", 1.2f),
    HEAP_AND_MALLOC("Heap + Native Malloc", "ART heap and native C++ memory allocations", 2.0f),
    ALL_WRITABLE("All Writable Memory", "Complete writable memory space (Heap, Malloc, BSS, Anon)", 3.8f),
    ALL_REGIONS("All Accessible (Incl Stack)", "Thorough scan including thread stacks and data", 5.2f);

    val displayName: String get() = label
}

data class MemoryRegion(
    val startAddress: Long,
    val endAddress: Long,
    val permissions: String, // e.g. "rw-p", "r-xp"
    val offset: Long = 0L,
    val device: String = "",
    val inode: Long = 0L,
    val pathname: String = ""
) {
    val sizeBytes: Long
        get() = endAddress - startAddress

    val isReadable: Boolean
        get() = permissions.contains("r")

    val isWritable: Boolean
        get() = permissions.contains("w")

    val isExecutable: Boolean
        get() = permissions.contains("x")

    val isPrivate: Boolean
        get() = permissions.contains("p")

    val isShared: Boolean
        get() = permissions.contains("s")

    val category: RegionCategory
        get() = when {
            pathname.startsWith("[heap]") || pathname.contains("dalvik") -> RegionCategory.HEAP
            pathname.contains("malloc") || pathname.contains("scudo") -> RegionCategory.NATIVE_MALLOC
            pathname.contains(".bss") || pathname.contains(".data") -> RegionCategory.APP_DATA
            pathname.startsWith("[stack") -> RegionCategory.STACK
            isWritable && (pathname.isEmpty() || pathname.startsWith("[anon")) -> RegionCategory.ANONYMOUS
            else -> RegionCategory.CODE_RO
        }

    fun matchesFilter(filter: RegionFilter): Boolean {
        if (!isReadable) return false
        return when (filter) {
            RegionFilter.HEAP_ONLY -> isWritable && category == RegionCategory.HEAP
            RegionFilter.HEAP_AND_MALLOC -> isWritable && (category == RegionCategory.HEAP || category == RegionCategory.NATIVE_MALLOC)
            RegionFilter.ALL_WRITABLE -> isWritable && category != RegionCategory.CODE_RO && category != RegionCategory.STACK
            RegionFilter.ALL_REGIONS -> isWritable
        }
    }

    val tag: String
        get() = when {
            pathname.startsWith("[heap]") -> "HEAP"
            pathname.contains("dalvik") -> "ART"
            pathname.contains("malloc") -> "MALLOC"
            pathname.startsWith("[stack") -> "STACK"
            pathname.startsWith("[anon") || pathname.isEmpty() -> "ANON"
            pathname.endsWith(".so") -> "LIB"
            pathname.endsWith(".apk") || pathname.endsWith(".dex") -> "APP/DEX"
            else -> "FILE"
        }

    val formattedRange: String
        get() = "0x%08X - 0x%08X".format(startAddress, endAddress)

    val formattedSize: String
        get() {
            val kb = sizeBytes / 1024
            return if (kb >= 1024) "${kb / 1024} MB" else "$kb KB"
        }
}

