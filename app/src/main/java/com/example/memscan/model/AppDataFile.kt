package com.example.memscan.model

data class AppDataFile(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val permissions: String = "-rw-rw----"
) {
    val formattedSize: String
        get() = when {
            isDirectory -> "<DIR>"
            sizeBytes >= 1024 * 1024 -> "%.1f MB".format(sizeBytes / (1024.0 * 1024.0))
            sizeBytes >= 1024 -> "%.1f KB".format(sizeBytes / 1024.0)
            else -> "$sizeBytes B"
        }

    val isEditableText: Boolean
        get() = !isDirectory && (name.endsWith(".xml") || name.endsWith(".json") || name.endsWith(".txt") || name.endsWith(".db-wal") == false)
}
