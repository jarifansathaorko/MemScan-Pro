package com.example.memscan.model

enum class ProcessType(val label: String, val isPrimaryRecommendation: Boolean) {
    MAIN_UI("Main UI Process", true),
    WEBVIEW_RENDERER("WebView Renderer", false),
    SERVICE("Background Service", false),
    ISOLATED("Isolated Process", false),
    OTHER("Worker Process", false);

    companion object {
        fun detect(cmdline: String, packageName: String): ProcessType {
            val suffix = if (cmdline.contains(":")) cmdline.substringAfter(":") else ""
            return when {
                suffix.isEmpty() || cmdline == packageName -> MAIN_UI
                suffix.contains("sandboxed") || suffix.contains("renderer") || suffix.contains("webview") -> WEBVIEW_RENDERER
                suffix.contains("service") || suffix.contains("bg") || suffix.contains("sync") || suffix.contains("push") -> SERVICE
                suffix.contains("isolated") -> ISOLATED
                else -> OTHER
            }
        }
    }
}

data class ProcessInfo(
    val pid: Int,
    val name: String,
    val packageName: String,
    val is64Bit: Boolean = true,
    val isDebuggable: Boolean = false,
    val memoryUsageMb: Int = 0,
    val isSystem: Boolean = false,
    val processType: ProcessType = ProcessType.MAIN_UI,
    val parentPid: Int? = null,
    val isPrimaryTarget: Boolean = (processType == ProcessType.MAIN_UI)
) {
    val displayLabel: String
        get() = "$packageName [$pid]"

    val architecture: String
        get() = if (is64Bit) "64-bit" else "32-bit"

    val roleBadge: String
        get() = when (processType) {
            ProcessType.MAIN_UI -> "MAIN UI"
            ProcessType.WEBVIEW_RENDERER -> "RENDERER"
            ProcessType.SERVICE -> "SERVICE"
            ProcessType.ISOLATED -> "ISOLATED"
            ProcessType.OTHER -> "WORKER"
        }
}

